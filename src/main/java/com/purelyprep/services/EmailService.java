package com.purelyprep.services;

import com.purelyprep.util.Util;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import jakarta.activation.DataSource;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;


import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


import java.nio.charset.StandardCharsets;

import java.util.Set;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static EmailService emailService;
    public static final String from = "devin.sills@gmail.com";

    private static JavaMailSender javaMailSender;

    @Autowired
    public EmailService(JavaMailSender javaMailSender){
        this.javaMailSender=javaMailSender;
    }
    public static EmailService getInstance() {
        return emailService;
    }

    @PostConstruct
    private void init() {
        emailService = this;
    }

    @Value("${sendgrid.api.key}")
    private String apiKey;

    public void sendPlainText(String from, Set<String> tos, String subject, String body) {
    	
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from);

            String[] toEmails = tos.stream()
                    .map(String::trim)  
                    .toArray(String[]::new);


            helper.setTo(toEmails);  
            helper.setText(body,false);
            helper.setSubject(subject);
            javaMailSender.send(mimeMessage);
            System.out.println("Email has been sent successfully to --->"+String.join(", ", tos));
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }


        //send(from, tos, subject, "text/plain", body);
    }

    public void sendHtml(String from, Set<String> tos, String subject, String body) {
        send(from, tos, subject, "text/html", body);
    }

    @Async
    public void send(String from, Set<String> tos, String subject, String type, String body) {
        if (tos.isEmpty()) {
            return;
        }

        Mail mail = new Mail();
        mail.from = new Email(from);
        Personalization personalization = new Personalization();
        for (String to : tos) {
            personalization.addTo(new Email(to));
        }
        mail.addPersonalization(personalization);
        mail.subject = subject;
        mail.addContent(new Content(type, body));

        SendGrid sg = new SendGrid(apiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            sg.api(request);
            log.info("Email sent to: [" + String.join(", ", tos) + "]");
        } catch (Exception ex) {
            log.error("Error sending email: ", ex);
        }
    }

}

