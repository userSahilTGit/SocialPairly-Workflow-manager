package com.SocialPairly_Workflow_Manager.config;

import com.SocialPairly_Workflow_Manager.entity.RefCountry;
import com.SocialPairly_Workflow_Manager.repository.RefCountryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Ensures {@code ref_countries} contains the full ISO country list for nationality dropdowns (#1552).
 * Existing rows (with postal_regex) are preserved; only missing codes are inserted.
 */
@Component
public class WorldCountrySeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WorldCountrySeedRunner.class);

    private final RefCountryRepository refCountryRepository;

    public WorldCountrySeedRunner(RefCountryRepository refCountryRepository) {
        this.refCountryRepository = refCountryRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        Map<String, String> world = loadWorldCountries();
        int inserted = 0;
        for (Map.Entry<String, String> entry : world.entrySet()) {
            if (refCountryRepository.existsById(entry.getKey())) {
                continue;
            }
            RefCountry country = new RefCountry();
            country.setCode(entry.getKey());
            country.setName(entry.getValue());
            country.setActive(true);
            refCountryRepository.save(country);
            inserted++;
        }
        if (inserted > 0) {
            log.info("Seeded {} world countries into ref_countries", inserted);
        }
    }

    static Map<String, String> loadWorldCountries() throws Exception {
        Map<String, String> map = new HashMap<>();
        ClassPathResource resource = new ClassPathResource("reference/world-countries.txt");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int sep = line.indexOf('|');
                if (sep <= 0 || sep >= line.length() - 1) {
                    continue;
                }
                String code = line.substring(0, sep).trim().toUpperCase();
                String name = line.substring(sep + 1).trim();
                if (code.length() == 2 && !name.isEmpty()) {
                    map.put(code, name);
                }
            }
        }
        return map;
    }
}
