package com.ddak.yongha.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

import com.ddak.yongha.service.ChildConsumeService;
import com.ddak.yongha.service.TransferService;
import com.ddak.yongha.service.UsersService2;
import com.ddak.yongha.vo.ConsumeVO;
import com.ddak.yongha.vo.Window;

@Controller
public class StatisticsController {

	@Autowired
	TransferService transferService;

	@Autowired
	UsersService2 usersService2;

	@Autowired
	ChildConsumeService childConsumeService;

	// 관리자의 부모의 용돈 총 이체량(회원 전체의 이체량)f
	@GetMapping("/totalTransferAmount")
	public String totalTransferAmount(Model model) {
		List<Map<String, Object>> dailyTransfers = transferService.getDailyTransferAmountAllAccounts();

		try {
			// 날짜, 금액 문자열 직렬화: "YYYY-MM-DD,amount;YYYY-MM-DD,amount;..."
			StringBuilder sb = new StringBuilder();
			for (Map<String, Object> row : dailyTransfers) {
				String date = (String) row.get("TRANS_DATE");
				Number amount = (Number) row.get("DAILY_AMOUNT");
				sb.append(date).append(",").append(amount.longValue()).append(";");
			}
			if (sb.length() > 0)
				sb.setLength(sb.length() - 1); // 마지막 세미콜론 제거

			List<String> command = List.of(
					"python",
					Path.of("src/scripts/generate_graph.py").toAbsolutePath().toString(),
					sb.toString());

			ProcessBuilder pb = new ProcessBuilder(command);
			pb.redirectErrorStream(true);
			Process process = pb.start();

			StringBuilder output = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line);
				}
			}

			int exitCode = process.waitFor();
			if (exitCode != 0 || output.toString().trim().isEmpty()) {
				model.addAttribute("error", "Python script failed or produced no output");
				return "error";
			}

			model.addAttribute("graphData", "data:image/png;base64," + output.toString().trim());

		} catch (Exception e) {
			model.addAttribute("error", e.getMessage());
			return "error";
		}

		return "TestStatistics";
	}

	// 관리자의 자녀와 부모 유저 비율
	@GetMapping("/childParentUserRatio")
	public String childParentUserRatio(Model model) {
		try {
			Map<String, Integer> counts = usersService2.getChildParentCounts();
			int parents = counts.get("parents");
			int children = counts.get("children");

			// Python 스크립트 경로
			Path scriptPath = Path.of("src/scripts/generate_graph2.py").toAbsolutePath();

			List<String> command = List.of(
					"python",
					scriptPath.toString(),
					String.valueOf(parents),
					String.valueOf(children));

			ProcessBuilder pb = new ProcessBuilder(command);
			pb.redirectErrorStream(true);
			Process process = pb.start();

			StringBuilder output = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line);
				}
			}

			int exitCode = process.waitFor();
			if (exitCode != 0 || output.isEmpty()) {
				model.addAttribute("error", "Python script failed or produced no output");
				return "error";
			}

			String base64Image = output.toString().trim();
			model.addAttribute("graphData", "data:image/png;base64," + base64Image);

		} catch (Exception e) {
			model.addAttribute("error", e.getMessage());
			return "error";
		}

		return "TestStatistics2"; // HTML에서 <img th:src="${graphData}"/>
	}

	// 자녀의 소비 통계 (자녀)

	@GetMapping("/childConsumeStats")
	public String childConsumeStats(@SessionAttribute("user_no") Integer userNo, Model model) {
		if (userNo == null) {
			model.addAttribute("error", "로그인이 필요합니다.");
			return "error";
		}

		try {
			LocalDate today = LocalDate.now();
			LocalDate weekAgo = today.minusDays(6); // 오늘 포함 7일

			List<ConsumeVO> lastWeekConsumes = childConsumeService.getTypeRatio(userNo, Window.DAY_7).getConsumes();

			Map<LocalDate, Long> dailyMap = new LinkedHashMap<>();
			for (int i = 0; i <= 6; i++) {
				dailyMap.put(weekAgo.plusDays(i), 0L);
			}
			for (ConsumeVO vo : lastWeekConsumes) {
				LocalDate date = vo.getCons_date().toLocalDate();
				dailyMap.put(date, dailyMap.getOrDefault(date, 0L) + vo.getAmount());
			}

			StringBuilder sb = new StringBuilder();
			for (Map.Entry<LocalDate, Long> entry : dailyMap.entrySet()) {
				sb.append(entry.getKey()).append(",").append(entry.getValue()).append(";");
			}
			if (sb.length() > 0)
				sb.setLength(sb.length() - 1);

			Path scriptPath = Path.of("src/scripts/generate_graph3.py").toAbsolutePath();
			List<String> command = List.of("python", scriptPath.toString(), sb.toString());

			ProcessBuilder pb = new ProcessBuilder(command);
			pb.redirectErrorStream(true);
			Process process = pb.start();

			StringBuilder output = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line);
				}
			}

			int exitCode = process.waitFor();
			String outAll = output.toString().trim();
			if (exitCode != 0 || outAll.isEmpty()) {
				// ↓↓↓ 디버깅용: 일시적으로 원문을 보자 (운영 반영 X)
				String preview = outAll.length() > 2000
						? outAll.substring(0, 2000) + "...(truncated)"
						: outAll;
				model.addAttribute("error",
						"Python failed. exit=" + exitCode + "\nOUT/ERR:\n" + preview);
				return "error";
			}

			model.addAttribute("graphData", "data:image/png;base64," + output.toString().trim());

		} catch (Exception e) {
			model.addAttribute("error", e.getMessage());
			return "error";
		}

		return "TestStatistics3";
	}

	// 자녀의 소비 통계 (부모)
}
