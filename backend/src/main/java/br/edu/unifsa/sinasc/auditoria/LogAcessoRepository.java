package br.edu.unifsa.sinasc.auditoria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LogAcessoRepository
        extends JpaRepository<LogAcesso, Long>, JpaSpecificationExecutor<LogAcesso> {
}
