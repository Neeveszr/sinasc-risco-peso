# Documentação Técnica — Dados e Modelagem

## 1. Objetivo

Desenvolver um modelo de classificação supervisionada para identificar padrões associados ao baixo peso ao nascer, utilizando atributos maternos e informações relacionadas ao pré-natal disponíveis na base SINASC.

## 2. Base de dados

Foi utilizada a base SINASC do município de São Paulo, referente ao ano de 2024, disponibilizada no arquivo `DNRE2024.csv`.

| Informação | Quantidade |
|---|---:|
| Registros originais | 120.786 |
| Colunas originais | 37 |
| Duplicatas exatas removidas | 5 |
| Registros sem peso removidos | 1 |
| Registros válidos | 120.780 |
| Registros de treinamento | 96.624 |
| Registros de teste | 24.156 |
| Atributos preditores | 10 |

A base original não é versionada no repositório.

Os registros com inconsistências obstétricas foram mantidos provisoriamente, considerando as limitações de qualidade dos dados históricos.

## 3. Definição da variável-alvo

A variável-alvo `baixo_peso` foi construída a partir do atributo `PESO`.

- Classe 0: peso ao nascer maior ou igual a 2.500 gramas.
- Classe 1: peso ao nascer inferior a 2.500 gramas.

A classe positiva corresponde a aproximadamente 10,32% dos registros válidos, caracterizando uma base desbalanceada.

## 4. Seleção dos atributos e prevenção de vazamento

Foram selecionados dez atributos originais, considerando sua disponibilidade no cenário experimental anterior ao nascimento.

| Atributo | Descrição |
|---|---|
| FxEtPartur | Faixa etária materna |
| ESTCIVMAE | Estado civil |
| ESCMAE2010 | Escolaridade |
| GRAVIDEZ | Tipo de gravidez |
| QTDGESTANT | Gestações anteriores |
| QTDPARTNOR | Partos normais anteriores |
| QTDPARTCES | Partos cesáreos anteriores |
| QTDFILVIVO | Filhos vivos |
| QTDFILMORT | Perdas fetais e abortos |
| MESPRENAT | Mês de início do pré-natal |

O atributo PESO foi utilizado exclusivamente para construir a variável-alvo.

Também foram excluídos atributos relacionados ao parto, ao recém-nascido e informações cuja disponibilidade anterior ao nascimento não poderia ser garantida.

O detalhamento das exclusões encontra-se em `selecao_atributos.md`.

## 5. Pipeline de pré-processamento

O pipeline foi desenvolvido utilizando Scikit-learn, garantindo que as transformações aplicadas durante o treinamento também sejam realizadas durante a inferência.

### 5.1. Limpeza dos atributos

- Padronização das faixas etárias.
- Tratamento dos códigos categóricos ignorados.
- Conversão e validação de valores numéricos.
- Tratamento de valores ausentes.
- Tratamento de meses inválidos de início do pré-natal.
- Criação do indicador `MESPRENAT_AUSENTE`.

O indicador representa a ausência de uma informação válida sobre o mês de início do pré-natal, não permitindo distinguir ausência de acompanhamento de ausência de informação.

### 5.2. Transformação dos atributos

A transformação foi implementada com `ColumnTransformer`.

Para atributos numéricos:

- Imputação pela mediana.
- Padronização com StandardScaler.

Para atributos categóricos:

- Imputação com a categoria Ignorado.
- Codificação com OneHotEncoder.
- Tratamento de categorias desconhecidas.

O indicador de ausência do mês de início do pré-natal é encaminhado diretamente ao classificador.

## 6. Metodologia experimental

A base foi dividida de maneira estratificada:

- 80% para treinamento.
- 20% para teste final.

Foi utilizada validação cruzada estratificada com cinco folds e random_state igual a 42.

O conjunto de teste permaneceu reservado durante a seleção dos modelos, hiperparâmetros e limiar de classificação.

Foram consideradas as seguintes métricas:

- Accuracy.
- Recall.
- Precision.
- F1-score.
- F2-score.
- Average Precision.
- ROC-AUC.

