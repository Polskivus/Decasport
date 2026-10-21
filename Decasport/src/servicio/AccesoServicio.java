package servicio;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import datos.BaseDatosMemoria;
import modelo.ErpException;
import modelo.Rol;
import modelo.Usuario;

/**
 * Paquete 1: Acceso. Inicio de sesion, gestion de usuarios y permisos por rol.
 */
public class AccesoServicio {

	// Modulos que puede usar cada rol (ADMIN puede usarlos todos)
	private static final Map<String, Set<Modulo>> PERMISOS = new HashMap<>();
	static {
		PERMISOS.put(Rol.ADMIN, EnumSet.allOf(Modulo.class));
		PERMISOS.put(Rol.VENTAS, EnumSet.of(Modulo.CLIENTES, Modulo.VENTAS, Modulo.DOCUMENTOS));
		PERMISOS.put(Rol.COMPRAS, EnumSet.of(Modulo.PROVEEDORES, Modulo.COMPRAS, Modulo.INVENTARIO));
		PERMISOS.put(Rol.ALMACEN, EnumSet.of(Modulo.INVENTARIO, Modulo.COMPRAS));
		PERMISOS.put(Rol.CONTABILIDAD, EnumSet.of(Modulo.DOCUMENTOS, Modulo.VENTAS, Modulo.COMPRAS));
	}

	private final BaseDatosMemoria bd;

	public AccesoServicio(BaseDatosMemoria bd) {
		this.bd = bd;
	}

	// ------------------------------------------------------------------
	// Sesion y permisos
	// ------------------------------------------------------------------
	/** Devuelve el usuario si el nombre y la contrasena son correctos. */
	public Usuario login(String username, String password) {
		Usuario usuario = buscarPorUsername(username);
		// Mismo mensaje si no existe o si falla la contrasena, para no dar pistas
		if (usuario == null || !usuario.comprobarPassword(password)) {
			throw new ErpException("Usuario o contrasena incorrectos");
		}
		if (!usuario.isActivo()) {
			throw new ErpException("El usuario esta desactivado");
		}
		return usuario;
	}

	public boolean puedeAcceder(Usuario usuario, Modulo modulo) {
		Set<Modulo> modulos = PERMISOS.get(rolDe(usuario).getNombre());
		return modulos != null && modulos.contains(modulo);
	}

	/** Al confirmar una venta se le ofrece emitir albaran y factura (solo ADMIN y VENTAS). */
	public boolean emiteDocumentosAlConfirmar(Usuario usuario) {
		String rol = rolDe(usuario).getNombre();
		return rol.equals(Rol.ADMIN) || rol.equals(Rol.VENTAS);
	}

	public Rol rolDe(Usuario usuario) {
		return bd.roles.obtener(usuario.getRolId());
	}

	public List<Rol> listarRoles() {
		return bd.roles.todos();
	}

	// ------------------------------------------------------------------
	// Usuarios
	// ------------------------------------------------------------------
	public List<Usuario> listarUsuarios() {
		return bd.usuarios.todos();
	}

	public Usuario obtenerUsuario(int id) {
		return bd.usuarios.obtener(id);
	}

	public Usuario buscarPorUsername(String username) {
		if (username == null) {
			return null;
		}
		for (Usuario u : bd.usuarios.todos()) {
			if (u.getUsername().equalsIgnoreCase(username.trim())) {
				return u;
			}
		}
		return null;
	}

	public Usuario crearUsuario(String username, String password, int rolId) {
		bd.roles.obtener(rolId); // FK: el rol debe existir
		Usuario nuevo = new Usuario(username, password, rolId);
		if (buscarPorUsername(nuevo.getUsername()) != null) {
			throw new ErpException("Ya existe el usuario " + nuevo.getUsername());
		}
		return bd.usuarios.insertar(nuevo);
	}

	public void cambiarPassword(int usuarioId, String nueva) {
		obtenerUsuario(usuarioId).setPassword(nueva);
	}

	public void cambiarRol(int usuarioId, int rolId, Usuario actual) {
		bd.roles.obtener(rolId);
		if (usuarioId == actual.getId()) {
			throw new ErpException("No puedes cambiar tu propio rol");
		}
		obtenerUsuario(usuarioId).setRolId(rolId);
	}

	public void activar(int usuarioId, boolean activo, Usuario actual) {
		if (!activo && usuarioId == actual.getId()) {
			throw new ErpException("No puedes desactivar tu propio usuario");
		}
		obtenerUsuario(usuarioId).setActivo(activo);
	}

	/** Solo se borran usuarios sin ventas ni compras; si tienen, se desactivan. */
	public void eliminarUsuario(int usuarioId, Usuario actual) {
		obtenerUsuario(usuarioId);
		if (usuarioId == actual.getId()) {
			throw new ErpException("No puedes eliminar tu propio usuario");
		}
		if (bd.ventas.existe(v -> v.getUsuarioId() == usuarioId)
				|| bd.compras.existe(c -> c.getUsuarioId() == usuarioId)) {
			throw new ErpException("El usuario tiene ventas o compras registradas; desactivalo en su lugar");
		}
		bd.usuarios.eliminar(usuarioId);
	}
}