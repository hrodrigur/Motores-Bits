package es.unex.cum.mdai.motoresbits.service.impl;

import es.unex.cum.mdai.motoresbits.data.model.entity.Producto;
import es.unex.cum.mdai.motoresbits.data.model.entity.Resena;
import es.unex.cum.mdai.motoresbits.data.model.entity.Usuario;
import es.unex.cum.mdai.motoresbits.data.repository.ProductoRepository;
import es.unex.cum.mdai.motoresbits.data.repository.ResenaRepository;
import es.unex.cum.mdai.motoresbits.data.repository.UsuarioRepository;
import es.unex.cum.mdai.motoresbits.service.ResenaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// Implementación del servicio de reseñas: crear, editar, listar y eliminar reseñas.
@Service
@Transactional
public class ResenaServiceImpl implements ResenaService {

    private final ResenaRepository resenaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;

    public ResenaServiceImpl(ResenaRepository resenaRepository,
                             ProductoRepository productoRepository,
                             UsuarioRepository usuarioRepository) {
        this.resenaRepository = resenaRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public Resena crearResena(Long idUsuario, Long idProducto, Integer puntuacion, String comentario) {
        // validaciones básicas
        if (puntuacion == null || puntuacion < 1 || puntuacion > 5) {
            throw new IllegalArgumentException("La puntuación debe estar entre 1 y 5");
        }

        Usuario u = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Producto p = productoRepository.findById(idProducto)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

        Resena r = new Resena();
        r.setUsuario(u);
        r.setProducto(p);
        r.setPuntuacion(puntuacion);
        r.setComentario(comentario);

        return resenaRepository.save(r);
    }

    @Override
    public Resena editarResena(Long idResena, Integer puntuacion, String comentario) {
        Resena r = resenaRepository.findById(idResena)
                .orElseThrow(() -> new IllegalArgumentException("Reseña no encontrada"));

        if (puntuacion != null) {
            if (puntuacion < 1 || puntuacion > 5) throw new IllegalArgumentException("La puntuación debe estar entre 1 y 5");
            r.setPuntuacion(puntuacion);
        }
        r.setComentario(comentario);
        return resenaRepository.save(r);
    }

    @Override
    public void eliminarResena(Long idResena) {
        if (!resenaRepository.existsById(idResena)) throw new IllegalArgumentException("Reseña no encontrada");
        resenaRepository.deleteById(idResena);
    }

    @Override
    @Transactional(readOnly = true)
    public Resena obtenerResena(Long idResena) {
        return resenaRepository.findById(idResena)
                .orElseThrow(() -> new IllegalArgumentException("Reseña no encontrada"));
    }

    @Override
    public List<Resena> listarPorProducto(Long idProducto) {
        return resenaRepository.findByProductoId(idProducto);
    }

    @Override
    public Optional<Double> mediaPuntuacionPorProducto(Long idProducto) {
        return resenaRepository.avgPuntuacionByProductoId(idProducto);
    }

    @Override
    public List<Resena> listarResenasUsuario(Long idUsuario) {
        return resenaRepository.findByUsuarioId(idUsuario);
    }

    @Override
    @Transactional
    public void eliminarResenasDeProducto(Long idProducto) {
        resenaRepository.deleteByProductoId(idProducto);
    }
}