O Recall recebeu atenção especial devido à importância experimental dos falsos negativos. O F2-score foi utilizado como medida complementar, atribuindo maior importância ao Recall.

## 7. Modelos avaliados

Inicialmente foram avaliados:

- DummyClassifier, como baseline.
- Regressão Logística.
- Random Forest.
- Regressão Logística balanceada.
- Random Forest balanceado.

### 7.1. Resultados iniciais da validação cruzada

Resultados obtidos com a classificação padrão dos modelos:

| Modelo | Recall | Precision | F2 | AP | ROC-AUC |
|---|---:|---:|---:|---:|---:|
| DummyClassifier | 0,0000 | 0,0000 | 0,0000 | 0,1032 | 0,5000 |
| Regressão Logística | 0,1661 | 0,6396 | 0,1950 | 0,2694 | 0,6383 |
| Random Forest | 0,1531 | 0,6672 | 0,1810 | 0,2786 | 0,6454 |
| Logística balanceada | 0,3839 | 0,1934 | 0,3206 | 0,2688 | 0,6388 |
| Random Forest balanceado | 0,3501 | 0,2133 | 0,3099 | 0,2742 | 0,6437 |

## 8. Otimização de hiperparâmetros

Foi utilizado RandomizedSearchCV para investigar diferentes configurações do Random Forest.

A busca avaliou 20 configurações com cinco folds, totalizando 100 ajustes durante a validação cruzada.

A melhor configuração encontrada na busca apresentou:

- 300 árvores.
- Profundidade máxima de 12.
- Min_samples_split igual a 10.
- Min_samples_leaf igual a 1.
- Max_features igual a log2.
- Sem balanceamento de classes.
- Average Precision de 0,2802.

Apesar da melhoria observada, o aumento de complexidade em relação à configuração com 100 árvores não foi considerado suficientemente vantajoso para a escolha final.

## 9. Ajuste do limiar de classificação

Foram utilizadas predições out-of-fold do conjunto de treinamento para investigar diferentes limiares.

A comparação final contemplou cinco configurações candidatas e 91 limiares por configuração, totalizando 455 combinações.

Foi adotado um cenário experimental de Recall mínimo de 70%, buscando um compromisso entre sensibilidade, Precision e quantidade de falsos positivos.

| Modelo | Limiar | Recall | Precision | F2 |
|---|---:|---:|---:|---:|
| Random Forest balanceado | 0,43 | 0,7431 | 0,1259 | 0,3752 |
| Random Forest otimizado | 0,08 | 0,7591 | 0,1250 | 0,3769 |
| Logística balanceada | 0,43 | 0,7443 | 0,1245 | 0,3729 |
| Regressão Logística | 0,08 | 0,7431 | 0,1241 | 0,3720 |
| Random Forest | 0,08 | 0,7602 | 0,1240 | 0,3752 |

Os resultados utilizados na seleção são exploratórios. A reutilização dos folds durante a otimização e a comparação pode introduzir otimismo nas estimativas.

O conjunto de teste reservado foi utilizado para a avaliação final.

## 10. Modelo selecionado

Foi selecionado o Random Forest balanceado, considerando seu desempenho semelhante ao modelo otimizado, maior Precision no cenário escolhido, menor quantidade de falsos positivos e menor número de árvores.

| Parâmetro | Valor |
|---|---|
| Algoritmo | RandomForestClassifier |
| n_estimators | 100 |
| max_depth | 12 |
| min_samples_split | 10 |
| min_samples_leaf | 5 |
| max_features | sqrt |
| criterion | gini |
| class_weight | balanced |
| random_state | 42 |
| Threshold | 0,43 |

O modelo definitivo foi treinado utilizando os 96.624 registros do conjunto de treinamento.

A diferença entre as configurações avaliadas não permite afirmar superioridade estatística.

## 11. Avaliação final

A avaliação foi realizada utilizando os 24.156 registros reservados para teste.

| Métrica | Resultado |
|---|---:|
| Accuracy | 0,4449 |
| Recall | 0,7398 |
| Precision | 0,1263 |
| F1-score | 0,2158 |
| F2-score | 0,3753 |
| Average Precision | 0,2719 |
| ROC-AUC | 0,6407 |

