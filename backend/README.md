# Backend — API REST (Java + Spring Boot)

Responsáveis: Rodolfo Dheymison (segurança e usuários), Carlos Arthur (domínio e predição).

## Escopo básico (obrigatório)

- API REST em camadas (controladores, serviços, repositórios), com GET, POST, PUT e DELETE e códigos HTTP corretos.
- Banco relacional com ao menos 3 entidades relacionadas: usuários, registros analisados e histórico de predições.
- CRUD dos registros de análise, consultas individuais e listagens.
- Autenticação por e-mail e senha com JWT, senhas com BCrypt.
- Perfis `operador` e `gestor/auditor`, selecionados no back-end.
- Cada previsão gera um novo registro (registro, usuário, resultado, probabilidade, faixa, fatores, versão do modelo, data/hora), sem sobrescrever o histórico.
- Validação de entrada (inclusive as regras de qualidade da governança) e erros padronizados.
- Documentação Swagger/OpenAPI.

## Integração com o modelo

O back-end chama o serviço Python em `MODEL_SERVICE_URL` (`POST /predict`). Contrato em `../ml/README.md`.

## Como executar

### Pré-requisitos

- Java 21 (JDK)
- Maven 3.9+ (`brew install maven` no macOS)
- Docker (para o PostgreSQL 16) — ou um PostgreSQL local

### 1. Variáveis de ambiente

Copie `.env.example` para `.env` (não vai para o Git) e preencha. O `.env.example` traz `JWT_SECRET` e `ADMIN_PASSWORD` vazios de propósito: valores de exemplo são públicos e permitiriam forjar tokens ou entrar como gestor. No deploy, use valores gerados de verdade.

| Variável | Obrigatória | Descrição |
|---|---|---|
| `JWT_SECRET` | **sim** | Segredo do JWT. Gere com `openssl rand -hex 32`. A API não sobe se estiver vazio, com menos de 32 caracteres ou com valor de exemplo |
| `DB_URL` | não | Padrão `jdbc:postgresql://localhost:5432/sinasc` |
| `DB_USER` / `DB_PASSWORD` | não | Padrão `sinasc` / `sinasc` |
| `JWT_EXPIRATION_MINUTES` | não | Padrão `120` |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | não | Cria o primeiro gestor/auditor (senha de 8 a 72 caracteres, de verdade: valores de exemplo são ignorados). **Sem isso ninguém cria contas** |
| `CORS_ORIGINS` | não | Origens do front, separadas por vírgula |
| `MODEL_SERVICE_URL` | não | Padrão `http://localhost:8000` |

### 2. Banco de dados

```bash
cd backend
docker compose -f docker-compose.dev.yml up -d
```

As tabelas são criadas sozinhas pelo Flyway (`src/main/resources/db/migration`) na primeira execução.

### 3. Subir a API

```bash
cd backend
set -a; source .env; set +a      # carrega as variáveis (macOS/Linux)
mvn spring-boot:run
```

- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- Testes: `mvn test`

### Com Docker (tudo junto)

O `docker-compose.yml` da raiz já prevê o serviço `api` (`build: ./backend`). O `Dockerfile` está em `backend/Dockerfile`. Para o serviço `api` funcionar, passe também `ADMIN_EMAIL`, `ADMIN_PASSWORD` e `CORS_ORIGINS` no `environment`, além de `DB_URL`, `MODEL_SERVICE_URL` e `JWT_SECRET`.

## Autenticação e perfis

1. `POST /api/auth/login` com `{ "email", "senha" }` devolve um token.
2. Nas demais rotas, envie `Authorization: Bearer <token>`.
3. No Swagger, use o botão **Authorize** e cole o token.

Não existe cadastro público: **só um gestor/auditor cria contas** (`POST /api/usuarios`), escolhendo o perfil. O primeiro gestor vem do `ADMIN_EMAIL`/`ADMIN_PASSWORD`.

| Perfil | Como é criado |
|---|---|
| `OPERADOR` | Por um gestor/auditor (`POST /api/usuarios`) |
| `GESTOR_AUDITOR` | Por outro gestor ou pelo gestor inicial (`ADMIN_EMAIL`) |

O perfil é lido do banco a cada requisição: mudar o perfil ou desativar uma conta vale na hora, sem esperar o token expirar.

