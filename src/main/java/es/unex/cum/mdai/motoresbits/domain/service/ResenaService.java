package es.unex.cum.mdai.motoresbits.domain.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import es.unex.cum.mdai.motoresbits.data.model.entity.Resena;

public interface ResenaService {
    Resena crear(Long productoId, Long usuarioId, int puntuacion, String comentario, LocalDateTime creadaEn);
    List<Resena> listarPorProducto(Long productoId);
    Optional<Double> mediaPuntuacion(Long productoId);
    void borrar(Long resenaId);
}
