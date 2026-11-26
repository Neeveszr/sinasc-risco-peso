"""Esquemas de entrada e saída do serviço de predição.

Seguem o contrato em ml/contrato_predict.md. As regras de qualidade de caso
individual ficam aqui, para valerem também na API (422 em caso de violação).
"""
from typing import Annotated, Literal, Optional

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

FaixaEtaria = Literal[
    "10-14", "15-19", "20-24", "25-29", "30-34",
    "35-39", "40-44", "45-49", ">=50",
]
Contagem = Annotated[int, Field(ge=0, le=15)]


class EntradaPredicao(BaseModel):
    # extra="forbid": campos como PESO são rejeitados.
    # strict=True: "1" (texto) e true não são aceitos no lugar de 1.
    model_config = ConfigDict(extra="forbid", strict=True)

    # Os 10 campos devem estar presentes; alguns aceitam null (= ignorado).
    FxEtPartur: Optional[FaixaEtaria]
    ESTCIVMAE: Optional[Literal[1, 2, 3, 4, 5, 9]]
    ESCMAE2010: Optional[Literal[0, 1, 2, 3, 4, 5, 9]]
    GRAVIDEZ: Optional[Literal[1, 2, 3, 9]]
    QTDGESTANT: Contagem
    QTDPARTNOR: Contagem
    QTDPARTCES: Contagem
    QTDFILVIVO: Contagem
    QTDFILMORT: Contagem
    # null = pré-natal não iniciado ou não informado. O 9 é o nono mês.
    MESPRENAT: Optional[Annotated[int, Field(ge=1, le=9)]]

    @field_validator(
        "ESTCIVMAE", "ESCMAE2010", "GRAVIDEZ", "QTDGESTANT", "QTDPARTNOR",
        "QTDPARTCES", "QTDFILVIVO", "QTDFILMORT", "MESPRENAT", mode="before",
    )
    @classmethod
    def nao_aceita_booleano(cls, valor):
        # em Python, True == 1; sem isso, true seria aceito como 1
        if isinstance(valor, bool):
            raise ValueError("Informe um número inteiro, não um booleano")
        return valor

    @field_validator("ESTCIVMAE", "ESCMAE2010", "GRAVIDEZ")
    @classmethod
    def codigo_9_vira_ignorado(cls, valor):
        return None if valor == 9 else valor

    @model_validator(mode="after")
    def partos_nao_excedem_gestacoes(self):
        if self.QTDPARTNOR + self.QTDPARTCES > self.QTDGESTANT:
            raise ValueError(
                "QTDPARTNOR + QTDPARTCES não pode ser maior que QTDGESTANT"
            )
        return self


class Fator(BaseModel):
    atributo: str
    valor: Optional[str | int]
    impacto: float
    direcao: Literal["AUMENTA", "REDUZ"]


class RespostaPredicao(BaseModel):
    score: float
    faixa_risco: Literal["BAIXO", "MEDIO", "ALTO"]
    fatores: list[Fator]
    versao_modelo: str
