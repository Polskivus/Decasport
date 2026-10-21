package modelo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Usuario del ERP (tabla Usuario). Es el nodo de auditoria: queda registrado
 * en cada venta y compra que hace.
 *
 * Nunca se guarda la contrasena en claro, solo su hash. Mientras los datos
 * esten en memoria se usa SHA-256 con sal; al pasar a MySQL el diseno pide
 * BCrypt (libreria jBCrypt), que solo cambia hashear() y comprobarPassword().
 */
public class Usuario implements Entidad {

	public static final int MIN_PASSWORD = 6;

	private int id; // PK AUTO_INCREMENT
	private String username; // VARCHAR(50) UQ
	private String passwordHash; // VARCHAR(255)
	private int rolId; // FK -> Rol.id
	private boolean activo = true; // TINYINT(1), por defecto 1

	public Usuario() {
	}

	public Usuario(String username, String password, int rolId) {
		setUsername(username);
		setPassword(password);
		setRolId(rolId);
	}

	// ------------------------------------------------------------------
	// Contrasena
	// ------------------------------------------------------------------
	/** Guarda el hash de la contrasena indicada (nunca la contrasena). */
	public void setPassword(String password) {
		if (password == null || password.length() < MIN_PASSWORD) {
			throw new ErpException("La contrasena debe tener al menos " + MIN_PASSWORD + " caracteres");
		}
		this.passwordHash = hashear(password, nuevaSal());
	}

	/** Devuelve true si la contrasena coincide con el hash guardado. */
	public boolean comprobarPassword(String password) {
		if (password == null || passwordHash == null || passwordHash.indexOf('$') < 0) {
			return false;
		}
		String sal = passwordHash.substring(0, passwordHash.indexOf('$'));
		byte[] esperado = passwordHash.getBytes(StandardCharsets.UTF_8);
		byte[] calculado = hashear(password, sal).getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(esperado, calculado);
	}

	private static String nuevaSal() {
		byte[] sal = new byte[16];
		new SecureRandom().nextBytes(sal);
		return Base64.getEncoder().encodeToString(sal);
	}

	// Formato guardado: sal$hash (ambos en Base64)
	private static String hashear(String password, String sal) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(sal.getBytes(StandardCharsets.UTF_8));
			byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
			return sal + "$" + Base64.getEncoder().encodeToString(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 no disponible", e);
		}
	}

	// ------------------------------------------------------------------
	// Getters y setters
	// ------------------------------------------------------------------
	@Override
	public int getId() {
		return id;
	}

	@Override
	public void setId(int id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = Validar.texto(username, "usuario", 50, true);
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	/** Solo para cargar un hash ya calculado (por ejemplo, desde la BD). */
	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public int getRolId() {
		return rolId;
	}

	public void setRolId(int rolId) {
		this.rolId = Validar.referencia(rolId, "rol");
	}

	public boolean isActivo() {
		return activo;
	}

	public void setActivo(boolean activo) {
		this.activo = activo;
	}

	@Override
	public String toString() {
		return username + " (id " + id + ")";
	}
}
