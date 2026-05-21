package com.devflow.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${devflow.email.from:noreply@devflow.app}")
    private String fromEmail;

    @Value("${devflow.email.from-name:DevFlow}")
    private String fromName;

    @Value("${devflow.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Async
    public void sendWelcomeEmail(String toEmail, String name) {
        log.info("Sending welcome email to {}", toEmail);
        try {
            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("loginUrl", frontendUrl + "/login");

            String htmlContent = templateEngine.process("welcome-email", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("¡Te damos la bienvenida a DevFlow!");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Welcome email sent successfully to {}", toEmail);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send welcome email to {}", toEmail, e);
        }
    }

    @Async
    public void sendTaskAssignedEmail(String toEmail, String taskTitle, String projectName, String assignedBy, String projectId, String taskId) {
        log.info("Sending task assignment email to {}", toEmail);
        try {
            Context context = new Context();
            context.setVariable("taskTitle", taskTitle);
            context.setVariable("projectName", projectName);
            context.setVariable("assignedBy", assignedBy);
            context.setVariable("taskUrl", frontendUrl + "/projects/" + projectId + "/tasks/" + taskId);

            String htmlContent = templateEngine.process("task-assigned-email", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Nueva tarea asignada: " + taskTitle);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Task assignment email sent successfully to {}", toEmail);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send task assignment email to {}", toEmail, e);
        }
    }
}
