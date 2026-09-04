package com.SocialPairly_Workflow_Manager.config;

import com.SocialPairly_Workflow_Manager.entity.RefCountry;
import com.SocialPairly_Workflow_Manager.repository.RefCountryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ensures identity/education country dropdowns have a broad active list (defect 1552).
 * Upserts by ISO code so existing postal regexes are preserved when already set.
 */
@Component
@Order(1)
public class RefCountryDataInitializer implements CommandLineRunner {

    private final RefCountryRepository refCountryRepository;

    public RefCountryDataInitializer(RefCountryRepository refCountryRepository) {
        this.refCountryRepository = refCountryRepository;
    }

    @Override
    public void run(String... args) {
        Map<String, String> countries = seedCountries();
        Map<String, String> postal = postalRegexes();
        int upserted = 0;
        for (Map.Entry<String, String> entry : countries.entrySet()) {
            String code = entry.getKey();
            RefCountry row = refCountryRepository.findById(code).orElseGet(RefCountry::new);
            boolean isNew = row.getCode() == null;
            row.setCode(code);
            row.setName(entry.getValue());
            row.setActive(true);
            if (isNew || row.getPostalRegex() == null || row.getPostalRegex().isBlank()) {
                row.setPostalRegex(postal.get(code));
            }
            refCountryRepository.save(row);
            upserted++;
        }
        System.out.println(">> Ensured " + upserted + " active ref_countries for identity dropdowns");
    }

    private static Map<String, String> postalRegexes() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("AU", "^[0-9]{4}$");
        m.put("BD", "^[0-9]{4}$");
        m.put("BR", "^[0-9]{5}-?[0-9]{3}$");
        m.put("CA", "^[A-Za-z][0-9][A-Za-z][ ]?[0-9][A-Za-z][0-9]$");
        m.put("CN", "^[0-9]{6}$");
        m.put("FR", "^[0-9]{5}$");
        m.put("DE", "^[0-9]{5}$");
        m.put("IN", "^[1-9][0-9]{5}$");
        m.put("IT", "^[0-9]{5}$");
        m.put("JP", "^[0-9]{3}-?[0-9]{4}$");
        m.put("MX", "^[0-9]{5}$");
        m.put("PK", "^[0-9]{5}$");
        m.put("SG", "^[0-9]{6}$");
        m.put("ES", "^[0-9]{5}$");
        m.put("AE", "^[0-9]{5}$");
        m.put("GB", "^(GIR 0AA|[A-Za-z]{1,2}[0-9][A-Za-z0-9]? [0-9][A-Za-z]{2})$");
        m.put("US", "^[0-9]{5}(-[0-9]{4})?$");
        return m;
    }

    private static Map<String, String> seedCountries() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("AF", "Afghanistan");
        m.put("AL", "Albania");
        m.put("DZ", "Algeria");
        m.put("AR", "Argentina");
        m.put("AM", "Armenia");
        m.put("AU", "Australia");
        m.put("AT", "Austria");
        m.put("AZ", "Azerbaijan");
        m.put("BH", "Bahrain");
        m.put("BD", "Bangladesh");
        m.put("BY", "Belarus");
        m.put("BE", "Belgium");
        m.put("BZ", "Belize");
        m.put("BJ", "Benin");
        m.put("BT", "Bhutan");
        m.put("BO", "Bolivia");
        m.put("BA", "Bosnia and Herzegovina");
        m.put("BW", "Botswana");
        m.put("BR", "Brazil");
        m.put("BN", "Brunei");
        m.put("BG", "Bulgaria");
        m.put("KH", "Cambodia");
        m.put("CM", "Cameroon");
        m.put("CA", "Canada");
        m.put("CL", "Chile");
        m.put("CN", "China");
        m.put("CO", "Colombia");
        m.put("CR", "Costa Rica");
        m.put("HR", "Croatia");
        m.put("CU", "Cuba");
        m.put("CY", "Cyprus");
        m.put("CZ", "Czechia");
        m.put("DK", "Denmark");
        m.put("DO", "Dominican Republic");
        m.put("EC", "Ecuador");
        m.put("EG", "Egypt");
        m.put("SV", "El Salvador");
        m.put("EE", "Estonia");
        m.put("ET", "Ethiopia");
        m.put("FI", "Finland");
        m.put("FR", "France");
        m.put("GE", "Georgia");
        m.put("DE", "Germany");
        m.put("GH", "Ghana");
        m.put("GR", "Greece");
        m.put("GT", "Guatemala");
        m.put("HK", "Hong Kong");
        m.put("HU", "Hungary");
        m.put("IS", "Iceland");
        m.put("IN", "India");
        m.put("ID", "Indonesia");
        m.put("IR", "Iran");
        m.put("IQ", "Iraq");
        m.put("IE", "Ireland");
        m.put("IL", "Israel");
        m.put("IT", "Italy");
        m.put("JM", "Jamaica");
        m.put("JP", "Japan");
        m.put("JO", "Jordan");
        m.put("KZ", "Kazakhstan");
        m.put("KE", "Kenya");
        m.put("KW", "Kuwait");
        m.put("LV", "Latvia");
        m.put("LB", "Lebanon");
        m.put("LY", "Libya");
        m.put("LT", "Lithuania");
        m.put("LU", "Luxembourg");
        m.put("MO", "Macao");
        m.put("MY", "Malaysia");
        m.put("MV", "Maldives");
        m.put("MT", "Malta");
        m.put("MX", "Mexico");
        m.put("MD", "Moldova");
        m.put("MC", "Monaco");
        m.put("MN", "Mongolia");
        m.put("ME", "Montenegro");
        m.put("MA", "Morocco");
        m.put("MZ", "Mozambique");
        m.put("MM", "Myanmar");
        m.put("NP", "Nepal");
        m.put("NL", "Netherlands");
        m.put("NZ", "New Zealand");
        m.put("NG", "Nigeria");
        m.put("MK", "North Macedonia");
        m.put("NO", "Norway");
        m.put("OM", "Oman");
        m.put("PK", "Pakistan");
        m.put("PA", "Panama");
        m.put("PY", "Paraguay");
        m.put("PE", "Peru");
        m.put("PH", "Philippines");
        m.put("PL", "Poland");
        m.put("PT", "Portugal");
        m.put("QA", "Qatar");
        m.put("RO", "Romania");
        m.put("RU", "Russia");
        m.put("SA", "Saudi Arabia");
        m.put("RS", "Serbia");
        m.put("SG", "Singapore");
        m.put("SK", "Slovakia");
        m.put("SI", "Slovenia");
        m.put("ZA", "South Africa");
        m.put("KR", "South Korea");
        m.put("ES", "Spain");
        m.put("LK", "Sri Lanka");
        m.put("SE", "Sweden");
        m.put("CH", "Switzerland");
        m.put("SY", "Syria");
        m.put("TW", "Taiwan");
        m.put("TZ", "Tanzania");
        m.put("TH", "Thailand");
        m.put("TT", "Trinidad and Tobago");
        m.put("TN", "Tunisia");
        m.put("TR", "Turkey");
        m.put("UG", "Uganda");
        m.put("UA", "Ukraine");
        m.put("AE", "United Arab Emirates");
        m.put("GB", "United Kingdom");
        m.put("US", "United States");
        m.put("UY", "Uruguay");
        m.put("UZ", "Uzbekistan");
        m.put("VE", "Venezuela");
        m.put("VN", "Vietnam");
        m.put("YE", "Yemen");
        m.put("ZM", "Zambia");
        m.put("ZW", "Zimbabwe");
        return m;
    }
}
