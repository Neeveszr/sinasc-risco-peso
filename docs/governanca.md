# Governança de Dados | Contexto 4A: risco de baixo peso ao nascer (SINASC SP, 2024)

Documento de governança do Trabalho Final Integrado (UNIFSA). Os números vêm dos arquivos de
`governanca/saida/`, gerados por `governanca/governanca_sinasc.py`.

## 1. Escopo e finalidade

O sistema apoia a equipe de saúde da família (UBS e agentes comunitários) a priorizar o
acompanhamento pré-natal das gestantes com maior risco de dar à luz um bebê com baixo peso
(menos de 2.500 g). A predição é feita durante o pré-natal, por isso só entram atributos que
já existem nesse momento. O resultado é apoio à decisão: a equipe continua responsável pelo
acompanhamento clínico.

Base: SINASC, nascidos vivos do município de São Paulo, ano de referência **2024**
(`DNRE2024.csv`), 120.786 registros e 37 colunas. Os 10 atributos do modelo são os códigos
originais do SINASC, sem agrupamento.

## 2. Catálogo e dicionário de dados (requisito 6.1)

- `01_catalogo_dados.csv`: para cada coluna, descrição, tipo, percentual de faltantes
  (calculado), classificação LGPD, confidencialidade, decisão e justificativa. Todas as
  descrições estão marcadas como pendentes de conferência com o dicionário oficial.
- `06_contrato_entrada_api.json`: para cada um dos 10 atributos, o nome no front e no
  back-end (`nomeFrontBackend`), o nome enviado ao serviço de IA (`nomeServicoIA`, nome original
  do SINASC) e os valores válidos. Seção 8.

## 3. Classificação dos dados (requisito 6.2)

Dimensão LGPD:
- Dado pessoal sensível (art. 5º, II): raça/cor da mãe e todos os atributos de saúde
  (peso, Apgar, anomalias, antecedentes obstétricos, pré-natal e parto).
- Dado pessoal: idade, escolaridade, estado civil, ocupação, data de nascimento, UF de
  naturalidade e faixa etária do pai.
- Não pessoal: códigos de município e país e campos administrativos.

Dimensão de confidencialidade:
- Restrito: raça/cor, data de nascimento e sexo do recém-nascido.
- Confidencial: atributos de saúde.
- Interno: demais atributos pessoais.
- Público: códigos de município e país.

A base não traz nome, CPF ou endereço, mas combinações de atributos podem facilitar a
identificação de uma pessoa. Por isso a classificação vale mesmo para a base de origem aberta.

## 4. Proteção dos dados pessoais (requisito 6.3)

Princípios da LGPD: finalidade, necessidade e segurança. Medidas aplicadas:

1. Remoção de identificadores e de quase-identificadores sem necessidade: data de nascimento,
   município, UF de naturalidade, ocupação e faixa etária do pai.
2. Remoção de raça/cor do modelo. A característica é sensível, a necessidade para a finalidade
   não foi demonstrada e há risco de discriminação. Equidade racial pode ser avaliada só em
   relatório agregado, fora do modelo.
3. Remoção de atributos do parto ou posteriores ao pré-natal (vazamento temporal): semanas
   de gestação, Apgar, tipo de parto, indução, apresentação, anomalia congênita, Robson e
   número de consultas.
4. Treino com a base bruta. O treino usa a base bruta com o pipeline de IA
   (`separar_atributos_alvo`), com os nomes originais do SINASC. O arquivo gerado em
   `data/processed/` (`analise_qualidade_2024.csv`) serve só para análise de qualidade e não
   entra no treino. Ele não é publicado nem versionado, e o acesso fica restrito ao Data
   Custodian.
5. Tabela publicada com k ≥ 5. As tabelas agregadas que saem do grupo (arquivo 09) omitem
   qualquer célula com menos de 5 registros, mostradas como "<5".
6. Listas por registro (linha do CSV original, peso e histórico obstétrico) ficam em
   `data/interim/`, que não é versionada. Só as contagens vão para o repositório.
7. Pseudonimização: não aplicável, porque a base não tem identificador direto.
8. Todo atributo mantido no modelo está justificado no catálogo.

Risco de reidentificação na base de análise e na base de treino: 13,2% dos registros estão em
combinações de atributos com menos de 5 pessoas (k mínimo = 1). Isso é aceito porque as bases
não são publicadas. O risco deve ser tratado nos controles de acesso, não na publicação.

## 5. Qualidade de dados (requisito 6.4)

Regras em `governanca_sinasc.py`. Antes e depois estão em `02_qualidade_antes_depois.csv`.

