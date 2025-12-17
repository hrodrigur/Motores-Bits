package es.unex.cum.mdai.motoresbits.service;

import es.unex.cum.mdai.motoresbits.data.model.entity.Categoria;
import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import es.unex.cum.mdai.motoresbits.service.dto.ProductoAdminDto;

import java.math.BigDecimal;
import java.util.List;

// Servicio de catálogo: gestión de categorías y productos.
public interface CatalogoService {

    Categoria crearCategoria(String nombre, String descripcion);
    Categoria editarCategoria(Long id, String nombre, String descripcion);
    Categoria obtenerCategoria(Long id);

    Categoria obtenerCategoriaPorNombre(String nombre);

    List<Categoria> listarCategorias();
    void eliminarCategoria(Long id);

    Producto crearProducto(Long idCategoria, String nombre, String referencia,
                           BigDecimal precio, Integer stock, String imagenUrl);

    Producto editarProducto(Long id, Long idCategoria, String nombre,
                            BigDecimal precio, Integer stock, String imagenUrl);

    Producto obtenerProducto(Long id);

    Producto obtenerProductoPorReferencia(String referencia);

    List<Producto> listarProductos();
    List<Producto> listarPorCategoria(Long idCategoria);
    void eliminarProducto(Long id);

    List<ProductoAdminDto> listarProductosConCategoria();
    List<ProductoAdminDto> listarPorCategoriaConCategoria(Long idCategoria);
}
