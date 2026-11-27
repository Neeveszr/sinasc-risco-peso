package br.edu.unifsa.sinasc.modelo;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** O número só pode ser um dos valores listados (nulo é aceito; use @NotNull se for obrigatório). */
@Documented
@Constraint(validatedBy = ValorEmValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValorEm {

    int[] valores();

    String message() default "valor não permitido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
