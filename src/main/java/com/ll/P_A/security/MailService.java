package com.ll.P_A.security;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    public void sendVerificationEmail(User user) {
        String to = user.getEmail();
        String subject = "[Project-A] 이메일 인증을 완료해주세요";
        String verificationLink = "http://localhost:8080/api/auth/verify-email?token=" + user.getEmailVerificationToken();

        String body = "<h1>이메일 인증</h1>"
                + "<p>안녕하세요, " + user.getNickname() + "님!</p>"
                + "<p>아래 링크를 클릭하여 이메일 인증을 완료해주세요.</p>"
                + "<a href='" + verificationLink + "'>이메일 인증하기</a>";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("메일 전송에 실패했습니다.", e);
        }
    }
}