### 11.1. Matriz de confusão

| Classe real | Previsto 0 | Previsto 1 |
|---|---:|---:|
| Sem baixo peso | 8.901 | 12.761 |
| Baixo peso | 649 | 1.845 |

O modelo identificou aproximadamente 73,98% dos casos reais de baixo peso. Entretanto, somente 12,63% das classificações positivas corresponderam a casos reais.

### 11.2. Análise dos resultados

As métricas do conjunto de teste permaneceram próximas das obtidas durante a seleção por predições out-of-fold.

Essa proximidade indica consistência do desempenho dentro da base estudada, mas não comprova capacidade de generalização para outras populações.

A elevada quantidade de falsos positivos e a capacidade discriminatória limitada constituem restrições importantes.

## 12. Faixas experimentais de score

Após a revisão do Pull Request, foram investigadas novas configurações utilizando os percentis dos scores out-of-fold do conjunto de treinamento. A configuração anterior concentrava 60,93% dos registros na faixa ALTO, com 12,59% de positivos.

| Percentil superior | Registros na faixa ALTO | Proporção de positivos na faixa | Casos positivos capturados |
|---|---:|---:|---:|
| 80 | 20,01% | 19,61% | 38,01% |
| 85 | 15,00% | 22,67% | 32,94% |
| 90 | 10,00% | 28,29% | 27,41% |

Foi selecionado o percentil 85 como compromisso experimental entre concentração de positivos e quantidade de registros selecionados, em resposta à sugestão da revisão de concentrar aproximadamente 10% a 15% dos maiores scores na faixa ALTO. O percentil 50 foi adotado para separar BAIXO e MEDIO. Essa escolha não demonstra superioridade estatística ou adequação clínica.

### 12.1. Limites definidos

| Faixa | Limite inferior (inclusivo) | Limite superior |
|---|---:|---:|
| BAIXO | 0,0 | 0,4495018644184755 (exclusivo) |
| MEDIO | 0,4495018644184755 | 0,5042154221007533 (exclusivo) |
| ALTO | 0,5042154221007533 | 1,0 (inclusivo) |

Os limites devem ser utilizados com a precisão armazenada em `metadata.json`.

### 12.2. Distribuição nas predições out-of-fold

| Faixa | Registros | Casos positivos | Percentual da base | Proporção de positivos na faixa | Casos positivos capturados |
|---|---:|---:|---:|---:|---:|
| BAIXO | 48.261 | 3.446 | 49,95% | 7,14% | 34,55% |
| MEDIO | 33.869 | 3.243 | 35,05% | 9,58% | 32,51% |
| ALTO | 14.494 | 3.286 | 15,00% | 22,67% | 32,94% |
| Total | 96.624 | 9.975 | 100,00% | 10,32% | 100,00% |

O limiar de classificação binária permanece em 0,43. As faixas de apresentação não equivalem às classes binárias: um score de 0,44, por exemplo, corresponde à classe 1 e à faixa BAIXO. O recall de 73,98% no teste final não corresponde à captura de positivos pela faixa ALTO, que foi de 32,94% nas predições out-of-fold do treinamento.

Os novos limites foram escolhidos sem utilizar o conjunto de teste. A avaliação complementar das faixas no conjunto de teste foi realizada posteriormente, preservando integralmente os limites definidos. A distribuição das faixas em novos dados não é garantida.

O score produzido pelo Random Forest balanceado não é uma probabilidade calibrada. As faixas são categorias experimentais relativas à distribuição de referência, sem validação clínica ou indicação para triagem.

### Avaliação das faixas no conjunto de teste

Após a definição dos limites experimentais,
foi realizada uma avaliação complementar utilizando
o conjunto de teste original, composto por 24.156
registros e 2.494 casos positivos de baixo peso.

Foram preservados os seguintes limites:

- BAIXO: score inferior a 0,4495018644184755.
- MEDIO: score maior ou igual a 0,4495018644184755
  e inferior a 0,5042154221007533.
- ALTO: score maior ou igual a 0,5042154221007533.

O limiar de classificação binária permaneceu em 0,43.

#### Resultados no conjunto de teste