Política: nada é excluído por suspeita. Valores fora de faixa e inconsistências são
sinalizados, e só são excluídos duplicatas exatas e registros sem peso, porque sem peso não
há alvo.

| Dimensão | Regra | Ocorrências (2024) | Tratamento |
|---|---|---|---|
| Completude | PESO ausente | 1 | Excluído (sem alvo) |
| Consistência | Duplicatas exatas | 5 | Excluídas |
| Acurácia | PESO fora de 300 a 6.500 g | 13 | Mantido e listado para revisão |
| Acurácia | Mês de início do pré-natal fora de 1 a 9 da API (99 e 10) | 112 | Tratado como ausente (110 são 99, código de não informado; 2 são 10) |
| Acurácia | Semanas de gestação fora de 22 a 44 | 71 | Variável não usada no modelo |
| Acurácia | APGAR fora de 0 a 10 | 156 | Variável não usada no modelo |
| Acurácia | Contagens obstétricas acima de 40 | 0 | Sem ação |
| Consistência | Partos anteriores maiores que gestações anteriores (K1) | 176 | Mantido e marcado como suspeito |
| Consistência | Número de filhos menor que partos anteriores (K2) | 81 | Mantido e marcado como suspeito (todos também em K1) |
| Consistência | Zero consultas com mês de início informado (K3) | 56 | Sem efeito no modelo (variável não usada) |
| Tempestividade | Data fora do ano de referência 2024 | 0 | Sem ação |

Os registros suspeitos e os pesos para revisão estão em `data/interim/` (não versionado),
com o número da linha do CSV original. Os casos de K1 e K2 só devem ser excluídos depois que
a equipe confirmar as definições de `QTDGESTANT`, `QTDPARTNOR`, `QTDPARTCES`, `QTDFILVIVO`
e `QTDFILMORT` no dicionário oficial. Os 13 casos de peso fora de 300 a 6.500 g podem ser
bebês reais de peso muito baixo, por isso ficam no treino até a revisão.

Resultado: 120.786 registros de entrada e 120.780 no arquivo de treino (6 excluídos: 5
duplicatas e 1 sem peso). Prevalência de baixo peso no treino: 10,32%.

Completude após o tratamento: todas as variáveis do modelo têm até 0,85% de ausentes
(`03_completude_variaveis.csv`). O mês de início do pré-natal é a que mais tem ausentes.

Tempestividade: a base é de 2024, cerca de dois anos antes da data de referência do trabalho
(novembro de 2026). Isso deve ser declarado no README e na apresentação.

## 6. Papéis e responsabilidades (requisito 6.5)

Sugestão para o sistema em uso real. O grupo deve adaptar e justificar:

- Data Owner (dono do dado e da decisão): Secretaria Municipal da Saúde de São Paulo, pela
  coordenação de atenção básica. Aprova a finalidade e os critérios de risco.
- Data Steward (guardião da qualidade): equipe de vigilância em saúde e dados. Mantém o
  dicionário de dados, executa as regras de qualidade e aprova mudanças nos campos.
- Data Custodian (custódia técnica): equipe de TI da secretaria ou do provedor de nuvem.
  Guarda o banco e o arquivo de treino, aplica criptografia, backups e controles de acesso.
  Atua na infraestrutura, sem perfil dentro da aplicação.
- Controlador (LGPD, art. 5º, VI): Secretaria Municipal da Saúde, que decide a finalidade.
- Operador (art. 5º, VII): equipe de desenvolvimento e provedor de hospedagem, que tratam
  os dados em nome do controlador, sob contrato com cláusulas de segurança.
- Encarregado (art. 5º, VIII): pessoa designada pelo controlador como canal com os titulares
  e com a ANPD.

No trabalho acadêmico, o grupo simula esses papéis. A declaração deve deixar isso claro.

## 7. Matriz de papéis × recursos e controles (requisito 6.6)

A aplicação tem dois perfis: **OPERADOR** e **GESTOR_AUDITOR**. O gestor é quem cria e
gerencia os operadores.

| Recurso | OPERADOR | GESTOR_AUDITOR |
|---|---|---|
| Realizar predição | Sim | Não |
| Consultar histórico próprio de predições | Sim | Não |
| Consultar histórico geral de predições | Não | Sim |
| Acessar auditoria e pedidos de revisão humana | Não | Sim |
| Ver documentação do modelo (versão ao usuário) | Sim | Sim (completa) |
| Criar, ativar, desativar e alterar perfis de usuários | Não | Sim |

