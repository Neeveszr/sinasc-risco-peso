package br.edu.unifsa.sinasc.auditoria;

import java.time.Instant;

import br.edu.unifsa.sinasc.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Registro de acessos (governança, seção 6): quem fez o quê e quando. Só é inserido, nunca alterado.
 * Em falhas de login com e-mail inexistente não há usuário: nesse caso "usuario" é nulo
 * e "identificador" guarda o e-mail informado.
 */
@Entity
@Table(name = "log_acesso")
public class LogAcesso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(length = 160)
    private String identificador;

    @Column(nullable = false, length = 40)
    private String acao;

    @Column(nullable = false, length = 120)
    private String recurso;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    public Long getId() { return id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getIdentificador() { return identificador; }
    public void setIdentificador(String identificador) { this.identificador = identificador; }

    public String getAcao() { return acao; }
    public void setAcao(String acao) { this.acao = acao; }

    public String getRecurso() { return recurso; }
    public void setRecurso(String recurso) { this.recurso = recurso; }

    public Instant getCriadoEm() { return criadoEm; }
}
