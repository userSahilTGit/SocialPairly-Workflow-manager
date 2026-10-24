package com.SocialPairly_Workflow_Manager.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailOrPhoneValidatorTest {

    private final EmailOrPhoneValidator validator = new EmailOrPhoneValidator();

    @Test
    void blankValuesAreLeftToNotBlankAndRealContactsAreChecked() {
        assertTrue(validator.isValid(null, null));
        assertTrue(validator.isValid("   ", null));
        assertTrue(validator.isValid("ada@example.com", null));
        assertTrue(validator.isValid("+12025550123", null));
        assertFalse(validator.isValid("not-a-contact", null));
    }
}
