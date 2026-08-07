package com.SocialPairly_Workflow_Manager.service;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class TextModerationService {

    private static final Pattern SPAM_PATTERN = Pattern.compile(
            "(?i)(@[a-zA-Z0-9_.]+|https?://\\S+|www\\.\\S+|insta|ig:|snap:|wa\\.me|\\+?\\d{10,12})"
    );

    public boolean containsForbiddenText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return SPAM_PATTERN.matcher(text).find();
    }
}