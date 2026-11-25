# frontend — Aplicação web (Streamlit)

Responsável: Antonio.

O front conversa **só com a API Spring Boot**: nunca chama o serviço do modelo nem o banco diretamente. O controle de acesso é feito pelo back-end; o front só esconde as telas que o perfil não usa.

## Telas

| Tela | Arquivo | Perfil | Conteúdo |
|------|---------|--------|----------|
| Login | `paginas/login.py` | público | E-mail e senha (`POST /api/auth/login`) |
| Nova predição | `paginas/predicao.py` | operador | Formulário dos 10 atributos, com validação |
| Resultado | `paginas/resultado.py` | operador | Faixa de risco em linguagem do usuário, chance estimada, principais fatores, aviso de apoio à decisão e botão **Solicitar revisão humana** |
| Auditoria | `paginas/auditoria.py` | gestor/auditor | Histórico com filtros por período, usuário e faixa de risco, e pedidos de revisão |
| Sobre o modelo | `paginas/transparencia.py` | todos | Model card para o usuário (provisório até a avaliação final) |
| Privacidade | `paginas/privacidade.py` | todos | Dados coletados, finalidade, retenção e quem acessa (provisório até o documento de governança) |

Outros arquivos: `app.py` (entrada e menu por perfil), `api.py` (cliente da API), `campos.py` (os 10 atributos, rótulos e validação), `sessao.py` (token e usuário logado), `mock.py` (respostas do modo demonstração).

## Como executar

Pré-requisito: Python 3.12 ou mais recente. Na pasta `frontend/`, no PowerShell:

```powershell
python -m venv .venv
.venv\Scripts\Activate.ps1
pip install -r requirements-dev.txt
```

Com a API rodando em `http://localhost:8080`:

```powershell
streamlit run app.py
```

Sem a API (modo demonstração, com resultados fictícios e um aviso no topo da tela):

```powershell
$env:API_MOCK = "1"; streamlit run app.py
```

No modo demonstração qualquer senha entra; e-mail começando com `gestor` entra como gestor/auditor.

Testes (rodam em modo demonstração, sem API):

```powershell
python -m pytest
```

### Variáveis de ambiente

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `API_URL` | `http://localhost:8080` | Endereço da API Spring Boot |
| `API_TIMEOUT` | `15` | Espera máxima por resposta, em segundos |
| `API_MOCK` | `0` | `1` liga o modo demonstração (não chama a API) |
| `PORT` | `8501` | Porta do Streamlit no container |

### Docker

```powershell
docker build -t sinasc-frontend .
docker run -p 8501:8501 -e API_URL=http://host.docker.internal:8080 sinasc-frontend
```

A hospedagem precisa aceitar **WebSocket** (o Streamlit depende disso).

## Contrato proposto com o back-end (rotas de predição)

O login (`POST /api/auth/login`) e a lista de usuários usada no filtro da Auditoria (`GET /api/usuarios`) já existem. As rotas abaixo são a **proposta** do front para o Carlos e o JP; o front já usa esse formato no modo demonstração. Se o formato final mudar, o ajuste fica em `api.py` e `campos.py`.

Os campos chegam em camelCase; o back-end converte para as colunas do SINASC antes de chamar o serviço do modelo (o mapeamento está em `campos.py`). Campo categórico não informado vai como `null`. `mesInicioPrenatal: null` significa **pré-natal não iniciado**. Faixa etária usa hífen (`25-29`).

### `POST /api/predicoes` (operador)

```json
{
  "faixaEtaria": "25-29",
  "estadoCivil": 5,
  "escolaridade": 3,
  "tipoGravidez": 1,
  "gestacoesAnteriores": 1,
  "partosNormais": 1,
  "partosCesareos": 0,
  "filhosVivos": 1,
  "perdasFetais": 0,
  "mesInicioPrenatal": 2
}
```

| Campo da API | Coluna SINASC | Valores |
|--------------|---------------|---------|
| `faixaEtaria` | `FxEtPartur` | `10-14` … `45-49`, `>=50` ou `null` |
| `estadoCivil` | `ESTCIVMAE` | 1 a 5 ou `null` |
| `escolaridade` | `ESCMAE2010` | 0 a 5 ou `null` |
| `tipoGravidez` | `GRAVIDEZ` | 1 a 3 ou `null` |
| `gestacoesAnteriores` | `QTDGESTANT` | 0 a 15 |
| `partosNormais` | `QTDPARTNOR` | 0 a 15 |
| `partosCesareos` | `QTDPARTCES` | 0 a 15 |
| `filhosVivos` | `QTDFILVIVO` | 0 a 15 |
| `perdasFetais` | `QTDFILMORT` | 0 a 15 |
| `mesInicioPrenatal` | `MESPRENAT` | 1 a 9 ou `null` (não iniciado) |

Regra de consistência validada no front (e que deve valer também na API): `partosNormais + partosCesareos <= gestacoesAnteriores`.

Resposta `201`:

```json
{
  "id": 15,
  "probabilidade": 0.62,
  "faixaRisco": "ALTO",
  "fatores": [
    {"atributo": "tipoGravidez", "descricao": "Gravidez dupla (gêmeos)", "efeito": "AUMENTA", "impacto": 0.41}
  ],
  "versaoModelo": "1.0.0",
  "criadoEm": "2026-11-25T14:03:00Z",
  "revisaoSolicitada": false
}
```

`faixaRisco` é `BAIXO`, `MEDIO` ou `ALTO`. `efeito` é `AUMENTA` ou `REDUZ`. `descricao` é o texto mostrado ao usuário.

### `POST /api/predicoes/{id}/revisao` (operador)

Corpo `{"motivo": "texto opcional"}`. Resposta `201`. Marca `revisaoSolicitada = true` na predição.

### `GET /api/predicoes` (gestor/auditor)

Parâmetros opcionais: `inicio` e `fim` (datas `AAAA-MM-DD`, **no horário de Brasília**), `usuarioId`, `faixa` (`BAIXO`, `MEDIO`, `ALTO`), `page`, `size`, `sort=criadoEm,desc`. Resposta: página do Spring (`content`, `totalElements`, ...), com cada item igual à resposta do `POST` mais `usuarioId` e `usuarioNome`.

## Uso de IA generativa

O código do front foi gerado com apoio de IA. Registrar a ferramenta e a tarefa na seção 9 do [README](../README.md).
