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
        Categoria c = new Categoria();
        c.setNombre(nombre);
        c.setDescripcion(descripcion);
        return categoriaRepository.save(c);
    }

    @Override
    public Categoria editarCategoria(Long id, String nombre, String descripcion) {
        Categoria c = obtenerCategoria(id);
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
    public void eliminarCategoria(Long id) {
        Categoria c = obtenerCategoria(id);

        boolean tieneProductos = productoRepository.existsByCategoriaId(id);
        if (tieneProductos) {
            throw new CategoriaConProductosException(id);
        }

        categoriaRepository.delete(c);
    }

    // ------------------ PRODUCTOS ------------------

    @Override
    public Producto crearProducto(Long idCategoria, String nombre, String referencia,
                                  BigDecimal precio, Integer stock) {

        Categoria categoria = obtenerCategoria(idCategoria);

        Producto p = new Producto();
        p.setCategoria(categoria);
        p.setNombre(nombre);
        p.setReferencia(referencia);
        p.setPrecio(precio);
        p.setStock(stock);

        return productoRepository.save(p);
    }

    @Override
    public Producto editarProducto(Long id, Long idCategoria, String nombre,
                                   BigDecimal precio, Integer stock) {

        Producto p = obtenerProducto(id);
        Categoria cat = obtenerCategoria(idCategoria);

        p.setNombre(nombre);
        p.setPrecio(precio);
        p.setStock(stock);
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
    public void eliminarProducto(Long id) {
        Producto p = obtenerProducto(id);

        boolean tieneDetallesPedido =
                detallePedidoRepository.existsByProducto_Id(id);

        boolean tieneResenas =
                resenaRepository.existsByProductoId(id);

        if (tieneDetallesPedido || tieneResenas) {
            throw new ProductoConDependenciasException(id);
        }

        productoRepository.delete(p);
    }
}
