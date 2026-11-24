# backend — API REST (Java + Spring Boot)

Responsáveis: Rodolfo Dheymison (segurança e usuários), Carlos Arthur (domínio e predição).

## Escopo básico (obrigatório)

- API REST em camadas (controllers, services, repositories), com GET, POST, PUT e DELETE e códigos HTTP corretos.
- Banco relacional com ao menos 3 entidades relacionadas: usuários, registros analisados e histórico de predições.
- CRUD dos registros de análise, consultas individuais e listagens.
- Autenticação por e-mail e senha com JWT, senhas com BCrypt.
- Perfis `operador` e `gestor/auditor`, verificados no back-end.
- Cada predição gera um novo registro (registro, usuário, resultado, probabilidade, faixa, fatores, versão do modelo, data/hora), sem sobrescrever o histórico.
- Validação de entrada (inclusive as regras de qualidade da governança) e erros padronizados.
- Documentação Swagger/OpenAPI.

## Integração com o modelo

O back-end chama o serviço Python em `MODEL_SERVICE_URL` (`POST /predict`). Contrato em [`../ml/README.md`](../ml/README.md).

## Como executar

TODO: versão do Java, variáveis de ambiente, banco de dados, autenticação e comandos.
