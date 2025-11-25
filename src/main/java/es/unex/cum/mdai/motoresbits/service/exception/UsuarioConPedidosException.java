package es.unex.cum.mdai.motoresbits.service.exception;

public class UsuarioConPedidosException extends RuntimeException {
    public UsuarioConPedidosException(Long idUsuario) {
        super("No se puede eliminar el usuario " + idUsuario + " porque tiene pedidos asociados");
    }
}
