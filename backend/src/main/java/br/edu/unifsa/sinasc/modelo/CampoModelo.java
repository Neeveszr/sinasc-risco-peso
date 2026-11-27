package br.edu.unifsa.sinasc.modelo;

import java.util.Arrays;
import java.util.Optional;

/**
 * Os 10 atributos do contrato do serviço de IA (ml/contrato_predict.md): nome que o front usa
 * (camelCase), nome original do SINASC (o que o serviço entende) e um rótulo em português.
 * É a única tabela de conversão: o cliente e a tradução dos erros e dos fatores usam esta mesma lista.
 */
public enum CampoModelo {

    FAIXA_ETARIA("faixaEtaria", "FxEtPartur", "Faixa etária"),
    ESCOLARIDADE("escolaridade", "ESCMAE2010", "Escolaridade"),
    ESTADO_CIVIL("estadoCivil", "ESTCIVMAE", "Situação conjugal"),
    TIPO_GRAVIDEZ("tipoGravidez", "GRAVIDEZ", "Tipo de gravidez"),
    MES_INICIO_PRENATAL("mesInicioPrenatal", "MESPRENAT", "Início do pré-natal"),
    GESTACOES_ANTERIORES("gestacoesAnteriores", "QTDGESTANT", "Gestações anteriores"),
    PARTOS_NORMAIS("partosNormais", "QTDPARTNOR", "Partos normais anteriores"),
    PARTOS_CESAREOS("partosCesareos", "QTDPARTCES", "Partos cesáreos anteriores"),
    FILHOS_VIVOS("filhosVivos", "QTDFILVIVO", "Filhos vivos"),
    PERDAS_FETAIS("perdasFetais", "QTDFILMORT", "Perdas fetais");

    private final String nomeFront;
    private final String nomeServico;
    private final String rotulo;

    CampoModelo(String nomeFront, String nomeServico, String rotulo) {
        this.nomeFront = nomeFront;
        this.nomeServico = nomeServico;
        this.rotulo = rotulo;
    }

    public String nomeFront() {
        return nomeFront;
    }

    public String nomeServico() {
        return nomeServico;
    }

    public String rotulo() {
        return rotulo;
    }

    public static Optional<CampoModelo> porNomeServico(String nome) {
        return Arrays.stream(values()).filter(c -> c.nomeServico.equals(nome)).findFirst();
    }
}
