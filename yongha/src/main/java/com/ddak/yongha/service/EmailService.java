package com.ddak.yongha.service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

	private final JavaMailSender mailSender;

	public EmailService(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

        @Async("mailExecutor")
        public void sendFamilyInviteMail(String to, String parentName, String acceptLink) {
                MimeMessage mail = mailSender.createMimeMessage();
                try {
                        byte[] qrBytes = generateQrImage(acceptLink);
                        MimeMessageHelper helper = new MimeMessageHelper(mail, true, "UTF-8");
                        helper.setTo(to);
                        helper.setSubject("가족 초대 안내");
                        helper.setText(
                                        parentName + "님으로부터 가족초대 입니다.<br> 초대를 원하시면 <a href='" + acceptLink + "'>여기</a>를 클릭해주세요.<br><img src='cid:qrImage' alt='QR코드'/>",
                                        true);
                        helper.addInline("qrImage", new ByteArrayResource(qrBytes), "image/png");
                        mailSender.send(mail);
                        System.out.println("이메일 발송 성공: " + to);
                } catch (Exception e) {
                        System.out.println("이메일 발송 실패: " + to + ", 에러: " + e.getMessage());
                }
        }

        public String createQrDataUrl(String link) throws Exception {
                return "data:image/png;base64," + Base64.getEncoder().encodeToString(generateQrImage(link));
        }

        private byte[] generateQrImage(String text) throws Exception {
                QRCodeWriter writer = new QRCodeWriter();
                BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, 200, 200);
                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
                        return out.toByteArray();
                }
        }
	
	// 인증코드 6자리 메일 전송
	@Async("mailExecutor")
	public void sendVerificationCodeMail(String to, String code) {
	    MimeMessage mail = mailSender.createMimeMessage();
	    try {
	        MimeMessageHelper helper = new MimeMessageHelper(mail, true, "UTF-8");
	        helper.setTo(to);
	        helper.setSubject("이메일 인증 코드");
	        helper.setText("인증코드: " + code + "\n\n이 코드는 5분간만 유효합니다.", false); // HTML 필요 없으면 false

	        mailSender.send(mail); // 전송
	        System.out.println("인증코드 이메일 발송 성공: " + to);
	    } catch (Exception e) {
	        System.out.println("인증코드 이메일 발송 실패: " + to + ", 에러: " + e.getMessage());
	    }
	}

}
