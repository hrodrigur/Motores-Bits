package es.unex.cum.mdai.motoresbits.service.impl;

import es.unex.cum.mdai.motoresbits.data.model.entity.Categoria;
import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import es.unex.cum.mdai.motoresbits.data.repository.*;
import es.unex.cum.mdai.motoresbits.service.CatalogoService;
import es.unex.cum.mdai.motoresbits.service.exception.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class CatalogoServiceImpl implements CatalogoService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final ResenaRepository resenaRepository;

    public CatalogoServiceImpl(
            CategoriaRepository categoriaRepository,
            ProductoRepository productoRepository,
            DetallePedidoRepository detallePedidoRepository,
            ResenaRepository resenaRepository
    ) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.resenaRepository = resenaRepository;
    }

    // ------------------ CATEGORÍAS ------------------

    @Override
    public Categoria crearCategoria(String nombre, String descripcion) {
        // Validaciones: nombre obligatorio, max 30; descripcion max 100
        if (nombre == null || nombre.isBlank() || nombre.length() > 30) {
            throw new DatosProductoInvalidosException("El nombre de la categoría debe tener entre 1 y 30 caracteres");
        }
        if (descripcion != null && descripcion.length() > 100) {
            throw new DatosProductoInvalidosException("La descripción de la categoría no puede exceder 100 caracteres");
        }
        Categoria c = new Categoria();
        c.setNombre(nombre);
        c.setDescripcion(descripcion);
        return categoriaRepository.save(c);
    }

    @Override
    public Categoria editarCategoria(Long id, String nombre, String descripcion) {
        Categoria c = obtenerCategoria(id);
        // Validaciones en edición también
        if (nombre == null || nombre.isBlank() || nombre.length() > 30) {
            throw new DatosProductoInvalidosException("El nombre de la categoría debe tener entre 1 y 30 caracteres");
        }
        if (descripcion != null && descripcion.length() > 100) {
            throw new DatosProductoInvalidosException("La descripción de la categoría no puede exceder 100 caracteres");
        }
        c.setNombre(nombre);
        c.setDescripcion(descripcion);
        return categoriaRepository.save(c);
    }

    @Override
    @Transactional(readOnly = true)
    public Categoria obtenerCategoria(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new CategoriaNoEncontradaException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Categoria> listarCategorias() {
        return categoriaRepository.findAll();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void eliminarCategoria(Long id) {

        // 1) Obtener la categoría (opcional, pero útil para validar)
        Categoria categoria = obtenerCategoria(id);

        // 2) Obtener todos los productos de esa categoría
        List<Producto> productosDeLaCategoria = productoRepository.findByCategoriaId(id);

        // 3) Eliminar cada producto usando tu lógica actual
        for (Producto p : productosDeLaCategoria) {
            eliminarProducto(p.getId());
        }

        // 4) Eliminar la categoría
        categoriaRepository.delete(categoria);
    }

    // Helper: clamp stock entre 0 y 100
    private Integer clampStock(Integer stock) {
        if (stock == null) return 0;
        if (stock < 0) return 0;
        if (stock > 100) return 100;
        return stock;
    }

    // ------------------ PRODUCTOS ------------------

    @Override
    public Producto crearProducto(Long idCategoria, String nombre, String referencia,
                                  BigDecimal precio, Integer stock) {

        // Validar referencia única
        if (productoRepository.existsByReferencia(referencia)) {
            throw new ReferenciaProductoDuplicadaException(referencia);
        }

        // Validaciones de nombre y precio
        if (nombre == null || nombre.isEmpty() || nombre.length() > 30) {
            throw new DatosProductoInvalidosException("El nombre debe tener entre 1 y 30 caracteres");
        }
        // Validar referencia
        if (referencia == null || referencia.isBlank() || referencia.length() > 15) {
            throw new DatosProductoInvalidosException("La referencia es obligatoria y debe tener como máximo 15 caracteres");
        }
        if (precio == null || precio.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new DatosProductoInvalidosException("El precio debe ser mayor que 0");
        }
        // máximo 9 dígitos en la parte entera: precio < 1,000,000,000
        java.math.BigDecimal limite = new java.math.BigDecimal("1000000000");
        if (precio.compareTo(limite) >= 0) {
            throw new DatosProductoInvalidosException("El precio entero no puede tener más de 9 dígitos");
        }

        Categoria categoria = obtenerCategoria(idCategoria);

        Producto p = new Producto();
        p.setCategoria(categoria);
        p.setNombre(nombre);
        p.setReferencia(referencia);
        p.setPrecio(precio);
        p.setStock(clampStock(stock));

        return productoRepository.save(p);
    }

    @Override
    public Producto editarProducto(Long id, Long idCategoria, String nombre,
                                   BigDecimal precio, Integer stock) {

        Producto p = obtenerProducto(id);
        Categoria cat = obtenerCategoria(idCategoria);

        // Validaciones de nombre y precio en edición también
        if (nombre == null || nombre.isEmpty() || nombre.length() > 30) {
            throw new DatosProductoInvalidosException("El nombre debe tener entre 1 y 30 caracteres");
        }
        if (precio == null || precio.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new DatosProductoInvalidosException("El precio debe ser mayor que 0");
        }
        java.math.BigDecimal limite = new java.math.BigDecimal("1000000000");
        if (precio.compareTo(limite) >= 0) {
            throw new DatosProductoInvalidosException("El precio entero no puede tener más de 9 dígitos");
        }

        p.setNombre(nombre);
        p.setPrecio(precio);
        p.setStock(clampStock(stock));
        p.setCategoria(cat);

        return productoRepository.save(p);
    }

    @Override
    @Transactional(readOnly = true)
    public Producto obtenerProducto(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ProductoNoEncontradoException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> listarProductos() {
        return productoRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> listarPorCategoria(Long idCategoria) {
        return productoRepository.findByCategoriaId(idCategoria);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void eliminarProducto(Long id) {

        // 1) Borrar detalles de pedido del producto
        detallePedidoRepository.deleteByProducto_Id(id);

        // 2) Borrar todas las reseñas de ese producto
        resenaRepository.deleteByProductoId(id);

        // 3) Borrar el propio producto
        productoRepository.deleteById(id);
    }


    @Override
    @Transactional(readOnly = true)
    public java.util.List<es.unex.cum.mdai.motoresbits.service.dto.ProductoAdminDto> listarProductosConCategoria() {
        var productos = productoRepository.findAll();
        var result = new java.util.ArrayList<es.unex.cum.mdai.motoresbits.service.dto.ProductoAdminDto>();
        for (var p : productos) {
            var dto = new es.unex.cum.mdai.motoresbits.service.dto.ProductoAdminDto();
            dto.setId(p.getId()); dto.setNombre(p.getNombre()); dto.setReferencia(p.getReferencia());
            dto.setPrecio(p.getPrecio()); dto.setStock(p.getStock());
            dto.setNombreCategoria(p.getCategoria() != null ? p.getCategoria().getNombre() : "");
            result.add(dto);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<es.unex.cum.mdai.motoresbits.service.dto.ProductoAdminDto> listarPorCategoriaConCategoria(Long idCategoria) {
        var productos = productoRepository.findByCategoriaId(idCategoria);
        var result = new java.util.ArrayList<es.unex.cum.mdai.motoresbits.service.dto.ProductoAdminDto>();
        for (var p : productos) {
            var dto = new es.unex.cum.mdai.motoresbits.service.dto.ProductoAdminDto();
            dto.setId(p.getId()); dto.setNombre(p.getNombre()); dto.setReferencia(p.getReferencia());
            dto.setPrecio(p.getPrecio()); dto.setStock(p.getStock());
            dto.setNombreCategoria(p.getCategoria() != null ? p.getCategoria().getNombre() : "");
            result.add(dto);
        }
        return result;
    }
}
