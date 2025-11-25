package es.unex.cum.mdai.motoresbits.service.exception;

public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Email o contraseña inválidos");
    }
}
