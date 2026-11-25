# Contrato da API de Predição — SINASC

**Responsáveis:** João Pedro Lima (avaliação, explicabilidade e serviço)  
**Status:** Contrato em definição  
**Fonte de referência:** Esta documentação, até a implementação do serviço.

## 1. Objetivo

Definir o padrão de comunicação entre o serviço de Inteligência Artificial e o back-end.

O modelo estima a probabilidade de um recém-nascido apresentar baixo peso ao nascer, definido como peso inferior a 2.500 g.

## 2. Endpoints

### POST `/predict`

Recebe os atributos do pré-natal e retorna a probabilidade estimada, a faixa de risco e os fatores que contribuíram para a previsão.

### GET `/health`

Verifica se o serviço está disponível.

### GET `/model/version`

Retorna a versão e os metadados do modelo utilizado.

## 3. Atributos de entrada

O serviço recebe os seguintes atributos:

| Atributo | Descrição |
|---|---|
| `FxEtPartur` | Faixa etária da parturiente |
| `ESTCIVMAE` | Estado civil da mãe |
| `ESCMAE2010` | Escolaridade da mãe |
| `GRAVIDEZ` | Tipo de gravidez |
| `QTDGESTANT` | Quantidade de gestações anteriores |
| `QTDPARTNOR` | Quantidade de partos normais |
| `QTDPARTCES` | Quantidade de partos cesáreos |
| `QTDFILVIVO` | Quantidade de filhos vivos |
| `QTDFILMORT` | Quantidade de filhos mortos |
| `MESPRENAT` | Mês de início do pré-natal |

Os atributos devem ser enviados com seus nomes originais, conforme o conjunto de dados utilizado no treinamento.

## 4. Regras de validação

- Os atributos devem respeitar os tipos e domínios definidos no esquema de entrada.
- Valores inválidos devem gerar resposta HTTP 422.
- Atributos adicionais não previstos no contrato devem ser rejeitados.
- Valores ausentes devem ser enviados como `null`, quando permitido.
- `MESPRENAT = null` representa ausência de informação sobre o início do pré-natal e deve ser tratado pelo pipeline.
- Os limites dos atributos quantitativos devem ser confirmados com a equipe de dados antes da implementação definitiva.

## 5. Resposta da predição

A resposta deverá conter:

- Probabilidade estimada de baixo peso.
- Faixa de risco: `BAIXO`, `MEDIO` ou `ALTO`.
- Principais fatores explicativos, utilizando SHAP.
- Versão do modelo utilizado.

Os nomes definitivos dos campos devem ser mantidos em snake_case para facilitar a integração entre IA e back-end.

## 6. Explicabilidade

Os fatores explicativos devem indicar:

- Atributo analisado.
- Impacto na previsão.
- Direção do impacto, indicando se contribuiu para aumentar ou reduzir o risco.

A explicação deve ser apresentada de forma compreensível para o usuário, sem afirmar causalidade.

## 7. Limiar e faixas de risco

Os limites das faixas de risco ainda dependem da avaliação do modelo.

A definição deverá considerar:

- Métricas de desempenho.
- Recall da classe de baixo peso.
- Custo dos falsos negativos.
- Necessidade de equilíbrio entre sensibilidade e precisão.

Os valores finais serão documentados após a avaliação.

## 8. Versionamento

Toda resposta de predição deverá identificar a versão do modelo utilizado.

A versão deverá estar associada aos metadados do artefato treinado.

## 9. Status da implementação

Este documento define o contrato esperado.

A implementação do serviço FastAPI, os esquemas de validação e os exemplos definitivos de entrada e saída serão adicionados posteriormente, após a integração com o modelo treinado e o alinhamento com o back-end.
