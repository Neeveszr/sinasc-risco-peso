package br.edu.unifsa.sinasc.analise;

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

@Entity
@Table(name = "registro_analise")
public class RegistroAnalise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    @Column(name = "faixa_etaria", length = 5)
    private String faixaEtaria;

    @Column(name = "estado_civil")
    private Integer estadoCivil;

    @Column(name = "escolaridade")
    private Integer escolaridade;

    @Column(name = "tipo_gravidez")
    private Integer tipoGravidez;

    @Column(name = "gestacoes_anteriores", nullable = false)
    private Integer gestacoesAnteriores;

    @Column(name = "partos_normais", nullable = false)
    private Integer partosNormais;

    @Column(name = "partos_cesareos", nullable = false)
    private Integer partosCesareos;

    @Column(name = "filhos_vivos", nullable = false)
    private Integer filhosVivos;

    @Column(name = "perdas_fetais", nullable = false)
    private Integer perdasFetais;

    @Column(name = "mes_pre_natal")
    private Integer mesPreNatal;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    public Long getId() { return id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getFaixaEtaria() { return faixaEtaria; }
    public void setFaixaEtaria(String faixaEtaria) { this.faixaEtaria = faixaEtaria; }

    public Integer getEstadoCivil() { return estadoCivil; }
    public void setEstadoCivil(Integer estadoCivil) { this.estadoCivil = estadoCivil; }

    public Integer getEscolaridade() { return escolaridade; }
    public void setEscolaridade(Integer escolaridade) { this.escolaridade = escolaridade; }

    public Integer getTipoGravidez() { return tipoGravidez; }
    public void setTipoGravidez(Integer tipoGravidez) { this.tipoGravidez = tipoGravidez; }

    public Integer getGestacoesAnteriores() { return gestacoesAnteriores; }
    public void setGestacoesAnteriores(Integer gestacoesAnteriores) { this.gestacoesAnteriores = gestacoesAnteriores; }

    public Integer getPartosNormais() { return partosNormais; }
    public void setPartosNormais(Integer partosNormais) { this.partosNormais = partosNormais; }

    public Integer getPartosCesareos() { return partosCesareos; }
    public void setPartosCesareos(Integer partosCesareos) { this.partosCesareos = partosCesareos; }

    public Integer getFilhosVivos() { return filhosVivos; }
    public void setFilhosVivos(Integer filhosVivos) { this.filhosVivos = filhosVivos; }

    public Integer getPerdasFetais() { return perdasFetais; }
    public void setPerdasFetais(Integer perdasFetais) { this.perdasFetais = perdasFetais; }

    public Integer getMesPreNatal() { return mesPreNatal; }
    public void setMesPreNatal(Integer mesPreNatal) { this.mesPreNatal = mesPreNatal; }

    public Instant getCriadoEm() { return criadoEm; }
}