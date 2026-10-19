package erp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Clase encargada de abrir y cerrar las conexiones JDBC contra MySQL.
 *
 * Base de datos: erp (la crea el script Azul_DB.sql).
 * Requiere el driver MySQL Connector/J 8.x en el classpath.
 */
public final class Conexion {

    // ------------------------------------------------------------------
    // Parámetros de conexión: cámbialos según tu instalación de MySQL.
    // ------------------------------------------------------------------
    private static final String HOST = "localhost";
    private static final String PUERTO = "3306";
    private static final String BASE_DATOS = "erp";
    private static final String USUARIO = "root";
    private static final String PASSWORD = "";

    private static final String URL = "jdbc:mysql://" + HOST + ":" + PUERTO + "/" + BASE_DATOS
            + "?useSSL=false"
            + "&allowPublicKeyRetrieval=true"
            + "&serverTimezone=UTC"
            + "&useUnicode=true"
            + "&characterEncoding=UTF-8";

    // Registra el driver una sola vez al cargar la clase.
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("No se encontró el driver de MySQL (mysql-connector-j). "
                    + "Añádelo al classpath del proyecto.");
        }
    }

    /** Clase de utilidades: no se instancia. */
    private Conexion() {
    }

    /**
     * Devuelve una conexión nueva. Quien la use debe cerrarla
     * (preferiblemente con try-with-resources).
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, PASSWORD);
    }

    /**
     * Cierra varios recursos JDBC ignorando los nulos.
     * Ejemplo: Conexion.cerrar(rs, ps, cn);
     */
    public static void cerrar(AutoCloseable... recursos) {
        for (AutoCloseable recurso : recursos) {
            if (recurso != null) {
                try {
                    recurso.close();
                } catch (Exception e) {
                    System.err.println("Error al cerrar recurso: " + e.getMessage());
                }
            }
        }
    }

    /** Comprueba que la base de datos responde. */
    public static boolean probarConexion() {
        try (Connection cn = getConnection()) {
            return cn != null && !cn.isClosed();
        } catch (SQLException e) {
            System.err.println("Error de conexión: " + e.getMessage());
            return false;
        }
    }

    /** Prueba rápida de conexión. */
    public static void main(String[] args) {
        if (probarConexion()) {
            System.out.println("Conexión correcta con la base de datos '" + BASE_DATOS + "'.");
        } else {
            System.out.println("No se pudo conectar con la base de datos '" + BASE_DATOS + "'.");
        }
    }
}
