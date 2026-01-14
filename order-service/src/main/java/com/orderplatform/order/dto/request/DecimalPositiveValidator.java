package com.orderplatform.order.dto.request;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

public class DecimalPositiveValidator implements ConstraintValidator<DecimalPositive, BigDecimal> {

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        return value != null && value.signum() > 0;
    }
}
