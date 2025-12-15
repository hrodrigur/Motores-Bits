package es.unex.cum.mdai.motoresbits.service.exception;

import java.math.BigDecimal;

public class SaldoInsuficienteException extends RuntimeException {

    public SaldoInsuficienteException(BigDecimal saldo, BigDecimal total) {
        super("Saldo insuficiente. Saldo actual: " + saldo + " €, total del pedido: " + total + " €");
    }

    public SaldoInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
