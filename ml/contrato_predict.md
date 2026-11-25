# Contrato do serviço de predição — `POST /predict`

Responsável: João Pedro Lima (revisão: Marcus Mikael).

**Status: provisório (v0.2).** Enquanto o serviço mock não for publicado, este documento é a fonte da verdade.

O back-end (Spring Boot) e o front-end (Streamlit) seguem este contrato. Caso alguma especificação seja alterada, a equipe deve ser avisada.

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| POST | `/predict` | Recebe os dados do pré-natal e devolve a predição |
| GET | `/health` | Verifica se o serviço está funcionando |
| GET | `/model/version` | Retorna a versão do modelo em uso |

## Entrada

### Exemplo de requisição

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

### Campos e valores válidos

| Campo | Obrigatório | Valores válidos | Observação |
|---|---|---|---|
| `FxEtPartur` | Não | `10-14`, `15-19`, `20-24`, `25-29`, `30-34`, `35-39`, `40-44`, `45-49`, `>=50` ou `null` | Texto exato, utilizando hífen (`-`), nunca travessão (`–`). `null` representa valor ignorado |
| `ESTCIVMAE` | Não | 1 a 5, ou `null` | `null` ou `9` são tratados como "Ignorado" |
| `ESCMAE2010` | Não | 0 a 5, ou `null` | `null` ou `9` são tratados como "Ignorado" |
| `GRAVIDEZ` | Não | 1 a 3, ou `null` | `null` ou `9` são tratados como "Ignorado" |
| `QTDGESTANT` | Sim | Inteiro de 0 a 15 | Quantidade de gestações anteriores |
| `QTDPARTNOR` | Sim | Inteiro de 0 a 15 | Quantidade de partos vaginais anteriores |
| `QTDPARTCES` | Sim | Inteiro de 0 a 15 | Quantidade de partos cesáreos anteriores |
| `QTDFILVIVO` | Sim | Inteiro de 0 a 15 | Quantidade de filhos vivos |
| `QTDFILMORT` | Sim | Inteiro de 0 a 15 | Quantidade de perdas fetais |
| `MESPRENAT` | Não | Inteiro de 1 a 9, ou `null` | `null` representa pré-natal não iniciado ou não informado. Os dois casos são tratados pelo indicador `MESPRENAT_AUSENTE`. O valor `9` representa o nono mês, não um valor ignorado |

### Regras de validação

- Nos campos `ESTCIVMAE`, `ESCMAE2010` e `GRAVIDEZ`, os valores `null` e `9` são convertidos para "Ignorado" dentro do serviço.
- O texto `Ign` não é aceito como valor. O cliente deve enviar `null`.
- Valores fora dos intervalos ou das categorias permitidas são rejeitados com HTTP 422, sem correção silenciosa.
- Campos extras, como `PESO`, são rejeitados. O serviço aceita somente os 10 atributos definidos neste contrato.
- O indicador `MESPRENAT_AUSENTE` é criado dentro do pipeline e não faz parte da entrada da API.
- O limite máximo de 15 para as colunas de contagem segue a validação atual do front-end. Esse limite ainda deve ser confirmado com o Marcos André, pois a revisão dos dados identificou 8 registros com 20 ou mais partos.
- A regra de consistência entre `QTDPARTNOR`, `QTDPARTCES` e `QTDGESTANT` permanece pendente de validação com a equipe de dados antes de ser aplicada como restrição definitiva.

## Saída (HTTP 200)

Os valores abaixo são **ilustrativos** e não representam resultados reais do modelo.

### Exemplo de resposta

```json
{
  "probabilidade": 0.62,
  "faixa_risco": "ALTO",
  "fatores": [
    {
      "atributo": "GRAVIDEZ",
      "valor": 2,
      "impacto": 0.41,
      "direcao": "AUMENTA"
    },
    {
      "atributo": "MESPRENAT",
      "valor": null,
      "impacto": 0.12,
      "direcao": "AUMENTA"
    },
    {
      "atributo": "FxEtPartur",
      "valor": "25-29",
      "impacto": 0.05,
      "direcao": "REDUZ"
    }
  ],
  "versao_modelo": "0.0.0-mock"
}
```

