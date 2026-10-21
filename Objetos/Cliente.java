package erp.modelo;

/**
 * Cliente (tabla Cliente). Se le hacen ventas (Venta.cliente_id).
 */
public class Cliente extends Tercero {

	public Cliente() {
	}

	public Cliente(String razonSocial, String nif, String email, String telefono, String direccion) {
		super(razonSocial, nif, email, telefono, direccion);
	}
}
