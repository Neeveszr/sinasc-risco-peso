"""Carrega o modelo do Marcus e devolve score, faixa e fatores (SHAP)."""
import json
import sys
from pathlib import Path

RAIZ_ML = Path(__file__).resolve().parents[1]
# ml/src precisa estar no sys.path ANTES do joblib.load: o pipeline serializado
# referencia a função limpar_atributos de preprocessing.py.
sys.path.insert(0, str(RAIZ_ML / "src"))

import joblib  # noqa: E402
import numpy as np  # noqa: E402
import pandas as pd  # noqa: E402
import shap  # noqa: E402

PASTA_ARTIFACTS = RAIZ_ML / "artifacts"
ATRIBUTOS = [
    "FxEtPartur", "ESTCIVMAE", "ESCMAE2010", "GRAVIDEZ", "QTDGESTANT",
    "QTDPARTNOR", "QTDPARTCES", "QTDFILVIVO", "QTDFILMORT", "MESPRENAT",
]
QTD_FATORES = 3


def atributo_original(coluna: str) -> str:
    """'categorico__FxEtPartur_25-29' -> 'FxEtPartur'.
    O indicador 'MESPRENAT_AUSENTE' conta como MESPRENAT."""
    nome = coluna.split("__", 1)[1]
    for atributo in ATRIBUTOS:
        if nome == atributo or nome.startswith(atributo + "_"):
            return atributo
    raise ValueError(f"Coluna sem atributo original: {coluna}")


class Preditor:
    def __init__(self):
        with open(PASTA_ARTIFACTS / "metadata.json", encoding="utf-8") as f:
            self.metadados = json.load(f)
        self.versao = self.metadados["versao"]
        self.faixas = self.metadados["classificacao"]["faixas"]

        self.pipeline = joblib.load(PASTA_ARTIFACTS / "modelo_sinasc.joblib")
        self.limpeza = self.pipeline.named_steps["limpeza"]
        self.preprocessador = self.pipeline.named_steps["preprocessamento"]
        self.explainer = shap.TreeExplainer(self.pipeline.named_steps["modelo"])

        colunas = self.preprocessador.get_feature_names_out()
        self.grupos = [atributo_original(c) for c in colunas]

    def classificar_faixa(self, score: float) -> str:
        if score < self.faixas["MEDIO"]["min"]:
            return "BAIXO"
        if score < self.faixas["ALTO"]["min"]:
            return "MEDIO"
        return "ALTO"

    def _shap_classe_positiva(self, x_transformado) -> np.ndarray:
        valores = self.explainer.shap_values(x_transformado)
        if isinstance(valores, list):          # versões antigas do shap
            return np.asarray(valores[1])[0]
        valores = np.asarray(valores)
        return valores[0, :, 1] if valores.ndim == 3 else valores[0]

    def prever(self, dados: dict) -> dict:
        entrada = pd.DataFrame([dados])
        score = float(self.pipeline.predict_proba(entrada)[0, 1])

        x = self.preprocessador.transform(self.limpeza.transform(entrada))
        por_coluna = self._shap_classe_positiva(x)

        soma = dict.fromkeys(ATRIBUTOS, 0.0)       # soma por atributo original
        for atributo, valor in zip(self.grupos, por_coluna):
            soma[atributo] += float(valor)

        principais = sorted(soma.items(), key=lambda p: abs(p[1]), reverse=True)
        fatores = [
            {
                "atributo": atributo,
                "valor": dados[atributo],
                "impacto": round(abs(contrib), 4),
                "direcao": "AUMENTA" if contrib >= 0 else "REDUZ",
            }
            for atributo, contrib in principais[:QTD_FATORES]
        ]
        return {
            "score": round(score, 6),
            "faixa_risco": self.classificar_faixa(score),
            "fatores": fatores,
            "versao_modelo": self.versao,
        }
