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

Copie `.env.example` para `.env` (não vai para o Git) e ajuste.

| Variável | Obrigatória | Descrição |
|---|---|---|
| `JWT_SECRET` | **sim** | Segredo do JWT, mínimo 32 caracteres |
| `DB_URL` | não | Padrão `jdbc:postgresql://localhost:5432/sinasc` |
| `DB_USER` / `DB_PASSWORD` | não | Padrão `sinasc` / `sinasc` |
| `JWT_EXPIRATION_MINUTES` | não | Padrão `120` |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | não | Cria o primeiro gestor/auditor (senha de 8 a 72 caracteres) |
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

| Perfil | Como é criado |
|---|---|
| `OPERADOR` | Cadastro público (`POST /api/auth/cadastro`) ou por um gestor |
| `GESTOR_AUDITOR` | Só por outro gestor (`POST /api/usuarios`) ou pelo gestor inicial (`ADMIN_EMAIL`) |

O perfil é lido do banco a cada requisição: mudar o perfil ou desativar uma conta vale na hora, sem esperar o token expirar.

## Endpoints (segurança e usuários)

| Método e rota | Quem acessa | Resposta |
|---|---|---|
| `POST /api/auth/login` | público | 200 (token) / 401 |
| `POST /api/auth/cadastro` | público | 201 / 409 / 422 |
| `GET /api/usuarios/eu` | qualquer perfil | 200 |
| `GET /api/usuarios` | gestor/auditor | 200 (paginado) |
| `GET /api/usuarios/{id}` | gestor/auditor | 200 / 404 |
| `POST /api/usuarios` | gestor/auditor | 201 / 409 / 422 |
| `PUT /api/usuarios/{id}` | gestor/auditor | 200 / 404 / 409 |
| `PUT /api/usuarios/{id}/perfil` | gestor/auditor | 200 / 404 / 409 |
| `DELETE /api/usuarios/{id}` | gestor/auditor | 204 (desativa a conta) |
| `GET /api/auditoria/acessos?usuarioId=` | gestor/auditor | 200 (paginado) |

Regras: ninguém desativa nem altera o perfil da própria conta; e-mail é único sem diferenciar maiúsculas; senha de 8 a 72 caracteres; campos desconhecidos no JSON geram 400.

## Erros padronizados

Formato RFC 7807 (`application/problem+json`): `title`, `status`, `detail`. Falhas de validação voltam com **422** e uma lista `erros` (`campo`, `mensagem`). Códigos usados: 400, 401, 403, 404, 409, 422, 500.

## Para quem integra (Carlos)

- **Usuário logado:** `@AuthenticationPrincipal AuthUser logado` no método do controller (`logado.id()`, `logado.perfil()`).
- **Perfis nas rotas:** `@PreAuthorize(Permissoes.OPERADOR)` ou `@PreAuthorize(Permissoes.GESTOR_AUDITOR)`. A matriz de acesso fica em `security/Permissoes.java`.
- **Auditoria:** `auditoriaService.registrar(logado.id(), "PREDICAO_CRIAR", "predicao/" + id)`. Sem dados pessoais no texto.
- **Erros:** use `RecursoNaoEncontradoException` (404) e `ConflitoException` (409). Para erros novos (ex.: modelo fora do ar, 503), adicione um método em `exception/GlobalExceptionHandler.java`.
- **Banco:** suas migrations começam em `V2__...sql` (a `V1` já tem `usuario` e `log_acesso`). Use `usuario_id BIGINT REFERENCES usuario(id)` nas suas tabelas.
- **Pacotes:** cada um trabalha nos seus (`registro/`, `predicao/` para o Carlos), para evitar conflitos no Git.

## Decisões provisórias (aguardam a equipe)

- A matriz de acesso da governança (seção 6) ainda está em `TODO`; os perfis seguem o que foi combinado e ficam num ponto único (`Permissoes`).
- O cadastro público cria `OPERADOR`. Se a equipe preferir que só gestores criem contas, basta remover `/api/auth/cadastro` do `SecurityConfig`.
- Exclusão de usuário é lógica (desativação), porque predições e auditoria referenciam o usuário.

## Uso de IA generativa

Registrar a ferramenta e a tarefa na seção 9 do [README](../README.md) da raiz.
