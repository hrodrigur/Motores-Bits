package es.unex.cum.mdai.motoresbits.domain.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.unex.cum.mdai.motoresbits.data.model.entity.Categoria;
import es.unex.cum.mdai.motoresbits.data.repository.CategoriaRepository;
import es.unex.cum.mdai.motoresbits.domain.exception.BusinessRuleException;
import es.unex.cum.mdai.motoresbits.domain.exception.NotFoundException;
import es.unex.cum.mdai.motoresbits.domain.service.CategoriaService;

@Service
@Transactional
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepo;

    public CategoriaServiceImpl(CategoriaRepository categoriaRepo) {
        this.categoriaRepo = categoriaRepo;
    }

    @Override
    public Categoria crear(Categoria c) { return categoriaRepo.save(c); }

    @Override
    public Categoria actualizar(Long id, Categoria datos) {
        Categoria c = categoriaRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada"));
        c.setNombre(datos.getNombre());
        c.setDescripcion(datos.getDescripcion());
        return c;
    }

    @Override
    @Transactional(readOnly = true)
    public Categoria buscarPorId(Long id) {
        return categoriaRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Categoria> listar() { return categoriaRepo.findAll(); }

    @Override
    public void borrar(Long id) {
        Categoria c = categoriaRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada"));
        if (c.getProductos() != null && !c.getProductos().isEmpty())
            throw new BusinessRuleException("No se puede borrar: la categoría tiene productos"); // tu entidad expone getProductos(). :contentReference[oaicite:9]{index=9}
        categoriaRepo.delete(c);
    }
}
