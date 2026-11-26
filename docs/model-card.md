# Model card — Risco de baixo peso ao nascer

Responsável: JP Lima.

| Campo | Conteúdo |
|-------|----------|
| **Versão do modelo** | TODO |
| **Data de treino** | TODO |
| **Finalidade** | Apoiar a priorização do acompanhamento pré-natal de gestantes com maior risco de baixo peso ao nascer (< 2.500 g). Não substitui o julgamento clínico |
| **Usuário** | Equipe de saúde da família da UBS e agentes comunitários de saúde |
| **Dados utilizados** | SINASC, município de São Paulo, ano 2024 (120.785 registros com peso válido). Dez atributos mantidos, listados com justificativa em [`ml/selecao_atributos.md`](../ml/selecao_atributos.md) |
| **Colunas removidas (vazamento)** | 27 colunas removidas, com a justificativa de cada uma em [`ml/selecao_atributos.md`](../ml/selecao_atributos.md) |
| **Modelo e pré-processamento** | TODO |
| **Métrica principal e justificativa** | TODO |
| **Métricas no teste** | TODO |
| **Limiar de decisão e faixas de risco** | TODO |
| **Limitações conhecidas** | TODO |
| **Usos indevidos** | TODO (ex.: negar atendimento, decisão automática sem revisão humana, uso fora do município de São Paulo) |

## Contagem de registros: modelo × governança

O total de registros do treino pode não bater com o do relatório de governança ([`docs/governanca.md`](governanca.md)), porque cada etapa aplica uma regra diferente sobre o mesmo arquivo de 120.786 linhas:

| Etapa | O que exclui | O que mantém |
|-------|--------------|--------------|
| Governança (`governanca/governanca_sinasc.py`) | 5 duplicatas exatas e 1 registro sem peso (sobram 120.780) | Pesos fora de 300 a 6.500 g, só sinalizados como suspeitos |
| Modelo (`ml/src/data_preparation.py`) | Registros sem peso, com peso 0 ou com o código 9999 | As duplicatas exatas |

Os números da governança descrevem a qualidade da base; o total usado no treino é o que sai do pipeline do modelo e deve ser registrado aqui junto com as métricas.
