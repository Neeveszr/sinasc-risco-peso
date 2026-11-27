package br.edu.unifsa.sinasc.analise;

import java.time.Instant;

import br.edu.unifsa.sinasc.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "revisao_humana")
public class RevisaoHumana {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "predicao_id", nullable = false)
    private Predicao predicao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StatusRevisao status = StatusRevisao.PENDENTE;

    @Column(name = "solicitado_em", nullable = false, updatable = false)
    private Instant solicitadoEm = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revisor_id")
    private Usuario revisor;

    @Column(columnDefinition = "text")
    private String parecer;

    @Column(name = "concluida_em")
    private Instant concluidaEm;

    public Long getId() { return id; }

    public Predicao getPredicao() { return predicao; }
    public void setPredicao(Predicao predicao) { this.predicao = predicao; }

    public Usuario getSolicitante() { return solicitante; }
    public void setSolicitante(Usuario solicitante) { this.solicitante = solicitante; }

    public StatusRevisao getStatus() { return status; }
    public void setStatus(StatusRevisao status) { this.status = status; }

    public Instant getSolicitadoEm() { return solicitadoEm; }

    public Usuario getRevisor() { return revisor; }
    public void setRevisor(Usuario revisor) { this.revisor = revisor; }

    public String getParecer() { return parecer; }
    public void setParecer(String parecer) { this.parecer = parecer; }

    public Instant getConcluidaEm() { return concluidaEm; }
    public void setConcluidaEm(Instant concluidaEm) { this.concluidaEm = concluidaEm; }
}
