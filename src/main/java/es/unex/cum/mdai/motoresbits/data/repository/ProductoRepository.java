package es.unex.cum.mdai.motoresbits.data.repository;

import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByCategoriaId(Long categoriaId);

    // Devuelve true si existe al menos un producto con la categoría dada
    boolean existsByCategoriaId(Long categoriaId);

    // Decrementa el stock de forma atómica si hay stock suficiente; devuelve filas afectadas (1 o 0)
    @Modifying(clearAutomatically = true)
    @Query("update Producto p set p.stock = p.stock - :cantidad where p.id = :id and p.stock >= :cantidad")
    int descontarStock(@Param("id") Long id, @Param("cantidad") int cantidad);

    // Incrementa el stock (uso en reposición al cancelar pedidos)
    @Modifying(clearAutomatically = true)
    @Query("update Producto p set p.stock = p.stock + :cantidad where p.id = :id")
    void incrementarStock(@Param("id") Long id, @Param("cantidad") int cantidad);
}
