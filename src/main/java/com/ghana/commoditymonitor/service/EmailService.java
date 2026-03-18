package com.ghana.commoditymonitor.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${app.mail.from-address}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    // ── PASSWORD RESET ──────────────────────────────────────────────────────────

    @Async
    public void sendPasswordResetEmail(String toEmail, String username, String rawToken) {
        if (!mailEnabled) { log.info("Mail disabled. Skipping password reset email to {}", toEmail); return; }
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("username", username);
            ctx.setVariable("resetLink", frontendBaseUrl + "/reset-password?token=" + rawToken);
            ctx.setVariable("expiryHours", 1);

            String html = templateEngine.process("email/password-reset", ctx);
            sendHtml(toEmail, "Reset Your CommodityGH Password", html);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    // ── AGENT: APPLICATION RECEIVED ────────────────────────────────────────────

    @Async
    public void sendAgentApplicationReceivedEmail(String toEmail, String username) {
        if (!mailEnabled) { log.info("Mail disabled. Skipping agent received email to {}", toEmail); return; }
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("username", username);

            String html = templateEngine.process("email/agent-application-received", ctx);
            sendHtml(toEmail, "We Received Your Field Agent Application — CommodityGH", html);
            log.info("Agent application received email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send agent received email to {}: {}", toEmail, e.getMessage());
        }
    }

    // ── AGENT: NOTIFY ADMIN ────────────────────────────────────────────────────

    @Async
    public void sendAgentApplicationAdminNotifyEmail(String adminEmail, String agentUsername,
                                                      String agentEmail, String operatingCity,
                                                      String applicationNote) {
        if (!mailEnabled) { log.info("Mail disabled. Skipping admin notify email."); return; }
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("agentUsername", agentUsername);
            ctx.setVariable("agentEmail", agentEmail);
            ctx.setVariable("operatingCity", operatingCity != null ? operatingCity : "Not specified");
            ctx.setVariable("applicationNote", applicationNote != null ? applicationNote : "None provided");
            ctx.setVariable("adminLink", frontendBaseUrl + "/admin");

            String html = templateEngine.process("email/agent-application-admin-notify", ctx);
            sendHtml(adminEmail, "New Field Agent Application — " + agentUsername, html);
            log.info("Admin notified of new agent application from {}", agentUsername);
        } catch (Exception e) {
            log.error("Failed to send admin notify email: {}", e.getMessage());
        }
    }

    // ── AGENT: APPROVED ────────────────────────────────────────────────────────

    @Async
    public void sendAgentApprovedEmail(String toEmail, String username) {
        sendAccountActivatedEmail(toEmail, username);
    }

    @Async
    public void sendAccountActivatedEmail(String toEmail, String username) {
        if (!mailEnabled) { log.info("Mail disabled. Skipping activation email to {}", toEmail); return; }
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("username", username);
            ctx.setVariable("loginLink", frontendBaseUrl + "/login");

            String html = templateEngine.process("email/agent-approved", ctx);
            sendHtml(toEmail, "Your CommodityGH Account is Active", html);
            log.info("Account activation email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send activation email to {}: {}", toEmail, e.getMessage());
        }
    }

    // ── AGENT: REJECTED ────────────────────────────────────────────────────────

    @Async
    public void sendAgentRejectedEmail(String toEmail, String username, String reason) {
        if (!mailEnabled) { log.info("Mail disabled. Skipping agent rejected email to {}", toEmail); return; }
        try {
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("username", username);
            ctx.setVariable("reason", (reason != null && !reason.isBlank())
                    ? reason
                    : "Your application did not meet our current requirements.");
            ctx.setVariable("contactEmail", fromAddress);

            String html = templateEngine.process("email/agent-rejected", ctx);
            sendHtml(toEmail, "Update on Your Field Agent Application — CommodityGH", html);
            log.info("Agent rejected email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send agent rejected email to {}: {}", toEmail, e.getMessage());
        }
    }

    // ── PRIVATE ────────────────────────────────────────────────────────────────

    private void sendHtml(String to, String subject, String htmlBody) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(new InternetAddress(fromAddress, fromName));
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        mailSender.send(message);
    }
}
