package es.unex.cum.mdai.motoresbits.domain.service;

import java.util.List;
import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;

public interface ProductoService {
    Producto crear(Producto p, Long categoriaId);
    Producto actualizar(Long id, Producto datos, Long categoriaId);
    Producto buscarPorId(Long id);
    List<Producto> listar();
    List<Producto> listarPorCategoria(Long categoriaId); // ← añade esta línea
    void borrar(Long id);
}