| Faixa | Registros | Casos positivos | Percentual da base | Proporção de positivos | Positivos capturados |
|---|---:|---:|---:|---:|---:|
| BAIXO | 12.016 | 863 | 49,74% | 7,18% | 34,60% |
| MEDIO | 8.289 | 788 | 34,31% | 9,51% | 31,60% |
| ALTO | 3.851 | 843 | 15,94% | 21,89% | 33,80% |
| Total | 24.156 | 2.494 | 100% | 10,32% | 100% |

Os percentuais foram arredondados para apresentação.

#### Comparação entre treinamento OOF e teste

| Indicador | Faixa | OOF (%) | Teste (%) | Diferença (p.p.) |
|---|---|---:|---:|---:|
| Percentual da base | BAIXO | 49,9472 | 49,7433 | -0,2039 |
| Percentual da base | MEDIO | 35,0524 | 34,3145 | -0,7379 |
| Percentual da base | ALTO | 15,0004 | 15,9422 | +0,9418 |
| Proporção de positivos | BAIXO | 7,1403 | 7,1821 | +0,0417 |
| Proporção de positivos | MEDIO | 9,5751 | 9,5066 | -0,0686 |
| Proporção de positivos | ALTO | 22,6715 | 21,8904 | -0,7810 |
| Positivos capturados | BAIXO | 34,5464 | 34,6030 | +0,0567 |
| Positivos capturados | MEDIO | 32,5113 | 31,5958 | -0,9154 |
| Positivos capturados | ALTO | 32,9424 | 33,8011 | +0,8588 |

As diferenças foram calculadas utilizando os valores
originais, antes do arredondamento.

#### Análise dos resultados

A faixa ALTO concentrou 15,94% dos registros
do conjunto de teste, contra 15,00% nas predições
out-of-fold do treinamento.

A proporção observada de baixo peso nessa faixa
foi de 21,89%, superior à prevalência geral
de 10,32% do conjunto de teste.

A faixa ALTO concentrou 33,80% dos casos positivos,
enquanto os demais 66,20% permaneceram distribuídos
entre as faixas BAIXO e MEDIO.

Todos os indicadores apresentaram diferenças
absolutas inferiores a um ponto percentual
entre os resultados OOF e teste.

Esses resultados demonstram consistência descritiva
das faixas dentro da base estudada, sem comprovar
capacidade de generalização para outras populações.

Os limites foram preservados integralmente.
Nenhum treinamento, ajuste de hiperparâmetros
ou redefinição das faixas foi realizado após
a observação dos resultados do teste.

As métricas originais do classificador também
foram reproduzidas, incluindo a matriz de confusão.

O recall de 73,98%, associado ao limiar de 0,43,
não deve ser confundido com os 33,80% de casos
positivos concentrados na faixa ALTO.

O experimento está documentado no notebook
`ml/notebooks/04_avaliacao_faixas_teste.ipynb`.

As faixas permanecem exclusivamente acadêmicas
e não possuem validação para triagem clínica.

## 13. Exportação dos artefatos

Os artefatos foram disponibilizados na pasta `ml/artifacts/`.

| Arquivo | Finalidade |
|---|---|
| modelo_sinasc.joblib | Pipeline treinado |
| metadata.json | Configuração, métricas, versão e limitações |
| input_schema.json | Contrato interno dos atributos de entrada |

O arquivo Joblib possui aproximadamente 2,19 MiB.

O pipeline exportado foi carregado e testado novamente nos 24.156 registros de teste, apresentando scores numericamente equivalentes e classificações idênticas às do modelo original.

Os módulos `preprocessing.py` e `data_preparation.py` devem permanecer disponíveis no ambiente de inferência.

## 14. Teste independente de inferência

Foi desenvolvido o arquivo `ml/tests/teste_inferencia.py` para verificar o funcionamento do modelo fora do notebook.

O teste utilizou exclusivamente dados fictícios, sem carregar a base original.

Resultado obtido:

```json
{
    "versao_modelo": "1.0.0",
    "score": 0.339087,
    "score_calibrado": false,
    "classe_prevista": 0,
    "faixa_experimental": "BAIXO",
    "threshold": 0.43,
    "finalidade": "Exclusivamente academica"
}
```

