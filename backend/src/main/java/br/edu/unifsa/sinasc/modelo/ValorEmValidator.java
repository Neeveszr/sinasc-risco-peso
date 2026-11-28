package br.edu.unifsa.sinasc.modelo;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValorEmValidator implements ConstraintValidator<ValorEm, Integer> {

    private Set<Integer> permitidos;

    @Override
    public void initialize(ValorEm anotacao) {
        permitidos = Arrays.stream(anotacao.valores()).boxed().collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(Integer valor, ConstraintValidatorContext contexto) {
        return valor == null || permitidos.contains(valor);
    }
}
