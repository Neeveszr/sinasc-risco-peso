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

# Atributos categóricos selecionados
COLUNAS_CATEGORICAS = [
    "FxEtPartur",
    "ESTCIVMAE",
    "ESCMAE2010",
    "GRAVIDEZ"
]

# Atributos numéricos selecionados
COLUNAS_NUMERICAS = [
    "QTDGESTANT",
    "QTDPARTNOR",
    "QTDPARTCES",
    "QTDFILVIVO",
    "QTDFILMORT",
    "MESPRENAT"
]

# Lista completa dos atributos preditores
COLUNAS_PREDITORAS = (
    COLUNAS_CATEGORICAS + COLUNAS_NUMERICAS
)

# Nome da variável-alvo
COLUNA_ALVO = "baixo_peso"


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

def separar_atributos_alvo(df):
    """Separa os atributos preditores da variável-alvo."""

    # Verificar se todas as colunas necessárias existem
    colunas_necessarias = COLUNAS_PREDITORAS + [COLUNA_ALVO]

    colunas_faltantes = [
        coluna for coluna in colunas_necessarias
        if coluna not in df.columns
    ]

    if colunas_faltantes:
        raise ValueError(
            f"Colunas ausentes: {colunas_faltantes}"
        )

    # Selecionar somente os atributos permitidos
    X = df[COLUNAS_PREDITORAS].copy()

    # Selecionar a variável-alvo
    y = df[COLUNA_ALVO].copy()

    return X, y

def verificar_vazamento(X):
    """Verifica se existem atributos não permitidos."""

    colunas_proibidas = [
        coluna for coluna in X.columns
        if coluna not in COLUNAS_PREDITORAS
    ]

    if colunas_proibidas:
        raise ValueError(
            f"Possível vazamento de dados: {colunas_proibidas}"
        )

    print("Verificação de vazamento: OK")


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

    # Separar atributos preditores e variável-alvo
    X, y = separar_atributos_alvo(dados)

    print("\nDimensões de X:", X.shape)
    print("Dimensões de y:", y.shape)

    print("\nAtributos selecionados:")
    for coluna in X.columns:
        print("-", coluna)

    # Teste positivo: atributos permitidos
    print("\nTeste positivo:")
    verificar_vazamento(X)

    # Teste negativo: inclusão proposital de PESO
    print("\nTeste negativo:")

    X_invalido = X.copy()
    X_invalido["PESO"] = dados["PESO"]

    try:
        verificar_vazamento(X_invalido)
    except ValueError as erro:
        print("Vazamento detectado corretamente:", erro)
    else:
        raise AssertionError("Falha na detecção de vazamento.")
