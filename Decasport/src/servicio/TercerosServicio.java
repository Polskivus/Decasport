package servicio;

import java.util.List;

import datos.BaseDatosMemoria;
import datos.Tabla;
import modelo.Cliente;
import modelo.ErpException;
import modelo.Producto;
import modelo.Proveedor;
import modelo.Tercero;

/**
 * Paquete 2: Terceros. Alta, modificacion, baja y busqueda de clientes y
 * proveedores.
 */
public class TercerosServicio {

	private final BaseDatosMemoria bd;

	public TercerosServicio(BaseDatosMemoria bd) {
		this.bd = bd;
	}

	// ------------------------------------------------------------------
	// Clientes
	// ------------------------------------------------------------------
	public List<Cliente> listarClientes() {
		return bd.clientes.todos();
	}

	public Cliente obtenerCliente(int id) {
		return bd.clientes.obtener(id);
	}

	public List<Cliente> buscarClientes(String texto) {
		return buscar(bd.clientes, texto);
	}

	public Cliente crearCliente(Cliente cliente) {
		return crear(bd.clientes, cliente);
	}

	public void modificarCliente(int id, Cliente datos) {
		modificar(bd.clientes, id, datos);
	}

	/** FK Venta.cliente_id sin ON DELETE: no se borra un cliente con ventas. */
	public void eliminarCliente(int id) {
		obtenerCliente(id);
		if (bd.ventas.existe(v -> v.getClienteId() == id)) {
			throw new ErpException("El cliente tiene ventas y no se puede eliminar");
		}
		bd.clientes.eliminar(id);
	}

	// ------------------------------------------------------------------
	// Proveedores
	// ------------------------------------------------------------------
	public List<Proveedor> listarProveedores() {
		return bd.proveedores.todos();
	}

	public Proveedor obtenerProveedor(int id) {
		return bd.proveedores.obtener(id);
	}

	public List<Proveedor> buscarProveedores(String texto) {
		return buscar(bd.proveedores, texto);
	}

	public Proveedor crearProveedor(Proveedor proveedor) {
		return crear(bd.proveedores, proveedor);
	}

	public void modificarProveedor(int id, Proveedor datos) {
		modificar(bd.proveedores, id, datos);
	}

	/**
	 * ON DELETE SET NULL: los productos del proveedor se quedan sin proveedor.
	 * Devuelve cuantos productos se han quedado sin proveedor.
	 */
	public int eliminarProveedor(int id) {
		obtenerProveedor(id);
		List<Producto> afectados = bd.productos.filtrar(p -> p.getProveedorId() != null && p.getProveedorId() == id);
		for (Producto p : afectados) {
			p.setProveedorId(null);
		}
		bd.proveedores.eliminar(id);
		return afectados.size();
	}

	public List<Producto> productosDeProveedor(int proveedorId) {
		return bd.productos.filtrar(p -> p.getProveedorId() != null && p.getProveedorId() == proveedorId);
	}

	// ------------------------------------------------------------------
	// Comun a clientes y proveedores
	// ------------------------------------------------------------------
	private <T extends Tercero> T crear(Tabla<T> tabla, T nuevo) {
		comprobarNifLibre(tabla, nuevo.getNif(), 0);
		return tabla.insertar(nuevo);
	}

	private <T extends Tercero> void modificar(Tabla<T> tabla, int id, Tercero datos) {
		T existente = tabla.obtener(id);
		comprobarNifLibre(tabla, datos.getNif(), id);
		existente.copiarDatosDe(datos);
	}

	// NIF es UQ: no puede repetirse dentro de la misma tabla
	private <T extends Tercero> void comprobarNifLibre(Tabla<T> tabla, String nif, int idPropio) {
		if (tabla.existe(t -> t.getNif().equalsIgnoreCase(nif) && t.getId() != idPropio)) {
			throw new ErpException("Ya existe un " + tabla.getNombre() + " con NIF " + nif);
		}
	}

	private <T extends Tercero> List<T> buscar(Tabla<T> tabla, String texto) {
		String t = texto == null ? "" : texto.trim().toLowerCase();
		return tabla.filtrar(x -> x.getRazonSocial().toLowerCase().contains(t) || x.getNif().toLowerCase().contains(t));
	}
}