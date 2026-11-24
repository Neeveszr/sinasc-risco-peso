# Esse arquivo contém funções reutilizáveis que serão utilizadas posteriormente no treinamento.
from pathlib import Path

import pandas as pd


# Caminho da base de dados
RAIZ_PROJETO = Path(__file__).resolve().parents[2]

CAMINHO_DADOS = (
    RAIZ_PROJETO / "data" / "raw" / "DNRE2024.csv"
)

# Definição do limite de baixo peso
LIMITE_BAIXO_PESO = 2500


def carregar_dados():
    """Carrega a base original do SINASC."""

    df = pd.read_csv(
        CAMINHO_DADOS,
        sep=",",
        encoding="utf-8",
        low_memory=False
    )

    return df


def criar_variavel_alvo(df):
    """Trata a coluna PESO e cria a variável-alvo."""

    df = df.copy()

    # Converter a coluna PESO para formato numérico
    df["PESO"] = pd.to_numeric(
        df["PESO"],
        errors="coerce"
    )

    # Remover registros sem peso ou com peso inválido
    df = df[
        df["PESO"].notna()
        & (df["PESO"] > 0)
        & (df["PESO"] != 9999)
    ].copy()

    # Criar variável-alvo binária
    df["baixo_peso"] = (
        df["PESO"] < LIMITE_BAIXO_PESO
    ).astype(int)

    return df


if __name__ == "__main__":

    dados = carregar_dados()

    print("Registros originais:", len(dados))

    dados = criar_variavel_alvo(dados)

    print("Registros considerados:", len(dados))

    print("\nDistribuição da variável-alvo:")
    print(dados["baixo_peso"].value_counts().sort_index())

    print("\nDistribuição percentual:")
    print(
        dados["baixo_peso"]
        .value_counts(normalize=True)
        .sort_index()
        .mul(100)
        .round(2)
    )
