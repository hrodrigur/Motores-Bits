package es.unex.cum.mdai.motoresbits.domain.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import es.unex.cum.mdai.motoresbits.data.model.entity.Resena;
import es.unex.cum.mdai.motoresbits.data.model.entity.Usuario;
import es.unex.cum.mdai.motoresbits.data.repository.ProductoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.ResenaRepository;
import es.unex.cum.mdai.motoresbits.data.repository.UsuarioRepository;
import es.unex.cum.mdai.motoresbits.domain.exception.NotFoundException;
import es.unex.cum.mdai.motoresbits.domain.service.ResenaService;

@Service
@Transactional
public class ResenaServiceImpl implements ResenaService {

    private final ResenaRepository resenaRepo;
    private final ProductoRepository productoRepo;
    private final UsuarioRepository usuarioRepo;

    public ResenaServiceImpl(ResenaRepository resenaRepo,
                             ProductoRepository productoRepo,
                             UsuarioRepository usuarioRepo) {
        this.resenaRepo = resenaRepo;
        this.productoRepo = productoRepo;
        this.usuarioRepo = usuarioRepo;
    }

    @Override
    public Resena crear(Long productoId, Long usuarioId, int puntuacion, String comentario, LocalDateTime creadaEn) {
        Producto producto = productoRepo.findById(productoId)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Resena r = new Resena();
        r.setProducto(producto);
        r.setUsuario(usuario);
        r.setPuntuacion(puntuacion);
        r.setComentario(comentario);
        r.setCreadaEn(creadaEn != null ? creadaEn : LocalDateTime.now());
        return resenaRepo.save(r);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Resena> listarPorProducto(Long productoId) {
        return resenaRepo.findByProductoId(productoId); // ya existe en tu repo. :contentReference[oaicite:7]{index=7}
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Double> mediaPuntuacion(Long productoId) {
        return resenaRepo.avgPuntuacionByProductoId(productoId); // ya existe en tu repo. :contentReference[oaicite:8]{index=8}
    }

    @Override
    public void borrar(Long resenaId) {
        if (!resenaRepo.existsById(resenaId))
            throw new NotFoundException("Reseña no encontrada");
        resenaRepo.deleteById(resenaId);
    }
}
