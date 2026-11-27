
package br.edu.unifsa.sinasc.analise;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistroAnaliseRepository extends JpaRepository<RegistroAnalise, Long> {

    Page<RegistroAnalise> findByUsuarioId(Long usuarioId, Pageable pageable);
}
