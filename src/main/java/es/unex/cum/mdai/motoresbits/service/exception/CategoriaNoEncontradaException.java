package es.unex.cum.mdai.motoresbits.service.exception;

public class CategoriaNoEncontradaException extends RuntimeException {

    // Para búsquedas por ID
    public CategoriaNoEncontradaException(Long id) {
        super("No existe la categoría con id=" + id);
    }

    // ✅ Para búsquedas por NOMBRE
    public CategoriaNoEncontradaException(String nombre) {
        super("No existe la categoría con nombre='" + nombre + "'");
    }
}
