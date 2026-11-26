"""Serviço de predição (FastAPI). Contrato: ml/contrato_predict.md.

MODELO_MOCK=1 sobe o serviço provisório (0.0.0-mock); sem a variável, carrega
o modelo real de ml/artifacts.
"""
import os

from fastapi import FastAPI

from mock import VERSAO_MOCK, prever_mock
from schemas import EntradaPredicao, RespostaPredicao

USA_MOCK = os.getenv("MODELO_MOCK", "0") == "1"

app = FastAPI(title="Serviço de predição SINASC", version="0.1.0")

preditor = None
if not USA_MOCK:
    from preditor import Preditor  # import tardio: o mock não precisa de shap

    preditor = Preditor()  # falha ao subir se o modelo não carregar


@app.get("/health")
def health():
    return {"status": "ok"}


@app.get("/model/version")
def versao_modelo():
    return {"versao_modelo": VERSAO_MOCK if USA_MOCK else preditor.versao}


@app.post("/predict", response_model=RespostaPredicao)
def predict(entrada: EntradaPredicao):
    dados = entrada.model_dump()
    return prever_mock(dados) if USA_MOCK else preditor.prever(dados)
