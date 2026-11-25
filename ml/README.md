# ml — Dados, modelo e serviço de predição

Responsáveis: Marcus Mikael (dados e modelagem), João Pedro Lima (avaliação, explicabilidade e serviço).

| Pasta | Conteúdo |
|-------|----------|
| `notebooks/` | Análise exploratória, tratamento e modelagem, executáveis do início ao fim |
| `src/` | Scripts reutilizáveis: obtenção e tratamento dos dados, regras de qualidade, treino |
| `service/` | Serviço FastAPI que carrega o modelo e expõe `POST /predict` |
| `artifacts/` | Modelo serializado (`.joblib`), metadados (versão, data, métricas, atributos, limiar) e esquema de entrada |

## Contrato do serviço

Consulte o [contrato da API de predição](contrato_predict.md).

## Como executar

As instruções de instalação das dependências, execução do tratamento dos dados, treinamento do modelo e inicialização do serviço serão adicionadas quando o fluxo de execução estiver definido.