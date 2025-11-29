package es.unex.cum.mdai.motoresbits.service;

import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;

import java.math.BigDecimal;
import java.util.List;

public interface ProductoService {

    Producto crearProducto(Long idCategoria, String nombre, String referencia,
                           BigDecimal precio, Integer stock);

    Producto editarProducto(Long id, Long idCategoria, String nombre,
                            BigDecimal precio, Integer stock);

    Producto getById(Long id);

    List<Producto> listarProductos();

    List<Producto> listarPorCategoria(Long idCategoria);

    void eliminarProducto(Long id);
}