O teste atualizado foi concluído com sucesso, incluindo a validação dos metadados, dos limites exatos das faixas e dos atributos de entrada. Trata-se de um teste independente de inferência com um registro fictício, não de um teste completo da API.

## 15. Ambiente e dependências

| Dependência | Versão utilizada |
|---|---|
| Python | 3.11.9 |
| Scikit-learn | 1.9.1 |
| Pandas | 3.1.1 |
| NumPy | 2.4.6 |
| SciPy | 1.17.1 |
| Joblib | 1.6.0 |
| SHAP | 0.51.0 |
| FastAPI | 0.165.0 |
| Uvicorn | 0.54.0 |
| Pydantic | 2.14.4 |

As principais dependências do modelo foram fixadas no arquivo `ml/requirements.txt`.

A compatibilidade declarada das dependências instaladas foi verificada com `pip check`, sem conflitos reportados. As importações de SHAP, FastAPI, Uvicorn e Pydantic também foram executadas com sucesso. Isso não comprova o funcionamento integral da explicabilidade ou do serviço. Matplotlib e Jupyter permanecem sem versões fixadas.

A instalação em um ambiente completamente novo ainda deverá ser validada antes do deploy.

## 16. Reprodução dos experimentos

A partir da raiz do repositório, utilizando um ambiente virtual Python compatível, instalar as dependências:

```bash

python -m pip install -r ml/requirements.txt

```

Disponibilizar a base original em:

```text

data/raw/DNRE2024.csv

```

Executar os notebooks na seguinte ordem:

1. `01_analise_exploratoria.ipynb`
2. `02_analise_preprocessamento.ipynb`
3. `03_treinamento_modelos.ipynb`
4. `04_avaliacao_faixas_teste.ipynb`
Para testar o modelo exportado:

```bash

python ml/tests/teste_inferencia.py

```

O teste de inferência não necessita da base original.

A execução integral dos notebooks em um ambiente novo ainda deverá ser confirmada.

## 17. Integração com os demais componentes

O serviço de IA deverá carregar o Pipeline, os metadados e o contrato interno de entrada.

A classificação deverá utilizar o score da classe positiva e aplicar explicitamente o limiar de 0,43.

A validação externa dos campos é responsabilidade da API. O contrato interno do Pipeline não substitui o contrato público do serviço.

As divergências relativas aos códigos ignorados, valores ausentes e regras de consistência obstétrica deverão ser alinhadas entre IA, governança e Backend.

A explicabilidade com SHAP, a implementação do serviço FastAPI e a consolidação do Model Card serão realizadas na etapa de avaliação e integração.

### 17.1. Orientações de integração após revisão

- Adotar o campo `score` em substituição a `probabilidade`, mediante alinhamento do contrato público entre IA, Backend e Frontend. Não apresentar o score como percentual de chance.
- Consultar os limites de apresentação diretamente em `metadata.json` e utilizar as categorias BAIXO, MEDIO e ALTO.
- Preservar o limiar binário de 0,43, independentemente das faixas.
- Enviar `null` para faixa etária desconhecida. O código histórico `Ign` continua sendo tratado internamente pelo pipeline, mas não integra os valores aceitos no contrato de entrada atualizado.
- Disponibilizar `ml/src` no caminho de importação Python antes de carregar o Joblib, permitindo localizar os módulos personalizados, como no teste independente.
- Utilizar as versões fixadas em `ml/requirements.txt`.
- Sinalizar a ausência de GRAVIDEZ e MESPRENAT na interface acadêmica e alinhar as regras de preenchimento com a API.

A versão do modelo permanece 1.0.0, pois o pipeline serializado não foi alterado. A atualização do contrato público, a integração efetiva do serviço, a explicabilidade e a consolidação do Model Card dependem de alinhamento com os responsáveis.

## 18. Análise de sensibilidade a valores ausentes

Foi utilizado um registro fictício, omitindo individualmente cada um dos dez atributos e mantendo os demais inalterados. Os resultados mais relevantes foram:

