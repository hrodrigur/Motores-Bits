package es.unex.cum.mdai.motoresbits.service.exception;

import es.unex.cum.mdai.motoresbits.data.model.enums.EstadoPedido;

public class EstadoPedidoInvalidoException extends RuntimeException {

    public EstadoPedidoInvalidoException(EstadoPedido actual, EstadoPedido nuevo) {
        super("Transición de estado no permitida: " + actual + " -> " + nuevo);
    }
}
