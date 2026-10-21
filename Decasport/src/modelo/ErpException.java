package erp.modelo;

/**
 * Error de negocio o de validacion (dato incorrecto, regla incumplida...).
 * La consola la captura y muestra el mensaje sin cerrar el programa.
 */
public class ErpException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ErpException(String mensaje) {
		super(mensaje);
	}
}
