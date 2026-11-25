-- Auditoria de falhas de login: tentativas com e-mail inexistente não têm usuário,
-- então usuario_id passa a aceitar nulo e guardamos o e-mail informado.
ALTER TABLE log_acesso ALTER COLUMN usuario_id DROP NOT NULL;

ALTER TABLE log_acesso ADD COLUMN identificador VARCHAR(160);

-- Todo registro precisa dizer quem foi: um usuário conhecido ou o e-mail informado.
ALTER TABLE log_acesso
    ADD CONSTRAINT ck_log_acesso_quem CHECK (usuario_id IS NOT NULL OR identificador IS NOT NULL);

CREATE INDEX ix_log_acesso_acao ON log_acesso (acao);
