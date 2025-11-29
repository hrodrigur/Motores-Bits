package es.unex.cum.mdai.motoresbits.service.impl;

import es.unex.cum.mdai.motoresbits.data.model.entity.Categoria;
import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import es.unex.cum.mdai.motoresbits.data.repository.CategoriaRepository;
import es.unex.cum.mdai.motoresbits.data.repository.ProductoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.DetallePedidoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.ResenaRepository;
import es.unex.cum.mdai.motoresbits.service.ProductoService;
import es.unex.cum.mdai.motoresbits.service.exception.ProductoNoEncontradoException;
import es.unex.cum.mdai.motoresbits.service.exception.ProductoConDependenciasException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final ResenaRepository resenaRepository;

    public ProductoServiceImpl(ProductoRepository productoRepository,
                               CategoriaRepository categoriaRepository,
                               DetallePedidoRepository detallePedidoRepository,
                               ResenaRepository resenaRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.resenaRepository = resenaRepository;
    }

    @Override
    public Producto crearProducto(Long idCategoria, String nombre, String referencia,
                                  BigDecimal precio, Integer stock) {
        Categoria categoria = categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada: " + idCategoria));

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
        Producto p = getById(id);
        Categoria categoria = categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada: " + idCategoria));

        p.setNombre(nombre);
        p.setPrecio(precio);
        p.setStock(stock);
        p.setCategoria(categoria);

        return productoRepository.save(p);
    }

    @Override
    @Transactional(readOnly = true)
    public Producto getById(Long id) {
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
        Producto p = getById(id);

        boolean tieneDetallesPedido = detallePedidoRepository.existsByProducto_Id(id);
        boolean tieneResenas = resenaRepository.existsByProductoId(id);

        if (tieneDetallesPedido || tieneResenas) {
            throw new ProductoConDependenciasException(id);
        }

        productoRepository.delete(p);
    }
}