### Campos da resposta

| Campo | Tipo | Descrição |
|---|---|---|
| `probabilidade` | Número entre 0 e 1 | Probabilidade estimada de baixo peso ao nascer (menos de 2.500 g) |
| `faixa_risco` | `BAIXO`, `MEDIO` ou `ALTO` | Classificação de risco, sem acento, para facilitar o uso como enum no Java |
| `fatores` | Lista | Os 3 principais fatores explicativos, ordenados do maior para o menor impacto |
| `fatores[].atributo` | Texto | Atributo original do SINASC. Não devem ser retornadas colunas do One-Hot Encoding, como `FxEtPartur_25-29` |
| `fatores[].valor` | Texto, número ou `null` | Valor do atributo no caso analisado |
| `fatores[].impacto` | Número | Valor absoluto da soma dos valores SHAP das colunas derivadas do atributo original |
| `fatores[].direcao` | `AUMENTA` ou `REDUZ` | Indica se a contribuição do atributo aumenta ou reduz a previsão de risco |
| `versao_modelo` | Texto | Versão do modelo utilizado na predição |

### Explicabilidade com SHAP

Os fatores explicativos devem ser apresentados por atributo original do SINASC.

Quando uma variável categórica for transformada em várias colunas pelo One-Hot Encoding, os valores SHAP dessas colunas devem ser somados para representar o atributo original.

A direção será definida pelo sinal da contribuição somada:

- `AUMENTA`: contribuição positiva para o risco previsto.
- `REDUZ`: contribuição negativa para o risco previsto.

O serviço deve retornar os 3 principais fatores, ordenados pelo valor absoluto do impacto.

O back-end será responsável por transformar os campos `atributo` e `direcao` em textos compreensíveis para o usuário no front-end.

Os fatores SHAP representam contribuições para a previsão do modelo e não devem ser interpretados como relações de causa e efeito.

## Faixas de risco

**Faixas provisórias.** Serão utilizadas até a avaliação do modelo prevista para 27/11/2026. Após a avaliação, os limites deverão ser revisados com base nos resultados e nos critérios definidos pela equipe.

| Faixa | Probabilidade |
|---|---|
| `BAIXO` | Menor que 0,15 |
| `MEDIO` | De 0,15 até menor que 0,30 |
| `ALTO` | 0,30 ou maior |

O limiar de decisão do modelo ainda não está definido.

## Erros

Entradas inválidas devem retornar HTTP 422.

Exemplo de resposta de erro no formato padrão do FastAPI:

```json
{
  "detail": [
    {
      "type": "less_than_equal",
      "loc": ["body", "MESPRENAT"],
      "msg": "Input should be less than or equal to 9"
    }
  ]
}
```

O back-end deve traduzir esse erro para o formato padronizado de erros utilizado pela aplicação.

## Versão provisória (mock)

Enquanto o modelo treinado não estiver disponível, o serviço poderá utilizar respostas simuladas apenas para permitir a integração entre as aplicações.

A versão `0.0.0-mock` identifica essas respostas.

| Entrada | Resposta do mock |
|---|---|
| `MESPRENAT` é `null` | `ALTO` (probabilidade 0,45) |
| `GRAVIDEZ` é 2 ou 3 | `MEDIO` (probabilidade 0,20) |
| Qualquer outra entrada válida | `BAIXO` (probabilidade 0,08) |

Essas respostas são **inventadas para fins de integração** e não representam o desempenho ou as previsões do modelo real.

O mock não deve ser utilizado no deploy final.

## Como rodar

O serviço mock ainda está em desenvolvimento e será disponibilizado em outro Pull Request.

Quando estiver publicado, as instruções de instalação e execução serão adicionadas aqui.
