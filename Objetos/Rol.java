package modelo;

/**
 * Rol de seguridad (tabla Rol). Un rol lo tienen muchos usuarios (1:N).
 */
public class Rol implements Entidad {

	public static final String ADMIN = "ADMIN";
	public static final String VENTAS = "VENTAS";
	public static final String COMPRAS = "COMPRAS";
	public static final String ALMACEN = "ALMACEN";
	public static final String CONTABILIDAD = "CONTABILIDAD";

	/** Filas que se insertan al crear la base de datos. */
	public static final String[] NOMBRES = { ADMIN, VENTAS, COMPRAS, ALMACEN, CONTABILIDAD };

	private int id; // PK AUTO_INCREMENT
	private String nombre; // VARCHAR(30) UQ

	public Rol() {
	}

	public Rol(String nombre) {
		setNombre(nombre);
	}

	@Override
	public int getId() {
		return id;
	}

	@Override
	public void setId(int id) {
		this.id = id;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = Validar.texto(nombre, "nombre", 30, true).toUpperCase();
	}

	@Override
	public String toString() {
		return id + " - " + nombre;
	}
}
