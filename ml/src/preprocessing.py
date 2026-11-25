
import pandas as pd
import numpy as np

from sklearn.compose import ColumnTransformer
from sklearn.impute import SimpleImputer
from sklearn.preprocessing import (
    OneHotEncoder,
    StandardScaler,
    FunctionTransformer
)
from sklearn.pipeline import Pipeline

from data_preparation import (
    COLUNAS_NUMERICAS,
    COLUNAS_CATEGORICAS
)


def limpar_atributos(X):
    """Padroniza categorias e trata valores inválidos."""

    X = X.copy()

    # Padronizar as faixas etárias
    X["FxEtPartur"] = (
        X["FxEtPartur"]
        .astype("string")
        .str.strip()
        .str.replace("–", "-", regex=False)
        .replace("Ign", pd.NA)
        .astype(object)
    )

    # Padronizar a representação dos valores ausentes
    X["FxEtPartur"] = X["FxEtPartur"].where(
        X["FxEtPartur"].notna(),
        np.nan
    )

    # Definir os códigos categóricos válidos
    categorias_validas = {
        "ESTCIVMAE": [1, 2, 3, 4, 5],
        "ESCMAE2010": [0, 1, 2, 3, 4, 5],
        "GRAVIDEZ": [1, 2, 3]
    }

    # Tratar códigos categóricos inválidos
    for coluna, valores in categorias_validas.items():

        X[coluna] = pd.to_numeric(
            X[coluna],
            errors="coerce"
        )

        X[coluna] = X[coluna].where(
            X[coluna].isin(valores),
            np.nan
        )

        # Padronizar códigos categóricos como inteiros
        X[coluna] = X[coluna].astype("Int64")

    # Tratar atributos numéricos
    for coluna in COLUNAS_NUMERICAS:

        X[coluna] = pd.to_numeric(
            X[coluna],
            errors="coerce"
        )

        X[coluna] = X[coluna].where(
            (X[coluna] >= 0)
            & np.isfinite(X[coluna])
            & (X[coluna] % 1 == 0),
            np.nan
        )

    # Validar o mês de início do pré-natal
    X["MESPRENAT"] = X["MESPRENAT"].where(
        X["MESPRENAT"].between(1, 9),
        np.nan
    )

    # Criar indicador de ausência de informação
    X["MESPRENAT_AUSENTE"] = (
        X["MESPRENAT"].isna().astype(int)
    )
    # Padronizar os atributos categóricos como texto
    for coluna in COLUNAS_CATEGORICAS:

        X[coluna] = (
            X[coluna]
            .astype("string")
            .astype(object)
        )

        X[coluna] = X[coluna].where(
            X[coluna].notna(),
            np.nan
        )

    return X


def criar_preprocessador():
    """Cria o ColumnTransformer para os atributos do modelo."""

    # Tratamento dos atributos numéricos
    pipeline_numerico = Pipeline(steps=[
        ("imputacao", SimpleImputer(strategy="median")),
        ("padronizacao", StandardScaler())
    ])

    # Tratamento dos atributos categóricos
    pipeline_categorico = Pipeline(steps=[
        ("imputacao", SimpleImputer(
            strategy="constant",
            fill_value="Ignorado"
        )),
        ("codificacao", OneHotEncoder(
            handle_unknown="ignore"
        ))
    ])

    # Combinar os tratamentos
    preprocessador = ColumnTransformer(
        transformers=[
            (
                "numerico",
                pipeline_numerico,
                COLUNAS_NUMERICAS
            ),
            (
                "categorico",
                pipeline_categorico,
                COLUNAS_CATEGORICAS
            ),
            (
                "indicador",
                "passthrough",
                ["MESPRENAT_AUSENTE"]
            )
        ],
        remainder="drop"
    )

    return preprocessador

def criar_pipeline(modelo):
    """Cria um pipeline completo de preparação e classificação."""

    pipeline = Pipeline(steps=[
        ("limpeza", FunctionTransformer(
            limpar_atributos,
            validate=False
        )),

        ("preprocessamento", criar_preprocessador()),

        ("modelo", modelo)
    ])

    return pipeline




