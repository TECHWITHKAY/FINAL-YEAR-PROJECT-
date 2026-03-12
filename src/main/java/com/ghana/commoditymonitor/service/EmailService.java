package com.ghana.commoditymonitor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service to handle sending email notifications.
 * Supports a "mock" mode for development when SMTP is not configured.
 */
@Slf4j
@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    public void sendEmail(String to, String subject, String body) {
        if (!mailEnabled) {
            log.info("[MOCK EMAIL] To: {}, Subject: {}, Body: {}", to, subject, body);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    public void sendAccountActivatedEmail(String to, String username) {
        String subject = "Account Activated - CommodityGH";
        String body = String.format(
                "Hello %s,\n\n" +
                "Good news! Your account on CommodityGH has been approved by an administrator.\n" +
                "You can now log in and start contributing data to the platform.\n\n" +
                "Login here: %s/login\n\n" +
                "Best regards,\n" +
                "The CommodityGH Team",
                username, frontendBaseUrl
        );
        sendEmail(to, subject, body);
    }
}
