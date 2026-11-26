
package br.edu.unifsa.sinasc.analise;

import java.time.Instant;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;


public final class RegistroAnaliseDtos {

    private RegistroAnaliseDtos() {
    }

public record RegistroRequest(
        @Pattern(
            regexp = "^(10-14|15-19|20-24|25-29|30-34|35-39|40-44|45-49|>=50)$",
            message = "faixa etária inválida"
        )
        String faixaEtaria,

        Integer estadoCivil,

        Integer escolaridade,

        Integer tipoGravidez,

        @NotNull @Min(0) @Max(15)
        Integer gestacoesAnteriores,

        @NotNull @Min(0) @Max(15)
        Integer partosNormais,

        @NotNull @Min(0) @Max(15)
        Integer partosCesareos,

        @NotNull @Min(0) @Max(15)
        Integer filhosVivos,

        @NotNull @Min(0) @Max(15)
        Integer perdasFetais,

        @Min(1) @Max(9)
        Integer mesPreNatal
) {
    @AssertTrue(message = "estado civil inválido")
    public boolean isEstadoCivilValido() {
        return estadoCivil == null
                || (estadoCivil >= 1 && estadoCivil <= 5)
                || estadoCivil == 9;
    }

    @AssertTrue(message = "escolaridade inválida")
    public boolean isEscolaridadeValida() {
        return escolaridade == null
                || (escolaridade >= 0 && escolaridade <= 5)
                || escolaridade == 9;
    }

    @AssertTrue(message = "tipo de gravidez inválido")
    public boolean isTipoGravidezValido() {
        return tipoGravidez == null
                || (tipoGravidez >= 1 && tipoGravidez <= 3)
                || tipoGravidez == 9;
    }
}

   
         {
    }

    public record RegistroResponse(
            Long id,
            Long usuarioId,
            String faixaEtaria,
            Integer estadoCivil,
            Integer escolaridade,
            Integer tipoGravidez,
            Integer gestacoesAnteriores,
            Integer partosNormais,
            Integer partosCesareos,
            Integer filhosVivos,
            Integer perdasFetais,
            Integer mesPreNatal,
            Instant criadoEm
    ) {
        public static RegistroResponse de(RegistroAnalise registro) {
            return new RegistroResponse(
                    registro.getId(),
                    registro.getUsuario().getId(),
                    registro.getFaixaEtaria(),
                    registro.getEstadoCivil(),
                    registro.getEscolaridade(),
                    registro.getTipoGravidez(),
                    registro.getGestacoesAnteriores(),
                    registro.getPartosNormais(),
                    registro.getPartosCesareos(),
                    registro.getFilhosVivos(),
                    registro.getPerdasFetais(),
                    registro.getMesPreNatal(),
                    registro.getCriadoEm()
            );
        }
    }
}
