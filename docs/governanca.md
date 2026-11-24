# Documento de governança de dados

Responsável: Marcos André (com apoio de Antonio e Marcus).

## Básico

### 1. Catálogo e dicionário de dados
Para cada coluna usada: descrição, tipo, origem, valores válidos, responsável e percentual de dados faltantes.

TODO

### 2. Classificação dos dados
| Atributo | LGPD (pessoal / sensível / não pessoal) | Confidencialidade (público / interno / confidencial / restrito) |
|----------|------------------------------------------|------------------------------------------------------------------|
| TODO | | |

### 3. Proteção dos dados pessoais
Atributos que, sozinhos ou combinados, identificam uma pessoa. Medidas aplicadas (remoção, generalização, faixas, pseudonimização) e justificativa com base em finalidade, necessidade e segurança. Todo atributo mantido no modelo precisa de justificativa.

TODO

### 4. Qualidade de dados
Regras de acurácia, completude, consistência e tempestividade, implementadas em código, com relatório antes e depois do tratamento. As regras de caso individual também valem na entrada da API.

TODO

### 5. Papéis e responsabilidades
- Data Owner, Data Steward, Data Custodian: TODO
- LGPD: controlador, operador e encarregado: TODO

### 6. Segurança e controle de acesso
Matriz de papéis × recursos (predição, histórico, documentação do modelo) e controles: criptografia, registro de acessos e prevenção de vazamento (DLP).

| Recurso | Operador | Gestor/auditor |
|---------|----------|----------------|
| Realizar predição | TODO | TODO |
| Consultar o próprio histórico | TODO | TODO |
| Consultar o histórico geral | TODO | TODO |
| Acessar a documentação do modelo | TODO | TODO |

## Intermediário (extra)

### 7. Linhagem dos dados
Diagrama: dado bruto → dado tratado → treino → modelo → predição, com a versão de cada etapa.

TODO

### 8. Direitos do titular e retenção
Revisão de decisões automatizadas (LGPD, art. 20): quem analisa, prazo e como o resultado é registrado. Tempo de retenção do registro de predições.

TODO
