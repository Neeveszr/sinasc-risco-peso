# sinasc-risco-peso

Sistema de apoio à decisão para UBS: alerta o risco de **baixo peso ao nascer** com base nos dados do SINASC (município de São Paulo), com IA explicável, governança de dados conforme a LGPD e API em Spring Boot.

> Trabalho Final Integrado — Inteligência Artificial · Governança de Dados · Desenvolvimento Back-end (UNIFSA, Engenharia de Software).
> **Contexto 4, Variante A.**

> ⚠️ Este README é um esqueleto. Cada seção marcada com `TODO` deve ser preenchida pela frente responsável até 30/11.

## 1. Problema e usuário

- **Pergunta do modelo:** o bebê nascerá com baixo peso (menos de 2.500 g)?
- **Unidade de análise:** nascido vivo.
- **Usuário:** equipe de saúde da família da UBS e agentes comunitários de saúde.
- **Uso:** priorizar o acompanhamento pré-natal das gestantes com maior risco.
- **Erro mais custoso:** TODO (falso negativo ou falso positivo, e por quê).

## 2. Dados e fonte

- **Base:** SINASC — Sistema de Informações sobre Nascidos Vivos, dados abertos da Prefeitura de São Paulo (Secretaria Municipal da Saúde).
- **Ano utilizado:** 2024 (arquivo `DNRE2024.csv`, 120.786 nascidos vivos).
- **Como obter:** ver [`data/README.md`](data/README.md). Os dados brutos **não são versionados**.

## 3. Arquitetura

```
SINASC (CSV) → tratamento e governança → treino (scikit-learn Pipeline)
            → serviço Python (FastAPI) ←→ API Spring Boot ←→ Front-end web (Streamlit)
                                              └── banco relacional
```

| Pasta | Conteúdo |
|-------|----------|
| [`ml/`](ml/) | Tratamento dos dados, treino, avaliação e serviço do modelo (Python) |
| [`backend/`](backend/) | API REST em Java + Spring Boot |
| [`frontend/`](frontend/) | Aplicação web em Streamlit (Python), que consome a API |
| [`docs/`](docs/) | Model card e documento de governança de dados |
| [`data/`](data/) | Instruções para obter os dados (conteúdo bruto fora do Git) |

## 4. Resultados do modelo

TODO: métricas no conjunto de teste, comparação com o baseline, limiar e faixas de risco. Detalhes no [model card](docs/model-card.md).

## 5. Limitações

TODO

## 6. Conformidade com a LGPD

TODO: resumo. Detalhes no [documento de governança](docs/governanca.md).

## 7. Como executar

TODO: pré-requisitos, variáveis de ambiente, banco de dados, autenticação e comandos para subir cada parte (ou `docker compose up`).

## 8. Equipe

| Integrante | Frente |
|------------|--------|
| Marcus Mikael | Inteligência Artificial — dados e modelagem |
| JP Lima | Inteligência Artificial — avaliação e explicabilidade |
| Marcos André | Governança de Dados |
| Rodolfo Dheymison | Back-end |
| Carlos Arthur | Back-end |
| Antonio | Gestão, front-end, deploy e repositório |

## 9. Declaração de uso de IA generativa

Conforme a Resolução nº 005/2026 (CONSUP) da UNIFSA, declaramos as ferramentas de IA generativa utilizadas e as tarefas em que foram usadas:

| Ferramenta | Tarefa | Quem usou |
|------------|--------|-----------|
| Claude Code (Anthropic) | Geração do código do front-end (Streamlit), dos testes e do Dockerfile, com revisão e testes feitos pelo integrante | Antonio |
| | | |

O grupo é capaz de explicar e defender todo o conteúdo entregue.

## 10. Referências e fontes

- SINASC — Prefeitura de São Paulo, dados abertos. TODO: link e data de acesso.
- TODO: bibliotecas e artigos de terceiros utilizados.
