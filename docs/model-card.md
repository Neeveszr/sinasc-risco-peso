# Model card — Risco de baixo peso ao nascer

Responsável: JP Lima.

| Campo | Conteúdo |
|-------|----------|
| **Versão do modelo** | TODO |
| **Data de treino** | TODO |
| **Finalidade** | Apoiar a priorização do acompanhamento pré-natal de gestantes com maior risco de baixo peso ao nascer (< 2.500 g). Não substitui o julgamento clínico |
| **Usuário** | Equipe de saúde da família da UBS e agentes comunitários de saúde |
| **Dados utilizados** | SINASC, município de São Paulo, ano 2024 (120.780 registros após o tratamento: 96.624 para treino e 24.156 para teste). Dez atributos mantidos, listados com justificativa em [`ml/selecao_atributos.md`](../ml/selecao_atributos.md) |
| **Colunas removidas (vazamento)** | 27 colunas removidas, com a justificativa de cada uma em [`ml/selecao_atributos.md`](../ml/selecao_atributos.md) |
| **Modelo e pré-processamento** | TODO |
| **Métrica principal e justificativa** | TODO |
| **Métricas no teste** | TODO |
| **Limiar de decisão e faixas de risco** | TODO |
| **Limitações conhecidas** | TODO |
| **Usos indevidos** | TODO (ex.: negar atendimento, decisão automática sem revisão humana, uso fora do município de São Paulo) |

## Contagem de registros: modelo × governança

O modelo e o relatório de governança ([`docs/governanca.md`](governanca.md)) partem do mesmo arquivo de 120.786 linhas e chegam ao mesmo total:

- **Excluídos:** 5 duplicatas exatas e 1 registro sem peso.
- **Resultado:** 120.780 registros, com 10,32% de baixo peso.
- **Divisão do modelo:** 96.624 para treino e 24.156 para teste, estratificada pela variável-alvo (`ml/modelagem.md` e `ml/artifacts/metadata.json`).
- **Mantidos:** os 13 pesos fora de 300 a 6.500 g, que a governança só sinaliza para revisão.
