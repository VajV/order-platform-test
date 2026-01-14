package com.ecommerce.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.security.auth.Subject;

@Service
@Profile({"default", "dev", "test"})
@Slf4j
public class DevEmailService implements EmailService {

    @Override
    public void send(String to, String subject, String htmlBody) {
        log.info("""
            
            ================================================================================
            📧 DEV EMAIL SERVICE (Mock - not sending real email)
            ================================================================================
            To:      {}
            Subject: {}
            ================================================================================
            Body:
            {}
            ================================================================================
            """, to, subject, htmlBody);
    }
}
