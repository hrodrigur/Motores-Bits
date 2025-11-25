package es.unex.cum.mdai.motoresbits.service.exception;

public class UsuarioNoEncontradoException extends RuntimeException {
    public UsuarioNoEncontradoException(Long id) {
        super("No se ha encontrado el usuario con id=" + id);
    }
}
