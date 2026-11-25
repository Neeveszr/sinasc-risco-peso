# Governança de Dados | Contexto 4A: risco de baixo peso ao nascer (SINASC SP)

Documento de apoio ao Trabalho Final Integrado (UNIFSA). Os números vêm dos arquivos da pasta `saida/`, gerados por `governanca_sinasc.py`.

## 1. Escopo e finalidade

O sistema apoia a equipe de saúde da família (UBS e agentes comunitários) a priorizar o
acompanhamento pré-natal das gestantes com maior risco de dar à luz um bebê com baixo
peso (menos de 2.500 g). A predição é feita durante o pré-natal, por isso só entram
atributos conhecidos nesse momento. O resultado é apoio à decisão; a equipe continua
responsável pelo acompanhamento clínico.

Base: SINASC, nascidos vivos do município de São Paulo, ano 2014 (`DNRE2014.csv`),
175.817 registros e 37 colunas.

## 2. Catálogo e dicionário de dados (requisito 6.1)

- `saida/01_catalogo_dados.csv`: para cada coluna, descrição, tipo, origem, percentual de
  faltantes, classificação LGPD, confidencialidade, decisão e justificativa. Cada linha está
  marcada como pendente de conferência com o dicionário oficial.
- `saida/06_contrato_entrada_api.json`: campos de entrada do modelo, categorias válidas e
  tratamento do valor "ignorado".

## 3. Classificação dos dados (requisito 6.2)

Dimensão LGPD:
- Dado pessoal sensível (art. 5º, II): raça/cor da mãe e todos os atributos de saúde
  (peso, Apgar, anomalias, antecedentes obstétricos, dados do pré-natal e do parto).
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

1. Remoção de identificadores e quase-identificadores sem necessidade: data de nascimento,
   município, UF de naturalidade, ocupação e faixa etária do pai.
2. Remoção de raça/cor do modelo. A característica é sensível, o ganho para a finalidade não
   foi demonstrado e há risco de discriminação. Equidade racial pode ser avaliada só em
   relatório agregado, fora do modelo.
3. Remoção de atributos do parto ou posteriores ao pré-natal (vazamento temporal): semanas
   de gestação, Apgar, tipo de parto, indução, apresentação, anomalia congênita, Robson e
   número de consultas (consolidado só ao fim da gestação).
4. Generalização em categorias amplas:
   - idade em 10–19, 20–34 e 35+;
   - escolaridade em baixa, média e alta;
   - situação conjugal em com e sem companheiro;
   - gestações, partos, filhos vivos e filhos mortos em poucas faixas;
   - início do pré-natal por trimestre.
5. Supressão de grupos pequenos. Depois da generalização, combinações de atributos com
   menos de 5 registros foram removidas do arquivo tratado. Foram 1.383 registros (0,8%).
   Desses, 538 tinham ausência em algum campo.
6. Pseudonimização: não aplicável, porque a base não tem identificador direto a substituir.
7. Todo atributo mantido no modelo está justificado no catálogo.

Limitação: antes da supressão, havia combinações com k = 1. A generalização reduziu o
problema, mas a supressão é o que garante o k mínimo no arquivo tratado. Por isso, o
arquivo tratado não deve ser publicado sem avaliação adicional do controlador.

## 5. Qualidade de dados (requisito 6.4)

Regras em `governanca_sinasc.py`. Antes e depois do tratamento estão em
`saida/02_qualidade_antes_depois.csv`.

Política de tratamento: nada é excluído por suspeita. Valores fora de faixa e
inconsistências são sinalizados em listas de revisão. Só são excluídos duplicatas exatas e
registros sem peso, porque sem peso não há alvo.

| Dimensão | Regra | Ocorrências | Tratamento |
|---|---|---|---|
| Acurácia | Peso fora de 300 a 6.500 g | 11 | Mantido e listado em `07_peso_para_revisao.csv` |
| Completude | Peso ausente | 4 | Excluído (sem alvo) |
| Acurácia | Semanas de gestação fora de 22 a 44 | 124 | Variável não usada no modelo |
| Acurácia | Apgar fora de 0 a 10 | 227 | Variável não usada no modelo |
| Acurácia | Mês de início do pré-natal inválido (10) | 21 | Tratado como ausente |
| Consistência | Partos anteriores maiores que gestações anteriores (K1) | 381 | Mantido e marcado como suspeito |
| Consistência | Número de filhos menor que partos anteriores (K2) | 156 | Mantido e marcado como suspeito |
| Consistência | Zero consultas com mês de início informado (K3) | 10 | Sem efeito no modelo (variável não usada) |
| Consistência | Registros duplicados exatos | 15 | Excluído |
| Tempestividade | Data fora do ano de referência | 0 | Sem ação |

Os registros suspeitos (K1 e K2) estão em `saida/08_registros_suspeitos.csv`, com o número
da linha no CSV original. Depois da supressão, restam 222 deles no arquivo tratado,
marcados na coluna `registro_suspeito`. Eles só devem ser excluídos depois que a equipe
confirmar as definições dos campos `QTDGESTANT`, `QTDPARTNOR`, `QTDPARTCES`, `QTDFILVIVO`
e `QTDFILMORT` no dicionário oficial. Pode ser que a definição de "gestação anterior" não
inclua gestações sem parto, por exemplo.

