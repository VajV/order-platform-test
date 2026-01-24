package com.orderplatform.order.dto.request;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DecimalPositiveValidator.class)
public @interface DecimalPositive {
    String message() default "Must be positive";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
