package br.edu.unifsa.sinasc.auditoria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogAcessoRepository extends JpaRepository<LogAcesso, Long> {

    Page<LogAcesso> findByUsuarioId(Long usuarioId, Pageable pageable);
}
