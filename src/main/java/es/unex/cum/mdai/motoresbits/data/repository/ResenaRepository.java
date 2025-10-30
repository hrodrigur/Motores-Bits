package es.unex.cum.mdai.motoresbits.data.repository;

import es.unex.cum.mdai.motoresbits.data.model.entity.Resena;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResenaRepository extends JpaRepository<Resena, Long> {
    List<Resena> findByProductoId(Long productoId);
}
