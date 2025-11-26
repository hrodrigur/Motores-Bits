package es.unex.cum.mdai.motoresbits.data.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.unex.cum.mdai.motoresbits.data.model.entity.Resena;

public interface ResenaRepository extends JpaRepository<Resena, Long> {

    List<Resena> findByProductoId(Long productoId);

    @Query("select avg(r.puntuacion) from Resena r where r.producto.id = :pid")
    Optional<Double> avgPuntuacionByProductoId(@Param("pid") Long productoId);

    // Devuelve true si existe al menos una reseña para el producto
    boolean existsByProductoId(Long productoId);
}
