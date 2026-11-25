"""Atributos do formulário de predição.

Ponto único com os 10 atributos escolhidos em ml/selecao_atributos.md:
o nome do campo na API, a coluna original do SINASC, o rótulo para o usuário,
as opções e os limites. Se o contrato mudar, só este arquivo muda.

Os códigos seguem o dicionário de dados do SINASC. "Não informado" é enviado
como null, e o modelo trata como ausente (o mesmo que o código 9 da base).
"""

FAIXAS_ETARIAS = ["10-14", "15-19", "20-24", "25-29", "30-34", "35-39", "40-44", "45-49", ">=50"]

ESTADO_CIVIL = {
    1: "Solteira",
    2: "Casada",
    3: "Viúva",
    4: "Separada judicialmente ou divorciada",
    5: "União estável",
}

ESCOLARIDADE = {
    0: "Sem escolaridade",
    1: "Fundamental I (1ª a 4ª série)",
    2: "Fundamental II (5ª a 8ª série)",
    3: "Ensino médio",
    4: "Superior incompleto",
    5: "Superior completo",
}

TIPO_GRAVIDEZ = {
    1: "Única",
    2: "Dupla (gêmeos)",
    3: "Tripla ou mais",
}

# Limite das contagens do histórico obstétrico (regra de qualidade provisória:
# acima disso a base tem só casos isolados, tratados como erro de digitação).
MAX_CONTAGEM = 15

CATEGORICOS = [
    {"api": "faixaEtaria", "sinasc": "FxEtPartur", "rotulo": "Faixa etária da gestante (anos)",
     "opcoes": {f: "50 ou mais" if f == ">=50" else f.replace("-", " a ") for f in FAIXAS_ETARIAS}},
    {"api": "estadoCivil", "sinasc": "ESTCIVMAE", "rotulo": "Situação conjugal", "opcoes": ESTADO_CIVIL},
    {"api": "escolaridade", "sinasc": "ESCMAE2010", "rotulo": "Escolaridade da gestante", "opcoes": ESCOLARIDADE},
    {"api": "tipoGravidez", "sinasc": "GRAVIDEZ", "rotulo": "Tipo de gravidez", "opcoes": TIPO_GRAVIDEZ},
]

CONTAGENS = [
    {"api": "gestacoesAnteriores", "sinasc": "QTDGESTANT", "rotulo": "Gestações anteriores"},
    {"api": "partosNormais", "sinasc": "QTDPARTNOR", "rotulo": "Partos normais anteriores"},
    {"api": "partosCesareos", "sinasc": "QTDPARTCES", "rotulo": "Cesáreas anteriores"},
    {"api": "filhosVivos", "sinasc": "QTDFILVIVO", "rotulo": "Filhos vivos"},
    {"api": "perdasFetais", "sinasc": "QTDFILMORT", "rotulo": "Perdas fetais ou abortos anteriores"},
]

MES_PRENATAL = {"api": "mesInicioPrenatal", "sinasc": "MESPRENAT",
                "rotulo": "Mês da gestação em que o pré-natal começou"}

# Rótulos de todos os campos, para mensagens de erro e explicações.
ROTULOS = {c["api"]: c["rotulo"] for c in CATEGORICOS + CONTAGENS + [MES_PRENATAL]}
ROTULOS.update({c["sinasc"]: c["rotulo"] for c in CATEGORICOS + CONTAGENS + [MES_PRENATAL]})


def validar(dados):
    """Valida as regras simples no front. Devolve a lista de mensagens de erro.

    A validação que vale é a do back-end; esta só evita ida e volta desnecessária.
    """
    erros = []
    for campo in CONTAGENS:
        valor = dados.get(campo["api"])
        if valor is None or not 0 <= valor <= MAX_CONTAGEM:
            erros.append(f"{campo['rotulo']}: informe um número de 0 a {MAX_CONTAGEM}.")

    mes = dados.get(MES_PRENATAL["api"])
    if mes is not None and not 1 <= mes <= 9:
        erros.append(f"{MES_PRENATAL['rotulo']}: informe um mês de 1 a 9.")

    partos = (dados.get("partosNormais") or 0) + (dados.get("partosCesareos") or 0)
    if partos > (dados.get("gestacoesAnteriores") or 0):
        erros.append("A soma de partos normais e cesáreas não pode ser maior que o número de gestações anteriores.")

    return erros