Os registros com peso entre 300 e 6.500 g não foram excluídos, porque valores muito baixos
podem ser casos reais de baixo peso extremo. Os 11 casos estão listados em
`saida/07_peso_para_revisao.csv`. Os limites de 300 g e 6.500 g são nossa suposição e
precisam ser confirmados com o dicionário oficial.

Resultado: 175.817 registros de entrada e 174.415 de saída. Prevalência de baixo peso no
arquivo tratado: 9,31%. Todas as categorias do modelo são válidas após o tratamento.

Regras de entrada da API: a API valida cada campo contra `06_contrato_entrada_api.json` e
rejeita valores fora das categorias, com mensagem clara.

Tempestividade: os dados são de 2014, cerca de 12 anos antes da data de referência
(novembro de 2026). Isso é uma limitação e deve ser declarada no README e na apresentação.

## 6. Papéis e responsabilidades (requisito 6.5)

Sugestão para o sistema em uso real. Deve ser adaptada pelo grupo:
- Data Owner (dono do dado e da decisão): Secretaria Municipal da Saúde de São Paulo, pela
  coordenação de atenção básica. Aprova a finalidade e os critérios de risco.
- Data Steward (guardião da qualidade): equipe de vigilância em saúde e dados. Mantém o
  dicionário de dados, executa as regras de qualidade e aprova mudanças nos campos.
- Data Custodian (custódia técnica): equipe de TI da secretaria ou do provedor de nuvem.
  Guarda o banco, aplica criptografia, backups e controles de acesso.
- Controlador (LGPD, art. 5º, VI): Secretaria Municipal da Saúde, que decide a finalidade.
- Operador (art. 5º, VII): equipe de desenvolvimento e provedor de hospedagem, que tratam
  os dados em nome do controlador, sob contrato com cláusulas de segurança.
- Encarregado (art. 5º, VIII): pessoa designada pelo controlador como canal com os titulares
  e com a ANPD.

No trabalho acadêmico, o grupo simula esses papéis. A declaração deve deixar isso claro.

## 7. Matriz de papéis × recursos e controles (requisito 6.6)

| Recurso | Operador | Gestor/Auditor | Administrador (Custodian) |
|---|---|---|---|
| Realizar predição | Sim | Não | Não |
| Consultar histórico próprio | Sim | Não | Não |
| Consultar histórico geral | Não | Sim | Não |
| Acessar auditoria e pedidos de revisão | Não | Sim | Não |
| Ver documentação do modelo | Versão ao usuário | Completa | Completa |
| Dados brutos e tratados | Não | Não | Sim (somente leitura) |
| Gerenciar usuários e perfis | Não | Não | Sim |

Controles de segurança (propostos; a implementação é responsabilidade do back-end):
- Criptografia em trânsito (HTTPS/TLS) e em repouso no banco de dados.
- Senhas com BCrypt; tokens JWT com expiração e refresh token.
- Registro de acessos e operações (tabela de auditoria com usuário, operação e data/hora).
- Prevenção de vazamento (DLP): dados brutos não são versionados (`.gitignore`); exportações
  CSV/PDF só trazem registros que o usuário pode ver; a API não expõe campos de outros pacientes.
- Permissões verificadas no back-end, nunca só na interface.

## 8. Direitos do titular e retenção (intermediário)

- Revisão de decisão automatizada (LGPD, art. 20): o botão "Solicitar revisão humana" abre
  um pedido analisado pelo gestor/auditor, com prazo e resultado registrados.
- Retenção do histórico de predições: prazo a definir pelo controlador, justificado pela
  finalidade (acompanhamento da decisão e auditoria) e pelas regras institucionais. O grupo
  propõe um prazo para discussão, por exemplo cinco anos, mas esse valor não é obrigação
  legal e depende de aprovação. Depois do prazo, os registros devem ser excluídos ou
  anonimizados.

## 9. Pendências e limitações

- Dicionário oficial não veio no arquivo. As descrições do catálogo seguem o layout padrão
  do SINASC e devem ser conferidas, sobretudo `RETROALIM` e `STDNNOVA`.
- Os limites de peso (300 g e 6.500 g) e a definição de gestação anterior precisam de
  confirmação no dicionário oficial antes de qualquer exclusão adicional.
- Contrato da API: alinhar com a equipe de IA quais campos aceitam "ignorado" e como o
  pipeline trata esse valor. A proposta está no contrato, mas não foi implementada.
- O anexo do trabalho fala em arquivos de 2015 a 2025, e o arquivo recebido é de 2014.
  Confirmar com a professora.
- Base de 2014: o modelo reflete padrões de anos atrás e precisa de atualização.
- O modelo não tem validação externa.

## 10. Declaração de uso de IA generativa

Preencher no README do grupo: ferramenta usada, tarefa e revisão humana, conforme a
Resolução nº 005/2026 (CONSUP) da UNIFSA.
