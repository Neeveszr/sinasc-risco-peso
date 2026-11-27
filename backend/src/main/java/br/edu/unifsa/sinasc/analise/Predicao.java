package br.edu.unifsa.sinasc.analise;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "predicao")
public class Predicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registro_analise_id", nullable = false, updatable = false)
    private RegistroAnalise registroAnalise;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    @Column(name = "score", nullable = false, updatable = false)
    private Double score;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "faixa_risco", nullable = false, length = 10, updatable = false)
    private FaixaRisco faixaRisco;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fatores_shap", nullable = false, columnDefinition = "jsonb", updatable = false)
    private JsonNode fatoresShap;

    @Column(name = "versao_modelo", nullable = false, length = 40, updatable = false)
    private String versaoModelo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    public Long getId() { return id; }

    public RegistroAnalise getRegistroAnalise() { return registroAnalise; }
    public void setRegistroAnalise(RegistroAnalise registroAnalise) { this.registroAnalise = registroAnalise; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public FaixaRisco getFaixaRisco() { return faixaRisco; }
    public void setFaixaRisco(FaixaRisco faixaRisco) { this.faixaRisco = faixaRisco; }

    public JsonNode getFatoresShap() { return fatoresShap; }
    public void setFatoresShap(JsonNode fatoresShap) { this.fatoresShap = fatoresShap; }

    public String getVersaoModelo() { return versaoModelo; }
    public void setVersaoModelo(String versaoModelo) { this.versaoModelo = versaoModelo; }

    public Instant getCriadoEm() { return criadoEm; }
}