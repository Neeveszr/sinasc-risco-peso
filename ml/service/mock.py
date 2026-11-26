"""Respostas INVENTADAS, só para o back-end integrar (versão 0.0.0-mock).
Não representam o modelo e não podem ir para o deploy final."""

VERSAO_MOCK = "0.0.0-mock"


def prever_mock(dados: dict) -> dict:
    # Vale a primeira regra que se aplicar.
    if dados["MESPRENAT"] is None:
        score, faixa = 0.62, "ALTO"
        fator = {"atributo": "MESPRENAT", "valor": None,
                 "impacto": 0.12, "direcao": "AUMENTA"}
    elif dados["GRAVIDEZ"] in (2, 3):
        score, faixa = 0.47, "MEDIO"
        fator = {"atributo": "GRAVIDEZ", "valor": dados["GRAVIDEZ"],
                 "impacto": 0.08, "direcao": "AUMENTA"}
    else:
        score, faixa = 0.30, "BAIXO"
        fator = {"atributo": "FxEtPartur", "valor": dados["FxEtPartur"],
                 "impacto": 0.02, "direcao": "REDUZ"}
    return {"score": score, "faixa_risco": faixa,
            "fatores": [fator], "versao_modelo": VERSAO_MOCK}
