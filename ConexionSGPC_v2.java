import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

/**
 * SGPC - Conexion JDBC a Oracle Autonomous Database (Wallet)
 * SC-504 Lenguajes de Bases de Datos - Avance 2
 *
 * IMPORTANTE: el profe pide que el CRUD se ejecute SIEMPRE a
 * traves de procedimientos almacenados (con sus funciones/cursores
 * de apoyo), nunca con SELECT/INSERT/UPDATE/DELETE escritos
 * directamente en el codigo Java. Por eso esta clase usa
 * CallableStatement para invocar FIDE_CARRERA_LISTAR_SP, en vez de
 * armar un "SELECT ... FROM FIDE_CARRERA_TB" a mano.
 *
 * Requisitos para que esto funcione:
 *  1. Se debe descomprimir el wallet en una carpeta, por ejemplo:
 *     C:\Users\valec\Documents\oracle_wallet
 *  2. Cuando están dentro de esa carpeta deben quedar los archivos:
 *     cwallet.sso, ewallet.p12, sqlnet.ora, tnsnames.ora, ojdbc.properties
 *  3. Editar sqlnet.ora dentro de esa carpeta y cambiar la linea:
 *       WALLET_LOCATION = (SOURCE = (METHOD = file)(METHOD_DATA = (DIRECTORY="?/network/admin")))
 *     por la ruta real donde descomprimieron el wallet.
 *  4. Agregar ojdbc11.jar y oraclepki.jar al proyecto (Libraries en NetBeans)
 *  5. Ajustar TNS_ADMIN, USUARIO, PASSWORD y TNS_ALIAS mas abajo
 */
public class ConexionSGPC {

    // Ruta donde descomprimieron el wallet
    private static final String TNS_ADMIN = "C:/Users/valec/Documents/oracle_wallet";

    // Alias de conexion definido dentro de tnsnames.ora (dentro del wallet) en mi caso ese es el alias
    private static final String TNS_ALIAS = "iiq2016_high";

    // Credenciales del esquema (el usuario/schema donde estan las tablas FIDE_*)
    private static final String USUARIO = "SGPC_ADMIN";
    private static final String PASSWORD = "Fidelitas$2026";

    public static Connection obtenerConexion() throws SQLException {
        System.setProperty("oracle.net.tns_admin", TNS_ADMIN);

        String url = "jdbc:oracle:thin:@" + TNS_ALIAS;

        Properties props = new Properties();
        props.setProperty("user", USUARIO);
        props.setProperty("password", PASSWORD);

        return DriverManager.getConnection(url, props);
    }

    /**
     * Llama al procedimiento FIDE_CARRERA_LISTAR_SP (el que ya tenemos
     * creado en la base de datos) y recorre el cursor que devuelve.
     * NO hay ningun SELECT escrito aqui - todo lo resuelve el
     * procedimiento almacenado.
     */
    public static void listarCarreras(Connection conn) throws SQLException {
        String llamada = "{call FIDE_CARRERA_LISTAR_SP(?)}";

        try (CallableStatement cs = conn.prepareCall(llamada)) {
            cs.registerOutParameter(1, Types.REF_CURSOR);
            cs.execute();

            try (ResultSet rs = (ResultSet) cs.getObject(1)) {
                boolean hayDatos = false;
                while (rs.next()) {
                    hayDatos = true;
                    System.out.println(
                        rs.getInt("ID_CARRERA") + " - " +
                        rs.getString("NOMBRE") + " (" +
                        rs.getString("CODIGO_CARRERA") + ")"
                    );
                }
                if (!hayDatos) {
                    System.out.println("(La tabla FIDE_CARRERA_TB no tiene registros todavia)");
                }
            }
        }
    }

    public static void main(String[] args) {
        try (Connection conn = obtenerConexion()) {
            System.out.println("Conexion exitosa a Oracle Autonomous Database.");

            listarCarreras(conn);

        } catch (SQLException e) {
            System.err.println("Error al conectar con la base de datos:");
            e.printStackTrace();
        }
    }
}
