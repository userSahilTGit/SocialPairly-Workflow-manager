package com.SocialPairly_Workflow_Manager.config;

import com.SocialPairly_Workflow_Manager.entity.RefCountry;
import com.SocialPairly_Workflow_Manager.repository.RefCountryRepository;
import com.SocialPairly_Workflow_Manager.util.PhoneNumberNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Seeds international dialing codes and strict national lengths onto {@code ref_countries} (BUG-1570).
 */
@Component
@Order(3)
public class PhoneCodeSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PhoneCodeSeedRunner.class);

    private final RefCountryRepository refCountryRepository;

    public PhoneCodeSeedRunner(RefCountryRepository refCountryRepository) {
        this.refCountryRepository = refCountryRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        Map<String, PhoneSeed> seeds = loadPhoneSeeds();
        int updated = 0;
        for (Map.Entry<String, PhoneSeed> entry : seeds.entrySet()) {
            RefCountry country = refCountryRepository.findById(entry.getKey()).orElse(null);
            if (country == null) {
                continue;
            }
            PhoneSeed seed = entry.getValue();
            boolean dirty = false;
            if (!seed.dialCode().equals(country.getPhoneCode())) {
                country.setPhoneCode(seed.dialCode());
                dirty = true;
            }
            if (!seed.nationalLength().equals(country.getPhoneNationalLength())) {
                country.setPhoneNationalLength(seed.nationalLength());
                dirty = true;
            }
            if (dirty) {
                refCountryRepository.save(country);
                updated++;
            }
            PhoneNumberNormalizer.registerNationalLength(seed.dialCode(), seed.nationalLength());
        }
        if (updated > 0) {
            log.info("Updated phone_code/national length on {} ref_countries rows (BUG-1570)", updated);
        }
    }

    static Map<String, PhoneSeed> loadPhoneSeeds() throws Exception {
        Map<String, PhoneSeed> map = new LinkedHashMap<>();
        ClassPathResource resource = new ClassPathResource("reference/world-phone-codes.txt");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\\|");
                if (parts.length < 3) {
                    continue;
                }
                String code = parts[0].trim().toUpperCase();
                String dial = parts[2].trim();
                if (!dial.startsWith("+")) {
                    dial = "+" + dial.replaceAll("\\D", "");
                } else {
                    dial = "+" + dial.substring(1).replaceAll("\\D", "");
                }
                int nationalLength = 10;
                if (parts.length >= 4) {
                    try {
                        nationalLength = Integer.parseInt(parts[3].trim());
                    } catch (NumberFormatException ignored) {
                        nationalLength = 10;
                    }
                }
                if (nationalLength < 4 || nationalLength > 15) {
                    nationalLength = 10;
                }
                if (code.length() == 2 && dial.length() > 1) {
                    map.put(code, new PhoneSeed(dial, nationalLength));
                }
            }
        }
        return map;
    }

    record PhoneSeed(String dialCode, Integer nationalLength) {}
}
