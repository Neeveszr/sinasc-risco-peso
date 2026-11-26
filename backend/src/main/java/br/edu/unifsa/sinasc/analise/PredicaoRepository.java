package br.edu.unifsa.sinasc.analise;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PredicaoRepository extends JpaRepository<Predicao, Long> {

    List<Predicao> findByRegistroAnaliseIdOrderByCriadoEmDesc(Long registroAnaliseId);

    List<Predicao> findByUsuarioId(Long usuarioId);
    
    boolean existsByRegistroAnaliseId(Long registroAnaliseId);
}
