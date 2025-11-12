package es.unex.cum.mdai.motoresbits.domain.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.unex.cum.mdai.motoresbits.data.model.entity.Categoria;
import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import es.unex.cum.mdai.motoresbits.data.repository.CategoriaRepository;
import es.unex.cum.mdai.motoresbits.data.repository.DetallePedidoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.ProductoRepository;
import es.unex.cum.mdai.motoresbits.domain.exception.BusinessRuleException;
import es.unex.cum.mdai.motoresbits.domain.exception.NotFoundException;
import es.unex.cum.mdai.motoresbits.domain.service.ProductoService;

@Service
@Transactional
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepo;
    private final CategoriaRepository categoriaRepo;
    private final DetallePedidoRepository detalleRepo;

    public ProductoServiceImpl(
            ProductoRepository productoRepo,
            CategoriaRepository categoriaRepo,
            DetallePedidoRepository detalleRepo) {
        this.productoRepo = productoRepo;
        this.categoriaRepo = categoriaRepo;
        this.detalleRepo = detalleRepo;
    }

    @Override
    public Producto crear(Producto p, Long categoriaId) {
        Categoria cat = categoriaRepo.findById(categoriaId)
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada"));
        p.setCategoria(cat);
        return productoRepo.save(p);
    }

    @Override
    public Producto actualizar(Long id, Producto datos, Long categoriaId) {
        Producto p = productoRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));
        Categoria cat = categoriaRepo.findById(categoriaId)
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada"));
        p.setCategoria(cat);
        p.setNombre(datos.getNombre());
        p.setReferencia(datos.getReferencia());
        p.setPrecio(datos.getPrecio());
        p.setStock(datos.getStock());
        return productoRepo.save(p);
    }

    @Override
    @Transactional(readOnly = true)
    public Producto buscarPorId(Long id) {
        return productoRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> listar() {
        return productoRepo.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> listarPorCategoria(Long categoriaId) {
        return productoRepo.findByCategoriaId(categoriaId); // ya existe en tu repo. :contentReference[oaicite:2]{index=2}
    }

    @Override
    public void borrar(Long id) {
        if (!productoRepo.existsById(id))
            throw new NotFoundException("Producto no encontrado");
        // Necesita el añadido: existsByProducto_Id en DetallePedidoRepository
        if (detalleRepo.existsByProducto_Id(id))
            throw new BusinessRuleException("No se puede borrar: el producto está en pedidos");
        productoRepo.deleteById(id);
    }
}
