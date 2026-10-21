package servicio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import datos.BaseDatosMemoria;
import modelo.Compra;
import modelo.ErpException;
import modelo.EstadoCompra;
import modelo.LineaCompra;
import modelo.Producto;
import modelo.Proveedor;
import modelo.TipoMovimiento;
import modelo.Usuario;

/**
 * Paquete 4: Compras. Cabeceras y lineas, y los cambios de estado.
 *
 * BORRADOR: se pueden anadir y quitar lineas.
 * CONFIRMADA: el pedido se ha hecho al proveedor.
 * RECIBIDA: entra el stock (movimiento ENTRADA por cada linea).
 * ANULADA: solo antes de recibirla.
 */
public class CompraServicio {

	private final BaseDatosMemoria bd;
	private final InventarioServicio inventario;

	public CompraServicio(BaseDatosMemoria bd, InventarioServicio inventario) {
		this.bd = bd;
		this.inventario = inventario;
	}

	public List<Compra> listar() {
		return bd.compras.todos();
	}

	public List<Compra> listarPorEstado(EstadoCompra estado) {
		return bd.compras.filtrar(c -> c.getEstado() == estado);
	}

	public Compra obtener(int id) {
		return bd.compras.obtener(id);
	}

	public List<LineaCompra> lineas(int compraId) {
		return bd.lineasCompra.filtrar(l -> l.getCompraId() == compraId);
	}

	/**
	 * La compra no guarda proveedor: se deduce de los productos de sus lineas
	 * (Producto.proveedor_id), como indica el diseno.
	 */
	public List<Proveedor> proveedoresDeCompra(int compraId) {
		Set<Integer> ids = new LinkedHashSet<>();
		for (LineaCompra l : lineas(compraId)) {
			Producto p = bd.productos.buscar(l.getProductoId());
			if (p != null && p.getProveedorId() != null) {
				ids.add(p.getProveedorId());
			}
		}
		List<Proveedor> proveedores = new ArrayList<>();
		for (Integer id : ids) {
			Proveedor p = bd.proveedores.buscar(id);
			if (p != null) {
				proveedores.add(p);
			}
		}
		return proveedores;
	}

	// ------------------------------------------------------------------
	// Borrador
	// ------------------------------------------------------------------
	public Compra crear(Usuario usuario) {
		return bd.compras.insertar(new Compra(usuario.getId()));
	}

	/** Si precioCoste es null se usa el precio de coste del producto. */
	public LineaCompra anadirLinea(int compraId, int productoId, int cantidad, BigDecimal precioCoste) {
		Compra compra = obtenerEnBorrador(compraId);
		Producto producto = inventario.obtenerProducto(productoId);
		BigDecimal precio = precioCoste != null ? precioCoste : producto.getPrecioCoste();
		LineaCompra linea = new LineaCompra(compraId, productoId, cantidad, precio);
		bd.lineasCompra.insertar(linea);
		recalcular(compra);
		return linea;
	}

	public void quitarLinea(int compraId, int lineaId) {
		Compra compra = obtenerEnBorrador(compraId);
		LineaCompra linea = bd.lineasCompra.obtener(lineaId);
		if (linea.getCompraId() != compraId) {
			throw new ErpException("La linea " + lineaId + " no es de la compra " + compraId);
		}
		bd.lineasCompra.eliminar(lineaId);
		recalcular(compra);
	}

	/** Borra un borrador y sus lineas (ON DELETE CASCADE). */
	public void eliminarBorrador(int compraId) {
		obtenerEnBorrador(compraId);
		bd.lineasCompra.eliminarSi(l -> l.getCompraId() == compraId);
		bd.compras.eliminar(compraId);
	}

	private void recalcular(Compra compra) {
		BigDecimal base = BigDecimal.ZERO;
		for (LineaCompra l : lineas(compra.getId())) {
			base = base.add(l.getSubtotal());
		}
		compra.setBaseImponible(base);
		compra.setTotal(Importes.total(base));
	}

	private Compra obtenerEnBorrador(int compraId) {
		Compra compra = obtener(compraId);
		if (compra.getEstado() != EstadoCompra.BORRADOR) {
			throw new ErpException("La compra " + compraId + " ya no esta en borrador (" + compra.getEstado() + ")");
		}
		return compra;
	}

	// ------------------------------------------------------------------
	// Cambios de estado
	// ------------------------------------------------------------------
	public void confirmar(int compraId) {
		Compra compra = obtenerEnBorrador(compraId);
		if (lineas(compraId).isEmpty()) {
			throw new ErpException("La compra no tiene lineas");
		}
		compra.setEstado(EstadoCompra.CONFIRMADA);
	}

	/** La mercancia llega al almacen: entra el stock de cada linea. */
	public void recibir(int compraId) {
		Compra compra = obtener(compraId);
		if (compra.getEstado() != EstadoCompra.CONFIRMADA) {
			throw new ErpException("Solo se reciben compras confirmadas (esta: " + compra.getEstado() + ")");
		}
		for (LineaCompra l : lineas(compraId)) {
			inventario.registrarMovimiento(l.getProductoId(), TipoMovimiento.ENTRADA, l.getCantidad(), null, compraId);
		}
		compra.setEstado(EstadoCompra.RECIBIDA);
	}

	public void anular(int compraId) {
		Compra compra = obtener(compraId);
		if (compra.getEstado() == EstadoCompra.ANULADA) {
			throw new ErpException("La compra ya esta anulada");
		}
		if (compra.getEstado() == EstadoCompra.RECIBIDA) {
			throw new ErpException("Una compra recibida no se puede anular");
		}
		compra.setEstado(EstadoCompra.ANULADA);
	}
}