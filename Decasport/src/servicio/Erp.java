package servicio;

import java.math.BigDecimal;

import datos.BaseDatosMemoria;
import modelo.Cliente;
import modelo.Compra;
import modelo.Producto;
import modelo.Proveedor;
import modelo.Rol;
import modelo.TipoDocumento;
import modelo.Usuario;
import modelo.Venta;

/**
 * Punto de acceso a todo el ERP: crea la base de datos en memoria y los
 * servicios que trabajan sobre ella.
 */
public class Erp {

	public final AccesoServicio acceso;
	public final TercerosServicio terceros;
	public final InventarioServicio inventario;
	public final VentaServicio ventas;
	public final CompraServicio compras;
	public final DocumentoServicio documentos;

	public Erp() {
		BaseDatosMemoria bd = new BaseDatosMemoria();
		acceso = new AccesoServicio(bd);
		terceros = new TercerosServicio(bd);
		inventario = new InventarioServicio(bd);
		ventas = new VentaServicio(bd, inventario);
		compras = new CompraServicio(bd, inventario);
		documentos = new DocumentoServicio(bd);
	}

	/**
	 * Datos de prueba para poder usar el programa nada mas arrancar. Se crean
	 * con los servicios, asi pasan por las mismas reglas que los del usuario.
	 */
	public void cargarDatosDePrueba() {
		// Un usuario por rol (contrasena = nombre + "123")
		String[][] usuarios = { { "admin", Rol.ADMIN }, { "ventas", Rol.VENTAS }, { "compras", Rol.COMPRAS },
				{ "almacen", Rol.ALMACEN }, { "conta", Rol.CONTABILIDAD } };
		for (String[] u : usuarios) {
			acceso.crearUsuario(u[0], u[0] + "123", idRol(u[1]));
		}
		Usuario admin = acceso.buscarPorUsername("admin");

		Proveedor norte = terceros.crearProveedor(new Proveedor("Suministros Norte SL", "B12345678",
				"pedidos@norte.es", "944000111", "Calle Mayor 1, Bilbao"));
		Proveedor electro = terceros.crearProveedor(new Proveedor("Electro Distribucion SA", "A87654321",
				"ventas@electro.es", "915551234", "Av. de la Industria 20, Madrid"));

		Cliente tienda = terceros.crearCliente(new Cliente("Tienda Centro SL", "B11111111", "compras@tiendacentro.es",
				"943111222", "Plaza Nueva 3, Donostia"));
		terceros.crearCliente(new Cliente("Oficinas Garcia", "12345678Z", "garcia@correo.es", "600123456",
				"Calle Luna 7, Vitoria"));

		Producto raton = inventario.crearProducto(new Producto("RAT-001", "Raton inalambrico",
				new BigDecimal("19.90"), new BigDecimal("8.50"), 50, 10, electro.getId()));
		Producto teclado = inventario.crearProducto(new Producto("TEC-001", "Teclado mecanico",
				new BigDecimal("59.00"), new BigDecimal("30.00"), 20, 5, electro.getId()));
		inventario.crearProducto(new Producto("MON-024", "Monitor 24 pulgadas", new BigDecimal("149.00"),
				new BigDecimal("95.00"), 3, 5, electro.getId()));
		Producto papel = inventario.crearProducto(new Producto("PAP-A4", "Paquete papel A4 500 hojas",
				new BigDecimal("4.95"), new BigDecimal("2.10"), 200, 50, norte.getId()));

		// Una venta completa con su albaran y su factura
		Venta venta = ventas.crear(tienda.getId(), admin);
		ventas.anadirLinea(venta.getId(), raton.getId(), 5);
		ventas.anadirLinea(venta.getId(), teclado.getId(), 2);
		ventas.confirmar(venta.getId());
		documentos.emitirParaVenta(venta.getId(), TipoDocumento.ALBARAN);
		documentos.emitirParaVenta(venta.getId(), TipoDocumento.FACTURA);

		// Una compra confirmada pendiente de recibir
		Compra compra = compras.crear(admin);
		compras.anadirLinea(compra.getId(), papel.getId(), 100, null);
		compras.confirmar(compra.getId());
	}

	private int idRol(String nombre) {
		for (Rol r : acceso.listarRoles()) {
			if (r.getNombre().equals(nombre)) {
				return r.getId();
			}
		}
		throw new IllegalStateException("Falta el rol " + nombre);
	}
}