package es.unex.cum.mdai.motoresbits.service.impl;

import es.unex.cum.mdai.motoresbits.data.model.entity.Categoria;
import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import es.unex.cum.mdai.motoresbits.data.repository.CategoriaRepository;
import es.unex.cum.mdai.motoresbits.data.repository.DetallePedidoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.ProductoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.ResenaRepository;
import es.unex.cum.mdai.motoresbits.service.CatalogoService;
import es.unex.cum.mdai.motoresbits.service.dto.ProductoAdminDto;
import es.unex.cum.mdai.motoresbits.service.exception.CategoriaNoEncontradaException;
import es.unex.cum.mdai.motoresbits.service.exception.DatosProductoInvalidosException;
import es.unex.cum.mdai.motoresbits.service.exception.ProductoNoEncontradoException;
import es.unex.cum.mdai.motoresbits.service.exception.ReferenciaProductoDuplicadaException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URI;
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

    // ✅ NUEVO: por nombre (para /categoria/{nombre})
    @Override
    @Transactional(readOnly = true)
    public Categoria obtenerCategoriaPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new DatosProductoInvalidosException("El nombre de la categoría no puede estar vacío");
        }

        String limpio = nombre.trim();

        return categoriaRepository.findByNombreIgnoreCase(limpio)
                .orElseThrow(() -> new CategoriaNoEncontradaException(limpio));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Categoria> listarCategorias() {
        return categoriaRepository.findAll();
    }

    @Override
    public void eliminarCategoria(Long id) {
        Categoria categoria = obtenerCategoria(id);
        List<Producto> productosDeLaCategoria = productoRepository.findByCategoriaId(id);

        for (Producto p : productosDeLaCategoria) {
            eliminarProducto(p.getId());
        }

        categoriaRepository.delete(categoria);
    }

    // Helper: clamp stock entre 0 y 100
    private Integer clampStock(Integer stock) {
        if (stock == null) return 0;
        if (stock < 0) return 0;
        if (stock > 100) return 100;
        return stock;
    }

    // Helper: normaliza y valida URL (si viene vacía -> null)
    private String normalizarYValidarImagenUrl(String imagenUrl) {
        if (imagenUrl == null) return null;

        String trimmed = imagenUrl.trim();
        if (trimmed.isEmpty()) return null;

        if (trimmed.length() > 1000) {
            throw new DatosProductoInvalidosException("La URL de imagen es demasiado larga");
        }

        try {
            URI uri = URI.create(trimmed);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new DatosProductoInvalidosException("La URL de imagen debe empezar por http:// o https://");
            }
        } catch (IllegalArgumentException ex) {
            throw new DatosProductoInvalidosException("La URL de imagen no es válida");
        }

        return trimmed;
    }

    // ------------------ PRODUCTOS ------------------

    @Override
    public Producto crearProducto(Long idCategoria, String nombre, String referencia,
                                  BigDecimal precio, Integer stock, String imagenUrl) {

        if (productoRepository.existsByReferencia(referencia)) {
            throw new ReferenciaProductoDuplicadaException(referencia);
        }

        if (nombre == null || nombre.isEmpty() || nombre.length() > 30) {
            throw new DatosProductoInvalidosException("El nombre debe tener entre 1 y 30 caracteres");
        }
        if (referencia == null || referencia.isBlank() || referencia.length() > 15) {
            throw new DatosProductoInvalidosException("La referencia es obligatoria y debe tener como máximo 15 caracteres");
        }
        if (precio == null || precio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DatosProductoInvalidosException("El precio debe ser mayor que 0");
        }
        BigDecimal limite = new BigDecimal("1000000000");
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

        p.setImagenUrl(normalizarYValidarImagenUrl(imagenUrl));

        return productoRepository.save(p);
    }

    @Override
    public Producto editarProducto(Long id, Long idCategoria, String nombre,
                                   BigDecimal precio, Integer stock, String imagenUrl) {

        Producto p = obtenerProducto(id);
        Categoria cat = obtenerCategoria(idCategoria);

        if (nombre == null || nombre.isEmpty() || nombre.length() > 30) {
            throw new DatosProductoInvalidosException("El nombre debe tener entre 1 y 30 caracteres");
        }
        if (precio == null || precio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DatosProductoInvalidosException("El precio debe ser mayor que 0");
        }
        BigDecimal limite = new BigDecimal("1000000000");
        if (precio.compareTo(limite) >= 0) {
            throw new DatosProductoInvalidosException("El precio entero no puede tener más de 9 dígitos");
        }

        p.setNombre(nombre);
        p.setPrecio(precio);
        p.setStock(clampStock(stock));
        p.setCategoria(cat);

        p.setImagenUrl(normalizarYValidarImagenUrl(imagenUrl));

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
    public void eliminarProducto(Long id) {
        detallePedidoRepository.deleteByProducto_Id(id);
        resenaRepository.deleteByProductoId(id);
        productoRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Producto obtenerProductoPorReferencia(String referencia) {
        if (referencia == null || referencia.isBlank()) {
            throw new DatosProductoInvalidosException("La referencia del producto no puede estar vacía");
        }

        String limpia = referencia.trim();

        return productoRepository.findByReferenciaIgnoreCase(limpia)
                .orElseThrow(() -> new ProductoNoEncontradoException("No existe el producto con referencia=" + limpia));
    }

    // ------------------ LISTADOS ADMIN DTO ------------------

    @Override
    @Transactional(readOnly = true)
    public List<ProductoAdminDto> listarProductosConCategoria() {
        var productos = productoRepository.findAll();
        var result = new java.util.ArrayList<ProductoAdminDto>();

        for (var p : productos) {
            var dto = new ProductoAdminDto();
            dto.setId(p.getId());
            dto.setNombre(p.getNombre());
            dto.setReferencia(p.getReferencia());
            dto.setPrecio(p.getPrecio());
            dto.setStock(p.getStock());
            dto.setNombreCategoria(p.getCategoria() != null ? p.getCategoria().getNombre() : "");
            dto.setImagenUrl(p.getImagenUrl());
            result.add(dto);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoAdminDto> listarPorCategoriaConCategoria(Long idCategoria) {
        var productos = productoRepository.findByCategoriaId(idCategoria);
        var result = new java.util.ArrayList<ProductoAdminDto>();

        for (var p : productos) {
            var dto = new ProductoAdminDto();
            dto.setId(p.getId());
            dto.setNombre(p.getNombre());
            dto.setReferencia(p.getReferencia());
            dto.setPrecio(p.getPrecio());
            dto.setStock(p.getStock());
            dto.setNombreCategoria(p.getCategoria() != null ? p.getCategoria().getNombre() : "");
            dto.setImagenUrl(p.getImagenUrl());
            result.add(dto);
        }
        return result;
    }
}
