# Dados

Os **dados brutos não são versionados** (exigência do trabalho). Esta pasta guarda só as instruções para obtê-los.

## Fonte

SINASC — Dados abertos da Prefeitura de São Paulo (Secretaria Municipal da Saúde). Existe um arquivo por ano (2015–2025), em CSV, XLSX ou DBF.

## Como obter

1. Baixar o **CSV de um ano fechado** (o ano corrente é preliminar) e o **dicionário de dados do SINASC**, ambos na mesma página.
2. Salvar o CSV em `data/raw/` (pasta ignorada pelo Git).
3. Ao abrir o CSV no Excel, importar como UTF-8 para não quebrar os acentos.

Os scripts que leem e tratam os dados ficam em [`ml/src/`](../ml/src/).

## Ano utilizado

TODO
