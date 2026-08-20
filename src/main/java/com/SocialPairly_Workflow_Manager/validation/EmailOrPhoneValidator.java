package com.SocialPairly_Workflow_Manager.validation;

import com.SocialPairly_Workflow_Manager.util.ContactIdentifier;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EmailOrPhoneValidator implements ConstraintValidator<EmailOrPhone, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // @NotBlank owns empty checks; allow null here so constraints compose cleanly
        if (value == null || value.isBlank()) {
            return true;
        }
        return ContactIdentifier.isValidEmailOrPhone(value);
    }
}
