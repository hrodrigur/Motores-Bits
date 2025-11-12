package es.unex.cum.mdai.motoresbits.domain.service;

import java.util.List;
import es.unex.cum.mdai.motoresbits.data.model.entity.Categoria;

public interface CategoriaService {
    Categoria crear(Categoria c);
    Categoria actualizar(Long id, Categoria datos);
    Categoria buscarPorId(Long id);
    List<Categoria> listar();
    void borrar(Long id); // no borrar si tiene productos
}
