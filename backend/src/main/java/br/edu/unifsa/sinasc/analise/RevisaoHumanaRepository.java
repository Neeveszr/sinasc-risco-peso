package br.edu.unifsa.sinasc.analise;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RevisaoHumanaRepository extends JpaRepository<RevisaoHumana, Long> {

    List<RevisaoHumana> findByPredicaoId(Long predicaoId);

    boolean existsByPredicaoIdAndStatus(Long predicaoId, StatusRevisao status);

    List<RevisaoHumana> findByStatus(StatusRevisao status);
}
