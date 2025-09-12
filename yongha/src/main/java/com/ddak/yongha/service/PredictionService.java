package com.ddak.yongha.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.ddak.yongha.mapper.PredictionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.mail.internet.MimeMessage;

@Service
public class PredictionService {

	@Autowired
	private PredictionMapper predictionMapper;

	@Autowired
	private JavaMailSender mailSender;

	private static final ObjectMapper objectMapper = new ObjectMapper();

	// 필요시 절대경로로 교체하세요.
	private static final String PYTHON_CMD = "python";
	private static final String SCRIPT_PATH = "src/scripts/predict_and_chart_base64.py";

	public void runPrediction() {
		try {
			// 실제용
			List<Map<String, Object>> parents = predictionMapper.getParentsToSend();
			// Test 용
			// List<Map<String, Object>> parents = predictionMapper.getParentsToSendTest();
			if (parents == null || parents.isEmpty())
				return;

			for (Map<String, Object> parent : parents) {
				Integer parentNo = getInteger(parent.get("USER_NO"));
				if (parentNo == null)
					continue;

				String email = (String) parent.get("EMAIL");
				if (email == null || email.isEmpty())
					continue;

				List<Map<String, Object>> expenses = predictionMapper.getUserExpensesByParent(parentNo);
				if (expenses == null || expenses.isEmpty())
					continue;

				// payload = 소비내역
				List<List<Object>> payload = new ArrayList<>();
				// payload2 = 자녀별 고정정보
				List<List<Object>> payload2 = new ArrayList<>();

				LocalDate today = LocalDate.now();
				int forecastDays = 90;

				// 자녀별 groupBy
				Map<Integer, List<Map<String, Object>>> expensesByChild = new HashMap<>();
				for (Map<String, Object> e : expenses) {
					Integer userNo = getInteger(e.get("USER_NO"));
					if (userNo == null)
						continue;
					expensesByChild.computeIfAbsent(userNo, k -> new ArrayList<>()).add(e);
				}

				for (Map.Entry<Integer, List<Map<String, Object>>> entry : expensesByChild.entrySet()) {
					Integer userNo = entry.getKey();
					List<Map<String, Object>> childExpenses = entry.getValue();

					// 소비내역 payload
					for (Map<String, Object> e : childExpenses) {
						Double amount = e.get("AMOUNT") != null ? ((Number) e.get("AMOUNT")).doubleValue() : 0.0;
						payload.add(Arrays.asList(
								userNo,
								getInteger(e.get("CATEGORY")),
								amount,
								e.get("SPEND_DATE")));
					}

					// 자녀별 고정정보 payload2 (널/이상치 방어)
					Double autoAmount = predictionMapper.getAutoTransferAmountByChild(userNo);
					if (autoAmount == null)
						autoAmount = 0.0;

					Integer transCycle = predictionMapper.getAutoTransferCycleByChild(userNo);
					if (transCycle == null || transCycle <= 0)
						transCycle = 7; // 기본 7일

					Date lastTransDate = predictionMapper.getAutoTransferLastDateByChild(userNo);
					LocalDate lastDate = (lastTransDate != null)
							? lastTransDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
							: today;

					// 다음 지급일 >= today 로 보정
					// (transCycle 방어 덕분에 무한루프 없음)
					while (lastDate.isBefore(today)) {
						lastDate = lastDate.plusDays(transCycle);
					}

					// 향후 90일치 자동이체 스케줄 생성 (오프셋 기반)
					Set<Integer> payOffsets = new HashSet<>();
					for (LocalDate d = lastDate; !d.isAfter(today.plusDays(forecastDays - 1)); d = d
							.plusDays(transCycle)) {
						int offset = (int) ChronoUnit.DAYS.between(today, d);
						if (offset >= 0 && offset < forecastDays)
							payOffsets.add(offset);
					}

					List<Double> dailyAutoAmount = new ArrayList<>(forecastDays);
					for (int i = 0; i < forecastDays; i++) {
						dailyAutoAmount.add(payOffsets.contains(i) ? autoAmount : 0.0);
					}

					Double currentBalance = predictionMapper.getChildCurrentBalance(userNo);
					if (currentBalance == null)
						currentBalance = 0.0;

					Double targetAmount = predictionMapper.getChildTargetAmount(userNo);
					if (targetAmount == null)
						targetAmount = 0.0;

					String childName = (String) childExpenses.get(0).get("USER_NAME");
					payload2.add(Arrays.asList(
							userNo,
							currentBalance,
							dailyAutoAmount,
							targetAmount,
							childName));
				}

				// JSON 구조를 {payload: [...], payload2: [...]} 형태로 변환
				Map<String, Object> finalPayload = new HashMap<>();
				finalPayload.put("payload", payload);
				finalPayload.put("payload2", payload2);

				String jsonInput = objectMapper.writeValueAsString(finalPayload);

				// -------------------------
				// 파이썬 호출: stdin 파이프 금지 → 임시 파일 I/O
				// -------------------------
				Path scriptAbs = Paths.get(SCRIPT_PATH).toAbsolutePath();
				if (!Files.exists(scriptAbs)) {
					System.err.println("[predict] Python script not found: " + scriptAbs);
					continue;
				}

				Path inFile = Files.createTempFile("pred_in_", ".json");
				Path outFile = Files.createTempFile("pred_out_", ".json");

				try (BufferedWriter bw = Files.newBufferedWriter(inFile, StandardCharsets.UTF_8)) {
					bw.write(jsonInput);
				}

				ProcessBuilder pb = new ProcessBuilder(
						PYTHON_CMD,
						scriptAbs.toString(),
						"--in", inFile.toString(),
						"--out", outFile.toString());
				pb.redirectErrorStream(true); // stdout에 합침
				Process p = pb.start();

				String log;
				try (InputStream is = p.getInputStream()) {
					log = new String(is.readAllBytes(), StandardCharsets.UTF_8);
				}

				boolean finished = p.waitFor(120, TimeUnit.SECONDS);
				if (!finished) {
					p.destroyForcibly();
					System.err.println("[predict] Python timeout\n" + log);
					safeDelete(inFile);
					safeDelete(outFile);
					continue;
				}
				int exit = p.exitValue();
				if (exit != 0) {
					System.err.println("[predict] Python failed. exit=" + exit + "\n" + log);
					safeDelete(inFile);
					safeDelete(outFile);
					continue;
				}

				String resultJson;
				try (BufferedReader r = Files.newBufferedReader(outFile, StandardCharsets.UTF_8)) {
					StringBuilder sb = new StringBuilder();
					String line;
					while ((line = r.readLine()) != null)
						sb.append(line);
					resultJson = sb.toString();
				}

				@SuppressWarnings("unchecked")
				Map<String, Object> pythonResult = objectMapper.readValue(resultJson, Map.class);

				@SuppressWarnings("unchecked")
				Map<String, String> charts = (Map<String, String>) pythonResult.getOrDefault("charts",
						Collections.emptyMap());

				sendPredictionEmail(email, new ArrayList<>(charts.values()));
				predictionMapper.updateParentLastSendDate(parentNo);

				safeDelete(inFile);
				safeDelete(outFile);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void safeDelete(Path p) {
		try {
			Files.deleteIfExists(p);
		} catch (Exception ignore) {
		}
	}

	private Integer getInteger(Object obj) {
		if (obj == null)
			return null;
		if (obj instanceof BigDecimal)
			return ((BigDecimal) obj).intValue();
		if (obj instanceof Integer)
			return (Integer) obj;
		if (obj instanceof Long)
			return ((Long) obj).intValue();
		if (obj instanceof Short)
			return ((Short) obj).intValue();
		return null;
	}

	private void sendPredictionEmail(String toEmail, List<String> base64Charts) {
		try {
			MimeMessage message = mailSender.createMimeMessage();

			// ★ CID 인라인을 쓰려면 related 멀티파트가 필요함
			MimeMessageHelper helper = new MimeMessageHelper(
					message,
					MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
					"UTF-8");

			helper.setTo(toEmail);
			helper.setSubject("자녀 소비 & 용돈 그래프");

			StringBuilder html = new StringBuilder()
					.append("<table role='presentation' width='100%' cellspacing='0' cellpadding='0' style='margin:0;padding:0;border:0;'>")
					.append("<tr><td align='center'>")
					.append("<table role='presentation' width='800' cellspacing='0' cellpadding='0' ")
					.append("style='width:800px;max-width:100%;font-family:Malgun Gothic,Apple SD Gothic Neo,Arial,sans-serif;'>")
					.append("<tr><td style='padding:16px 12px'>")
					.append("<p style='margin:0 0 12px 0;font-size:16px;'>자녀 소비 및 정기 입금 그래프를 첨부합니다.</p>");

			// HTML에는 data:URL 대신 cid: 참조
			for (int i = 0; i < base64Charts.size(); i++) {
				String cid = "chart" + i; // Content-ID 값
				html.append("<h3 style='margin:16px 0 8px 0;font-size:18px;'>자녀 ")
						.append(i + 1)
						.append("</h3>")
						.append("<img src=\"cid:")
						.append(cid)
						.append("\" ")
						.append("width='900' ")
						.append("style='display:block;max-width:900px;width:100%;height:auto;border:0;line-height:0;-ms-interpolation-mode:bicubic;' ")
						.append("alt='자녀 ")
						.append(i + 1)
						.append(" 그래프'>");
			}

			html.append("</td></tr></table></td></tr></table>");

			helper.setText(html.toString(), true);

			// ★ 실제 이미지 바이너리를 CID로 첨부
			for (int i = 0; i < base64Charts.size(); i++) {
				byte[] img = Base64.getDecoder().decode(base64Charts.get(i));
				String cid = "chart" + i;
				helper.addInline(cid, new ByteArrayResource(img), "image/png");
			}

			mailSender.send(message);
			System.out.println("[Email] " + toEmail + " 발송 완료.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}