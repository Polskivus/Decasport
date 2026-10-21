package servicio;

/**
 * Partes del ERP a las que se da o quita acceso segun el rol.
 */
public enum Modulo {

	USUARIOS("Usuarios y roles"),
	CLIENTES("Clientes"),
	PROVEEDORES("Proveedores"),
	INVENTARIO("Productos e inventario"),
	VENTAS("Ventas"),
	COMPRAS("Compras"),
	DOCUMENTOS("Albaranes y facturas");

	private final String descripcion;

	Modulo(String descripcion) {
		this.descripcion = descripcion;
	}

	public String getDescripcion() {
		return descripcion;
	}
}