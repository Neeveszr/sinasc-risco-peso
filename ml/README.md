# ml — Dados, modelo e serviço de predição

Responsáveis: Marcus Mikael (dados e modelagem), JP Lima (avaliação, explicabilidade e serviço).

| Pasta | Conteúdo |
|-------|----------|
| `notebooks/` | Análise exploratória, tratamento e modelagem, executáveis do início ao fim |
| `src/` | Scripts reutilizáveis: obtenção e tratamento dos dados, regras de qualidade, treino |
| `service/` | Serviço FastAPI que carrega o modelo e expõe `POST /predict` |
| `artifacts/` | Modelo serializado (`.joblib`), metadados (versão, data, métricas, atributos, limiar) e esquema de entrada |

## Contrato do serviço (a fechar até 27/11)

`POST /predict` recebe os atributos do pré-natal no formato original e devolve:

- probabilidade de baixo peso;
- faixa de risco (baixo, médio, alto);
- principais fatores da explicação (SHAP);
- versão do modelo.

TODO: JSON de exemplo de entrada e saída (o back-end usa o mesmo contrato).

## Como executar

TODO: instalar `requirements.txt`, rodar o tratamento, treinar e subir o serviço.
