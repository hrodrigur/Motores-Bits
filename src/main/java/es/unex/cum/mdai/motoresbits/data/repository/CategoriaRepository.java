package es.unex.cum.mdai.motoresbits.data.repository;

import es.unex.cum.mdai.motoresbits.data.model.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}
