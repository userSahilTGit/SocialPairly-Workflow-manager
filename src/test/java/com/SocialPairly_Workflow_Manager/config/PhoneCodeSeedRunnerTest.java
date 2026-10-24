package com.SocialPairly_Workflow_Manager.config;

import com.SocialPairly_Workflow_Manager.entity.RefCountry;
import com.SocialPairly_Workflow_Manager.repository.RefCountryRepository;
import com.SocialPairly_Workflow_Manager.util.PhoneNumberNormalizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhoneCodeSeedRunnerTest {

    @Mock
    private RefCountryRepository refCountryRepository;

    @Test
    void loadsPhoneSeedsFromClasspathFile() throws Exception {
        var seeds = PhoneCodeSeedRunner.loadPhoneSeeds();
        assertTrue(seeds.size() >= 190);
        assertEquals("+91", seeds.get("IN").dialCode());
        assertEquals(10, seeds.get("IN").nationalLength());
        assertEquals("+971", seeds.get("AE").dialCode());
        assertEquals(9, seeds.get("AE").nationalLength());
        assertEquals("+1", seeds.get("US").dialCode());
        assertEquals(10, seeds.get("US").nationalLength());
    }

    @Test
    void runUpdatesMissingPhoneCodeAndNationalLength() throws Exception {
        when(refCountryRepository.findById(anyString())).thenAnswer(inv -> {
            String code = inv.getArgument(0);
            if (!"IN".equals(code)) {
                return Optional.empty();
            }
            RefCountry india = new RefCountry();
            india.setCode("IN");
            india.setName("India");
            india.setActive(true);
            return Optional.of(india);
        });
        when(refCountryRepository.save(any(RefCountry.class))).thenAnswer(inv -> inv.getArgument(0));

        PhoneCodeSeedRunner runner = new PhoneCodeSeedRunner(refCountryRepository);
        runner.run(new DefaultApplicationArguments());

        ArgumentCaptor<RefCountry> captor = ArgumentCaptor.forClass(RefCountry.class);
        verify(refCountryRepository, atLeastOnce()).save(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(c ->
                "IN".equals(c.getCode())
                        && "+91".equals(c.getPhoneCode())
                        && Integer.valueOf(10).equals(c.getPhoneNationalLength())));
        assertEquals(10, PhoneNumberNormalizer.nationalLengthFor("+91"));
    }

    @Test
    void runSkipsSaveWhenAlreadySeeded() throws Exception {
        when(refCountryRepository.findById(anyString())).thenAnswer(inv -> {
            String code = inv.getArgument(0);
            RefCountry row = new RefCountry();
            row.setCode(code);
            row.setName(code);
            row.setActive(true);
            if ("IN".equals(code)) {
                row.setPhoneCode("+91");
                row.setPhoneNationalLength(10);
            } else {
                // Force mismatch so runner may save others; IN must not be saved again.
                row.setPhoneCode("+999");
                row.setPhoneNationalLength(8);
            }
            return Optional.of(row);
        });

        PhoneCodeSeedRunner runner = new PhoneCodeSeedRunner(refCountryRepository);
        runner.run(new DefaultApplicationArguments());

        verify(refCountryRepository, never()).save(argThat(c -> c != null && "IN".equals(c.getCode())));
    }

    @Test
    void parserSkipsCommentsBlankAndMalformedRows() throws Exception {
        String raw = """
                # comment

                ONLY|TWO
                USA|United States|1|10
                in|India|91|10
                gb|United Kingdom|+44|bad
                fr|France|33|99
                de|Germany|+49 30|7
                zz|Nowhere|+|10
                """;
        var seeds = PhoneCodeSeedRunner.loadPhoneSeeds(new BufferedReader(new StringReader(raw)));
        assertEquals("+91", seeds.get("IN").dialCode());
        assertEquals(10, seeds.get("IN").nationalLength());
        assertEquals("+44", seeds.get("GB").dialCode());
        assertEquals(10, seeds.get("GB").nationalLength());
        assertEquals(10, seeds.get("FR").nationalLength());
        assertEquals("+4930", seeds.get("DE").dialCode());
        assertEquals(7, seeds.get("DE").nationalLength());
        assertFalse(seeds.containsKey("USA"));
        assertFalse(seeds.containsKey("ZZ"));
    }
}
