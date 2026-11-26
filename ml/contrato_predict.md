# Contrato do serviço de predição — `POST /predict`

Responsável: João Pedro Lima

Status: Em revisão

O back-end (Spring Boot) segue este contrato. O front-end (Streamlit) se comunica exclusivamente com o back-end, utilizando os campos em camelCase. Mudou algo aqui, avise o grupo.

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| POST | `/predict` | Recebe os dados do pré-natal e devolve a predição |
| GET | `/health` | Verifica se o serviço está de pé |
| GET | `/model/version` | Versão do modelo em uso |

## Entrada

```json
{
  "FxEtPartur": "25-29",
  "ESTCIVMAE": 5,
  "ESCMAE2010": 3,
  "GRAVIDEZ": 1,
  "QTDGESTANT": 1,
  "QTDPARTNOR": 1,
  "QTDPARTCES": 0,
  "QTDFILVIVO": 1,
  "QTDFILMORT": 0,
  "MESPRENAT": 2
}
```

| Campo | Obrigatório | Valores válidos | Observação |
|---|---|---|---|
| `FxEtPartur` | não | `10-14`, `15-19`, `20-24`, `25-29`, `30-34`, `35-39`, `40-44`, `45-49`, `>=50` ou `null` | Texto exato, com **hífen** (`-`), nunca travessão (`–`). `null` = ignorado |
| `ESTCIVMAE` | não | 1 a 5, 9 ou `null` | `null` ou `9` = ignorado |
| `ESCMAE2010` | não | 0 a 5, 9 ou `null` | `null` ou `9` = ignorado |
| `GRAVIDEZ` | não | 1 a 3, 9 ou `null` | `null` ou `9` = ignorado |
| `QTDGESTANT` | sim | inteiro de 0 a 15 | Gestações anteriores |
| `QTDPARTNOR` | sim | inteiro de 0 a 15 | Partos vaginais anteriores |
| `QTDPARTCES` | sim | inteiro de 0 a 15 | Partos cesáreos anteriores |
| `QTDFILVIVO` | sim | inteiro de 0 a 15 | Filhos vivos |
| `QTDFILMORT` | sim | inteiro de 0 a 15 | Perdas fetais |
| `MESPRENAT` | não | 1 a 9 ou `null` | `null` = pré-natal **não iniciado ou não informado** (os dois casos são tratados do mesmo jeito, pelo `MESPRENAT_AUSENTE`). O `9` é o nono mês, e não "ignorado" |

Regras:

- Nas colunas `ESTCIVMAE`, `ESCMAE2010` e `GRAVIDEZ`, `null` e `9` viram "Ignorado" dentro do serviço.
- O texto `Ign` **não** é aceito como valor: o cliente envia `null`.
- Valores fora da lista (por exemplo `MESPRENAT` 10 ou 99) são **rejeitados com HTTP 422**, e não corrigidos em silêncio.
- Consistência: `QTDPARTNOR + QTDPARTCES` não pode ser maior que `QTDGESTANT` (HTTP 422). O front já valida isso, e a API também deve validar.
- Campos extras (por exemplo `PESO`) são rejeitados: o serviço só aceita os 10 atributos.
- O indicador `MESPRENAT_AUSENTE` é criado dentro do pipeline; **não** faz parte da entrada.
- O máximo 15 das colunas `QTD*` segue o front. A confirmar com o Marcos André nas regras de qualidade (a revisão achou 8 registros com 20 ou mais partos).

## Saída (HTTP 200)

Os valores abaixo são **ilustrativos**, não são resultado do modelo.

```json
{
  "probabilidade": 0.62,
  "faixa_risco": "ALTO",
  "fatores": [
    {"atributo": "GRAVIDEZ", "valor": 2, "impacto": 0.41, "direcao": "AUMENTA"},
    {"atributo": "MESPRENAT", "valor": null, "impacto": 0.12, "direcao": "AUMENTA"},
    {"atributo": "FxEtPartur", "valor": "25-29", "impacto": 0.05, "direcao": "REDUZ"}
  ],
  "versao_modelo": "0.0.0-mock"
}
```

| Campo | Tipo | Descrição |
|---|---|---|
| `probabilidade` | número de 0 a 1 | Probabilidade de baixo peso (< 2.500 g) |
| `faixa_risco` | `BAIXO`, `MEDIO` ou `ALTO` | Sem acento, para virar enum no Java |
| `fatores` | lista | Os **3 principais** fatores da explicação (SHAP), do maior impacto para o menor |
| `fatores[].atributo` | texto | Coluna **original** do SINASC (nunca colunas do one-hot, como `FxEtPartur_25-29`) |
| `fatores[].valor` | texto, número ou `null` | Valor do atributo no caso analisado |
| `fatores[].impacto` | número | Valor absoluto da soma do SHAP das colunas derivadas do atributo. **Unidade provisória**, veja abaixo |
| `fatores[].direcao` | `AUMENTA` ou `REDUZ` | Efeito sobre o risco (sinal da soma do SHAP) |
| `versao_modelo` | texto | Versão do modelo usado (o back-end grava no histórico) |

O back-end transforma `direcao` e `atributo` no texto em linguagem do usuário que o front mostra.

Unidade do `impacto`: depende do modelo final e do explainer (log-odds para regressão logística ou boosting; probabilidade para Random Forest com `TreeExplainer`). Será fixada e documentada aqui quando o modelo estiver escolhido. Até lá, o back-end **não deve** exibir o `impacto` como percentual: use-o só para ordenar os fatores e, junto com a `direcao`, indicar o efeito.

Os fatores do SHAP mostram a contribuição de cada atributo para a previsão do modelo e não indicam relação de causa e efeito.

## Faixas de risco

**Provisórias.** Valem até a avaliação do modelo (sexta, 27/11), quando serão trocadas pelos cortes definidos com base nos dados.

| Faixa | Probabilidade |
|---|---|
| `BAIXO` | menor que 0,15 |
| `MEDIO` | de 0,15 até menos de 0,30 |
| `ALTO` | 0,30 ou mais |

O limiar de decisão ainda não está definido.

## Erros

Entrada inválida devolve **HTTP 422** no formato padrão do FastAPI:

```json
{"detail": [{"type": "less_than_equal", "loc": ["body", "MESPRENAT"], "msg": "Input should be less than or equal to 9"}]}
```

O back-end deve traduzir esse erro para o seu formato padronizado de erros.

## Versão provisória (mock)

Enquanto o modelo não existe, o serviço devolve respostas **inventadas**, só para integrar. A versão `0.0.0-mock` identifica isso:

| Entrada | Resposta do mock |
|---|---|
| `MESPRENAT` é `null` | `ALTO` (probabilidade 0,45) |
| `GRAVIDEZ` 2 ou 3 | `MEDIO` (probabilidade 0,20) |
| qualquer outra | `BAIXO` (probabilidade 0,08) |

As regras são avaliadas na ordem da tabela e **vale a primeira que se aplicar**. Exemplo: `MESPRENAT` nulo e `GRAVIDEZ` 2 ao mesmo tempo resultam em `ALTO`.

Isso **não** representa o modelo e não pode ir para o deploy final.

## Como rodar

Ainda não disponível: o serviço mock está em desenvolvimento e será publicado em outro PR.
