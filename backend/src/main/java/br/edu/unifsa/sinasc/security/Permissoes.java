package br.edu.unifsa.sinasc.security;

/**
 * Ponto único da matriz de acesso (governança, seção 6). Para mudar quem pode o quê,
 * basta trocar a expressão aqui ou o uso nos controllers:
 * <pre>
 * &#64;PreAuthorize(Permissoes.OPERADOR)
 * </pre>
 */
public final class Permissoes {

    public static final String OPERADOR = "hasRole('OPERADOR')";
    public static final String GESTOR_AUDITOR = "hasRole('GESTOR_AUDITOR')";
    public static final String QUALQUER_PERFIL = "isAuthenticated()";

    private Permissoes() {
    }
}