Decisão tomada: o cadastro público foi removido (PR #12, já na `main`). Só o gestor/auditor
cria contas. O primeiro gestor é criado a partir de `ADMIN_EMAIL` e `ADMIN_PASSWORD`, que
devem ser definidos no ambiente, nunca com os valores de exemplo.

Controles de segurança:
- Criptografia em trânsito (HTTPS/TLS) e em repouso no banco de dados (proposto).
- Senhas com BCrypt (implementado no back-end).
- Tokens JWT com expiração. Refresh token é **proposto**, ainda não implementado.
- Registro de acessos e operações: tabela de auditoria com usuário, operação e data/hora.
  Proposta: registrar também tentativas de login falhas (usuário informado, data/hora e
  origem, nunca a senha) e acessos a predições e histórico.
- Segredos fora do repositório: `JWT_SECRET` e a senha inicial do gestor devem ser gerados
  para cada ambiente. Os valores de exemplo do `.env.example` não podem ser usados em produção.
- Prevenção de vazamento (DLP): dados brutos e de treino nunca são versionados (`.gitignore`
  bloqueia `data/`). Exportações CSV/PDF só trazem registros que o usuário pode ver.
- Permissões verificadas no back-end, nunca só na interface.

## 8. Contrato de entrada da API (requisito 7.2)

O contrato está em `06_contrato_entrada_api.json`. Para cada um dos 10 atributos ele traz:
- `nomeFrontBackend`: nome usado pelo front e pelo back-end (camelCase).
- `nomeServicoIA`: nome enviado pelo back-end ao serviço de IA (nome original do SINASC).
- `valoresValidos`, `obrigatorio` e `nulo`: o que a API aceita e o que faz com cada caso.

Regras acordadas com o grupo:
- Escolaridade, estado civil e tipo de gravidez: código 9 é "Ignorado", e null também é
  tratado como "Ignorado".
- Mês de início do pré-natal: 1 a 9. Null significa "não iniciado ou não informado".
  O código 99 não é aceito pela API.
- Faixa etária com hífen (40-44). O desconhecido vai como null. O valor "Ign" não é aceito.
- As 5 contagens (gestações anteriores, partos normais, partos cesáreos, filhos vivos e
  perdas fetais) são obrigatórias, de 0 a 15. Null gera erro 422.
- Regra entre campos: partos normais + partos cesáreos <= gestações anteriores.

Confirmado com a equipe de IA: no modelo da `main`, null, 99 e 10 em mês de início do
pré-natal dão o mesmo score, e null e "Ign" na faixa etária também. Portanto, esses valores
são tratados como ausentes, tanto na API quanto no modelo.

A análise de qualidade (`analise_qualidade_2024.csv`) já segue o domínio da API, com "Ign" e
99 tratados como ausentes. O treino usa a base bruta com o pipeline de IA.

Na base de 2024, 4 registros em cada uma das colunas QTDGESTANT, QTDPARTNOR, QTDPARTCES e
QTDFILMORT passam de 15 e seriam rejeitados pela API. O limite de 40 continua como
sinalização de qualidade (seção 5).

## 9. Direitos do titular e retenção (intermediário)

Propostas para decisão do grupo até sábado, 28/11. Valem só depois de aprovadas pelo
controlador. Não são exigências legais fixas.

- Prazo para revisão humana (LGPD, art. 20): proposta de 5 dias úteis para o gestor/auditor
  responder ao pedido aberto pelo botão "Solicitar revisão humana". A data do pedido, a data
  da resposta e o resultado ficam registrados.
- Retenção do histórico de predições: proposta de 5 anos a partir da data de cada predição.
  Depois desse prazo, os registros identificáveis são excluídos de forma definitiva. Dados
  agregados e sem identificação podem ser mantidos para estatística.

## 10. Limitações e pendências

- O dicionário oficial não veio com a base. As descrições do catálogo seguem o layout
  padrão do SINASC e devem ser conferidas, sobretudo `RETROALIM` e `STDNNOVA`.
- Suposições a confirmar: faixa de peso de 300 a 6.500 g e limite de 40 para contagens
  obstétricas. Ambas definem apenas o que é sinalizado, não o que é excluído.
- Definição de gestação anterior: confirmar se gestações sem parto entram em
  `QTDGESTANT`, porque isso decide os casos de K1 e K2.
- Treino e API: confirmado com a equipe de IA. Null, 99 e 10 em mês de início do pré-natal
  e null e "Ign" na faixa etária geram o mesmo score no modelo.
- Base de 2024: o modelo reflete o período de referência e precisa de atualização periódica.
- O modelo não tem validação externa.

## 11. Declaração de uso de IA generativa

Preencher no README do grupo: ferramenta usada, tarefa e revisão humana, conforme a
Resolução nº 005/2026 (CONSUP) da UNIFSA.