## Endpoints (segurança e usuários)

| Método e rota | Quem acessa | Resposta |
|---|---|---|
| `POST /api/auth/login` | público | 200 (token) / 401 |
| `GET /api/usuarios/eu` | qualquer perfil | 200 |
| `GET /api/usuarios` | gestor/auditor | 200 (paginado) |
| `GET /api/usuarios/{id}` | gestor/auditor | 200 / 404 |
| `POST /api/usuarios` | gestor/auditor | 201 / 409 / 422 |
| `PUT /api/usuarios/{id}` | gestor/auditor | 200 / 404 / 409 / 422 |
| `PUT /api/usuarios/{id}/perfil` | gestor/auditor | 200 / 404 / 409 |
| `DELETE /api/usuarios/{id}` | gestor/auditor | 204 (desativa a conta) |
| `GET /api/auditoria/acessos?usuarioId=&acao=` | gestor/auditor | 200 (paginado) |

Regras: ninguém desativa nem altera o perfil da própria conta; e-mail é único sem diferenciar maiúsculas; senha de 8 a 72 caracteres; campos desconhecidos no JSON geram 400.

No `PUT /api/usuarios/{id}`, para **não** trocar a senha, omita o campo `senha` (ou envie `null`). String vazia (`""`) ou só espaços retorna 422.

## Registro de acessos

Ficam registrados: login bem-sucedido (`LOGIN`), **falhas de login** (`LOGIN_FALHA`) e as ações de gestão de usuários. Nas falhas, o motivo real fica só na auditoria (`auth/email-desconhecido`, `auth/senha-incorreta` ou `auth/conta-inativa`); quem tenta logar recebe sempre a mesma resposta, 401. Se o e-mail não existe, o registro guarda o e-mail informado em `identificador`. A senha digitada nunca é gravada.

Para ver as tentativas que falharam: `GET /api/auditoria/acessos?acao=LOGIN_FALHA`.

## Erros padronizados

Formato RFC 7807 (`application/problem+json`): `title`, `status`, `detail`. Falhas de validação voltam com **422** e uma lista `erros` (`campo`, `mensagem`). Códigos usados: 400, 401, 403, 404, 409, 422, 500.

## Para quem integra (Carlos)

- **Usuário logado:** `@AuthenticationPrincipal AuthUser logado` no método do controller (`logado.id()`, `logado.perfil()`).
- **Perfis nas rotas:** `@PreAuthorize(Permissoes.OPERADOR)` ou `@PreAuthorize(Permissoes.GESTOR_AUDITOR)`. A matriz de acesso fica em `security/Permissoes.java`.
- **Auditoria:** `auditoriaService.registrar(logado.id(), "PREDICAO_CRIAR", "predicao/" + id)`. Sem dados pessoais no texto.
- **Erros:** use `RecursoNaoEncontradoException` (404) e `ConflitoException` (409). Para erros novos (ex.: modelo fora do ar, 503), adicione um método em `exception/GlobalExceptionHandler.java`.
- **Banco:** suas migrations começam em `V3__...sql` (a `V1` cria `usuario` e `log_acesso`; a `V2` ajusta a auditoria de falhas de login). Use `usuario_id BIGINT REFERENCES usuario(id)` nas suas tabelas.
- **Pacotes:** cada um trabalha nos seus (`registro/`, `predicao/` para o Carlos), para evitar conflitos no Git.

## Decisões provisórias (aguardam a equipe)

- A matriz de acesso da governança (seção 6) ainda está em `TODO`; os perfis seguem o que foi combinado e ficam num ponto único (`Permissoes`).
- O cadastro público foi removido: só o gestor/auditor cria contas. A matriz de papéis da governança (seção 6) deve refletir isso. Se o grupo decidir reabrir o cadastro, é preciso recriar a rota e liberá-la no `SecurityConfig`.
- Ainda não há limite de tentativas de login (bloqueio ou atraso após várias falhas). As falhas só são registradas para auditoria.
- Exclusão de usuário é lógica (desativação), porque predições e auditoria referenciam o usuário.

## Uso de IA generativa

Registrar a ferramenta e a tarefa na seção 9 do [README](../README.md) da raiz.
