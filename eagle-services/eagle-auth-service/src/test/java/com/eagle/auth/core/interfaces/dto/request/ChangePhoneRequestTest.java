package com.eagle.auth.core.interfaces.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangePhoneRequestTest {

    private final Validator validator;

    ChangePhoneRequestTest() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            this.validator = factory.getValidator();
        }
    }

    private ChangePhoneRequest req(String phone, String code) {
        ChangePhoneRequest r = new ChangePhoneRequest();
        r.setPhone(phone);
        r.setCode(code);
        return r;
    }

    @Test
    void validWhenPhoneAndCodeWellFormed() {
        assertTrue(validator.validate(req("13900139000", "1234")).isEmpty());
    }

    @Test
    void invalidWhenPhonePatternWrong() {
        assertFalse(validator.validate(req("12345", "1234")).isEmpty());
    }

    @Test
    void invalidWhenCodeBlank() {
        assertFalse(validator.validate(req("13900139000", "")).isEmpty());
    }
}
