package es.unex.cum.mdai.motoresbits.service;

import es.unex.cum.mdai.motoresbits.data.model.entity.Resena;

import java.util.List;
import java.util.Optional;

// Servicio para gestionar reseñas: crear, editar, listar y eliminar.
public interface ResenaService {

    Resena crearResena(Long idUsuario, Long idProducto, Integer puntuacion, String comentario);

    Resena editarResena(Long idResena, Integer puntuacion, String comentario);

    void eliminarResena(Long idResena);

    List<Resena> listarPorProducto(Long idProducto);

    Optional<Double> mediaPuntuacionPorProducto(Long idProducto);

    Resena obtenerResena(Long idResena);

    List<Resena> listarResenasUsuario(Long idUsuario);

    void eliminarResenasDeProducto(Long idProducto);
}
