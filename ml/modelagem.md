# Documentação Técnica — Dados e Modelagem
## 1. Objetivo

Desenvolver um modelo de classificação supervisionada para identificar padrões associados ao baixo peso ao nascer, utilizando atributos maternos e informações relacionadas ao pré-natal disponíveis na base SINASC.

O modelo possui finalidade exclusivamente acadêmica e experimental, não sendo indicado para diagnóstico ou tomada de decisões clínicas.

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

As faixas foram definidas utilizando as predições out-of-fold do conjunto de treinamento.

| Faixa | Intervalo |
|---|---|
| BAIXO | Score inferior a 0,40 |
| MEDIO | Score maior ou igual a 0,40 e inferior a 0,43 |
| ALTO | Score maior ou igual a 0,43 |

O limite de 0,40 foi selecionado considerando a distribuição dos registros entre as faixas baixa e moderada.

O limiar de classificação permanece igual a 0,43.

As faixas não representam categorias clínicas validadas, e os scores não devem ser interpretados como probabilidades calibradas.

## 13. Exportação dos artefatos

Os artefatos foram disponibilizados na pasta `ml/artifacts/`.

| Arquivo | Finalidade |
|---|---|
| modelo_sinasc.joblib | Pipeline treinado |
| metadata.json | Configuração, métricas, versão e limitações |
| input_schema.json | Contrato interno dos atributos de entrada |

O arquivo Joblib possui aproximadamente 2,19 MB.

O pipeline exportado foi carregado e testado novamente nos 24.156 registros de teste, apresentando scores numericamente equivalentes e classificações idênticas às do modelo original.

Os módulos `preprocessing.py` e `data_preparation.py` devem permanecer disponíveis no ambiente de inferência.

## 14. Teste independente de inferência

Foi desenvolvido o arquivo `ml/tests/teste_inferencia.py` para verificar o funcionamento do modelo fora do notebook.

O teste utilizou exclusivamente dados fictícios, sem carregar a base original.

Resultado obtido:

```json
{
    "versao_modelo": "1.0.0",
    "score_experimental": 0.339087,
    "classe_prevista": 0,
    "faixa_experimental": "Baixo",
    "threshold": 0.43,
    "finalidade": "Exclusivamente academica"
}
```

O teste foi concluído com sucesso.

## 15. Ambiente e dependências

| Dependência | Versão utilizada |
|---|---|
| Python | 3.11.9 |
| Scikit-learn | 1.9.1 |
| Pandas | 3.1.1 |
| NumPy | 2.4.6 |
| SciPy | 1.17.1 |
| Joblib | 1.6.0 |

As principais dependências do modelo foram fixadas no arquivo `ml/requirements.txt`.

A compatibilidade das dependências instaladas foi verificada com `pip check`.

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

## 18. Limitações

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

O modelo não deve ser utilizado como ferramenta de diagnóstico, triagem clínica ou recomendação de condutas médicas.

## 19. Conclusão

A etapa de dados e modelagem contemplou análise exploratória, definição da variável-alvo, prevenção de vazamento de dados, pré-processamento, treinamento, validação cruzada, balanceamento, otimização de hiperparâmetros, seleção do limiar e avaliação final.

O Pipeline treinado foi exportado e validado para reutilização pelo serviço de IA.

Os resultados demonstram a viabilidade técnica da integração dos componentes de Machine Learning ao sistema acadêmico, respeitando as limitações identificadas durante os experimentos.