-- Rodolfo: usuários e registro de acessos (governança, seção 6).
-- Carlos: use V2__... em diante para registro_analise, predicao, revisao_humana.

CREATE TABLE usuario (
    id          BIGSERIAL PRIMARY KEY,
    nome        VARCHAR(120) NOT NULL,
    email       VARCHAR(160) NOT NULL,
    senha_hash  VARCHAR(100) NOT NULL,
    perfil      VARCHAR(20)  NOT NULL,
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_usuario_perfil CHECK (perfil IN ('OPERADOR', 'GESTOR_AUDITOR'))
);

-- E-mail único sem diferenciar maiúsculas/minúsculas.
CREATE UNIQUE INDEX ux_usuario_email ON usuario (lower(email));

CREATE TABLE log_acesso (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT       NOT NULL REFERENCES usuario (id),
    acao        VARCHAR(40)  NOT NULL,
    recurso     VARCHAR(120) NOT NULL,
    criado_em   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_log_acesso_usuario ON log_acesso (usuario_id);
CREATE INDEX ix_log_acesso_criado_em ON log_acesso (criado_em);
