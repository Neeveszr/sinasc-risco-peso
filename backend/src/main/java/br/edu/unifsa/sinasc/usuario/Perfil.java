package br.edu.unifsa.sinasc.usuario;

/**
 * Perfis do sistema. O perfil é definido no back-end: só um gestor/auditor cria contas e escolhe o perfil.
 * No Spring Security viram as autoridades ROLE_OPERADOR e ROLE_GESTOR_AUDITOR.
 */
public enum Perfil {
    OPERADOR,
    GESTOR_AUDITOR
}
