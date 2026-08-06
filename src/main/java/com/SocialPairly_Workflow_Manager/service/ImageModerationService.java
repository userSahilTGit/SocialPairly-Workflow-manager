package com.SocialPairly_Workflow_Manager.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
public class ImageModerationService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String apiKey;

    public ImageModerationService(@Value("${moderatecontent.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        if (apiKey == null || apiKey.isBlank()) {
            log.info("ModerateContent API key not set - NSFW image checks are disabled (uploads allowed).");
        }
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public boolean isImageSafe(byte[] imageBytes) {
        if (!isEnabled() || imageBytes == null || imageBytes.length == 0) {
            return true;
        }
        try {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String url = "https://api.moderatecontent.com/moderate/?key=" + apiKey;
            Map<String, Object> body = Map.of("base64", "data:image/jpeg;base64," + base64Image);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, body, Map.class);

            if (response != null) {
                String rating = (String) response.get("rating_letter");
                if (rating != null) {
                    return "e".equalsIgnoreCase(rating);
                }
            }
            return true;
        } catch (Exception e) {
            log.error("ModerateContent API call failed; allowing image (fail-open).", e);
            return true;
        }
    }
}