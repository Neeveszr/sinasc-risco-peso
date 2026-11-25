
# Seleção de Atributos e Prevenção de Vazamento de Dados

## 1. Identificação

- Projeto: SINASC — Risco de baixo peso ao nascer
- Base: DNRE2024

## 2. Definição do problema

O projeto consiste em um problema de classificação binária,
cujo objetivo é estimar o risco de baixo peso ao nascer
utilizando informações maternas e gestacionais.

A variável-alvo é denominada baixo_peso:

- Classe 0: peso igual ou superior a 2500 gramas.
- Classe 1: peso inferior a 2500 gramas.

A predição proposta ocorre durante o acompanhamento
pré-natal, após o seu início e quando o tipo de gravidez
já estiver conhecido, antes do nascimento.

## 3. Atributos selecionados

| Atributo | Tipo | Justificativa |
|---|---|---|
| FxEtPartur | Categórico | Representa a faixa etária materna. |
| ESTCIVMAE | Categórico | Representa o contexto sociofamiliar. |
| ESCMAE2010 | Categórico | Representa o contexto socioeducacional. |
| GRAVIDEZ | Categórico | Identifica o tipo de gravidez. |
| QTDGESTANT | Numérico | Representa o histórico de gestações anteriores. |
| QTDPARTNOR | Numérico | Representa o histórico de partos vaginais. |
| QTDPARTCES | Numérico | Representa o histórico de partos cesáreos. |
| QTDFILVIVO | Numérico | Representa o histórico de filhos vivos. |
| QTDFILMORT | Numérico | Representa o histórico de perdas fetais. |
| MESPRENAT | Numérico | Identifica o mês de início do acompanhamento pré-natal. |

A seleção deverá ser validada com a frente de Governança
de Dados, considerando finalidade, necessidade e
disponibilidade das informações.

## 4. Atributos excluídos

### 4.1. Vazamento temporal ou indisponibilidade

| Atributo | Justificativa |
|---|---|
| LOCNASC | Local efetivo do nascimento. |
| CODMUNNASC | Município efetivo do nascimento. |
| SEMAGESTAC | Duração gestacional registrada no nascimento. |
| TPMETESTIM | Método de estimativa registrado na declaração de nascimento. |
| CONSPRENAT | Total retrospectivo de consultas até o nascimento. |
| TPAPRESENT | Apresentação do bebê registrada no nascimento. |
| STTRABPART | Informação sobre indução do trabalho de parto. |
| PARTO | Tipo de parto efetivamente realizado. |
| STCESPARTO | Informação sobre a realização da cesárea. |
| TPNASCASSI | Profissional que assistiu ao nascimento. |
| DTNASC2 | Data efetiva do nascimento. |
| APGAR1 | Avaliação realizada após o nascimento. |
| APGAR5 | Avaliação realizada após o nascimento. |
| IDANOMAL | Pode conter informações identificadas após o nascimento. |
| TPROBSON | Classificação que incorpora informações obstétricas do parto. |

### 4.2. Minimização, redundância e finalidade

| Atributo | Justificativa |
|---|---|
| RACACORMAE | Dado sensível excluído inicialmente por minimização. Sua exclusão não garante ausência de vieses. |
| CODUFNATU | Informação de naturalidade não essencial à proposta. |
| ESCMAE | Redundante com ESCMAE2010. |
| SERIESCMAE | Redundância e elevado percentual de ausência. |
| CODOCUPMAE | Elevada cardinalidade e necessidade de minimização. |
| CODMUNRES | Informação geográfica não essencial à proposta inicial. |
| CODPAISRES | Informação geográfica não essencial. |
| SEXO | Disponibilidade não garantida no momento da predição. |
| FaixaEtPai | Apresenta 74,36% de valores ausentes. |
| RETROALIM | Informação administrativa sem finalidade preditiva definida. |
| STDNNOVA | Informação administrativa sem finalidade preditiva definida. |

### 4.3. Origem da variável-alvo

| Atributo | Justificativa |
|---|---|
| PESO | Utilizado exclusivamente para criar a variável-alvo. Sua inclusão como preditor causaria vazamento direto do resultado. |

## 5. Estratégia de prevenção de vazamento

Foi implementada uma lista explícita de atributos
preditores permitidos no arquivo data_preparation.py.

A função separar_atributos_alvo realiza a separação
entre os atributos de entrada (X) e a variável-alvo (y).

A função verificar_vazamento identifica atributos
que não pertencem à lista permitida e interrompe
a execução caso sejam encontrados.

## 6. Testes realizados

### Teste positivo

Entrada contendo os 10 atributos permitidos.

Resultado: aprovado.

### Teste negativo

Inclusão proposital da coluna PESO em uma cópia de X.

Resultado: atributo proibido detectado corretamente.

## 7. Resultado da preparação

- Registros originais: 120786.
- Registros considerados: 120785.
- Atributos originais: 37.
- Atributos preditores selecionados: 10.
- Atributos excluídos da entrada: 27.
- Dimensões de X: (120785, 10).
- Dimensões de y: (120785,).
- Classe 0: 108313 registros (89,67%).
- Classe 1: 12472 registros (10,33%).

## 8. Limitações

A base SINASC contém informações registradas
retrospectivamente na declaração de nascimento. Não é possível reconstruir perfeitamente quais
informações estavam disponíveis em cada consulta
pré-natal.

A seleção dos atributos busca reduzir o vazamento
temporal, mas não elimina todas as limitações
decorrentes da natureza retrospectiva da base. A verificação automática de colunas não substitui
a análise conceitual de vazamento de dados.


## 9. Referências

- Dicionário de dados SINASC — Dados Abertos CSV/XLSX.
- Base SINASC 2024 — DNRE2024.
- Trabalho Final Integrado — UNIFSA, 2026.
