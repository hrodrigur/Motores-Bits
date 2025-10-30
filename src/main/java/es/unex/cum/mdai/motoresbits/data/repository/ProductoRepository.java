package es.unex.cum.mdai.motoresbits.data.repository;

import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByCategoriaId(Long categoriaId);
}
