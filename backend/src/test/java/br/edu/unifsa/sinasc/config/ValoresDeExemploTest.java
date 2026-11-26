package br.edu.unifsa.sinasc.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ValoresDeExemploTest {

    @Test
    void reconheceValoresDeExemplo() {
        assertTrue(ValoresDeExemplo.ehPlaceholder("troque-esta-senha"));
        assertTrue(ValoresDeExemplo.ehPlaceholder("  Troque-Por-Um-Segredo"));
    }

    @Test
    void aceitaValoresReaisEVazio() {
        assertFalse(ValoresDeExemplo.ehPlaceholder("d33ffa77031cde037bf092a45f3487ef"));
        assertFalse(ValoresDeExemplo.ehPlaceholder(""));
        assertFalse(ValoresDeExemplo.ehPlaceholder(null));
    }
}