| Atributo ausente | Score original | Score alterado | Variação | Faixa original | Faixa alterada |
|---|---:|---:|---:|---|---|
| GRAVIDEZ | 0,339087 | 0,732275 | +0,393188 | BAIXO | ALTO |
| MESPRENAT | 0,339087 | 0,525467 | +0,186380 | BAIXO | ALTO |

Os demais oito atributos não provocaram alteração de faixa nesse registro fictício.

### 18.1. Investigação no conjunto de treinamento

| Atributo | Situação após limpeza | Registros | Casos positivos | Proporção de positivos |
|---|---|---:|---:|---:|
| GRAVIDEZ | Ausente | 8 | 2 | 25,00% |
| GRAVIDEZ | Informado | 96.616 | 9.973 | 10,32% |
| MESPRENAT | Ausente ou inválido | 811 | 225 | 27,74% |
| MESPRENAT | Informado | 95.813 | 9.750 | 10,18% |

GRAVIDEZ ausente é tratada como categoria Ignorado; MESPRENAT ausente ou inválido é tratado por imputação e pelo indicador MESPRENAT_AUSENTE. A baixa representatividade de GRAVIDEZ ausente (apenas oito registros) torna incerta a frequência observada e pode contribuir para a instabilidade nessa condição.

Esses resultados são associações descritivas e não comprovam causalidade. O teste de sensibilidade com um único registro fictício não permite generalizar o comportamento. A ausência de informação não deve ser interpretada como maior risco real. Recomenda-se sinalizar dados ausentes na interface acadêmica e alinhar as regras de validação com a API.

## 19. Limitações

- Base restrita ao município de São Paulo em 2024.
- Desbalanceamento da variável-alvo.
- Baixa Precision.
- Elevada quantidade de falsos positivos.
- Capacidade discriminatória limitada.
- Ausência de calibração dos scores.
- Ausência de validação externa e clínica.
- Limitações decorrentes da natureza retrospectiva dos dados.
- Possíveis inconsistências nos registros históricos.
- Diferenças entre os valores históricos utilizados no treinamento e os limites de validação da API.
- Faixas experimentais sem validação clínica.
- Distribuição das faixas por percentis não garantida em novos dados.
- A faixa ALTO concentra apenas 32,94% dos casos positivos nas predições out-of-fold e 33,80% no conjunto de teste. Esses indicadores não equivalem ao recall do classificador.
- Sensibilidade à ausência de GRAVIDEZ e MESPRENAT demonstrada em um único registro fictício, sem avaliação agregada da estabilidade dos scores.
- Poucos exemplos de GRAVIDEZ ausente no treinamento.
- O score não deve ser apresentado como percentual de chance; o contrato público ainda precisa ser alinhado.

O modelo não deve ser utilizado como ferramenta de diagnóstico, triagem clínica ou recomendação de condutas médicas.

## 20. Conclusão

A etapa de dados e modelagem contemplou análise exploratória, definição da variável-alvo, prevenção de vazamento de dados, pré-processamento, treinamento, validação cruzada, balanceamento, otimização de hiperparâmetros, seleção do limiar e avaliação final.

O Pipeline treinado foi exportado e validado para reutilização pelo serviço de IA.

Os resultados demonstram a viabilidade técnica de reutilizar o pipeline no sistema acadêmico, respeitando as limitações identificadas durante os experimentos.

Após a revisão do PR, foram redefinidas as faixas de apresentação, documentada a sensibilidade a valores ausentes, atualizados os metadados e o contrato interno e fixadas as dependências adicionais.

O teste independente de inferência foi concluído com sucesso.

A avaliação complementar das faixas no conjunto de teste apresentou resultados próximos aos observados nas predições out-of-fold, com diferenças inferiores a um ponto percentual em todos os indicadores avaliados.

A faixa ALTO concentrou 15,94% dos registros de teste, com frequência observada de baixo peso de 21,89% e captura de 33,80% dos casos positivos.

Os limites experimentais, o modelo treinado e o limiar de classificação de 0,43 foram preservados integralmente.

A integração completa, a consolidação do Model Card e a validação em ambiente novo ainda deverão ser confirmadas.

O modelo permanece exclusivamente acadêmico, sem calibração dos scores ou validação clínica.
