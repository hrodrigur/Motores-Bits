package es.unex.cum.mdai.motoresbits;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;

@SpringBootApplication
public class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        // Antes de arrancar Spring, comprobamos si la base de datos remota está accesible.
        // Si no lo está, forzamos el uso de H2 en memoria para que la aplicación arranque
        // y pueda servir las páginas HTML.
        boolean useH2 = false;

        // Intentamos obtener la URL configurada (primero System property, luego env var, luego default)
        String configuredUrl = System.getProperty("spring.datasource.url", System.getenv("SPRING_DATASOURCE_URL"));
        if (configuredUrl == null || configuredUrl.isEmpty()) {
            configuredUrl = "jdbc:mariadb://localhost:3306/MotoresBits"; // fallback
        }

        try {
            // Soportamos jdbc:mariadb://host:port/... y jdbc:mysql://host:port/...
            if (configuredUrl.startsWith("jdbc:mariadb://") || configuredUrl.startsWith("jdbc:mysql://")) {
                String after = configuredUrl.substring(configuredUrl.indexOf("//") + 2);
                int slash = after.indexOf('/');
                String hostPort = (slash == -1) ? after : after.substring(0, slash);
                int q = hostPort.indexOf('?');
                if (q != -1) hostPort = hostPort.substring(0, q);

                String host = hostPort;
                int port = 3306; // por defecto
                if (hostPort.contains(":")) {
                    String[] hp = hostPort.split(":", 2);
                    host = hp[0];
                    try {
                        port = Integer.parseInt(hp[1]);
                    } catch (NumberFormatException ignore) {
                    }
                }

                // Intento rápido de conexión TCP con timeout
                try (Socket s = new Socket()) {
                    s.connect(new InetSocketAddress(host, port), 1500);
                    // Si llega aquí, hay conectividad TCP con la base de datos
                    System.out.println("Conectividad detectada en " + host + ":" + port + " — arrancando con datasource configurado.");
                }
            }
        } catch (Exception e) {
            // Si hay cualquier fallo al comprobar la conectividad, usamos H2
            useH2 = true;
        }

        if (useH2) {
            System.out.println("No se detectó la base de datos remota. Se utilizará H2 en memoria para arrancar la aplicación.");
            // Configuraciones mínimas para H2 en memoria
            System.setProperty("spring.datasource.url", "jdbc:h2:mem:motoresbits;DB_CLOSE_DELAY=-1;MODE=MariaDB");
            System.setProperty("spring.datasource.username", "sa");
            System.setProperty("spring.datasource.password", "");
            System.setProperty("spring.datasource.driver-class-name", "org.h2.Driver");
            // Aseguramos que Hibernate use un dialecto H2 si hace falta
            System.setProperty("spring.jpa.database-platform", "org.hibernate.dialect.H2Dialect");
            System.setProperty("spring.jpa.hibernate.ddl-auto", "update");
        }

        // Ejecutar script de esquema si falta la tabla 'categorias' (antes de arrancar JPA/Hibernate)
        try {
            // usamos la URL ya calculada anteriormente (configuredUrl)
            String url = configuredUrl;
            // intentamos obtener credenciales de system properties / env vars
            String user = System.getProperty("spring.datasource.username", System.getenv("SPRING_DATASOURCE_USERNAME"));
            String pass = System.getProperty("spring.datasource.password", System.getenv("SPRING_DATASOURCE_PASSWORD"));

            // si no están en system props/env vars, leemos application.properties desde classpath
            if ((user == null || user.isEmpty()) || (pass == null)) {
                try (java.io.InputStream in = Application.class.getResourceAsStream("/application.properties")) {
                    if (in != null) {
                        java.util.Properties props = new java.util.Properties();
                        props.load(in);
                        if (user == null || user.isEmpty()) user = props.getProperty("spring.datasource.username", user);
                        if (pass == null) pass = props.getProperty("spring.datasource.password", pass);
                    }
                } catch (Exception ignore) {
                    // no crítico: seguimos con lo que tengamos
                }
            }

            if (url != null && !url.isEmpty()) {
                // Cargamos el driver si se especificó
                String driver = System.getProperty("spring.datasource.driver-class-name");
                // si no está, intentar leer de application.properties
                if (driver == null || driver.isEmpty()) {
                    try (java.io.InputStream in = Application.class.getResourceAsStream("/application.properties")) {
                        if (in != null) {
                            java.util.Properties props = new java.util.Properties();
                            props.load(in);
                            driver = props.getProperty("spring.datasource.driver-class-name", driver);
                        }
                    } catch (Exception ignore) {
                    }
                }
                if (driver != null && !driver.isEmpty()) {
                    try {
                        Class.forName(driver);
                    } catch (ClassNotFoundException e) {
                        LOGGER.debug("Driver JDBC no encontrado en classpath: {}", driver);
                    }
                }

                try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                    DatabaseMetaData meta = conn.getMetaData();
                    boolean categoriasExists = false;
                    // probamos varias variantes de nombre (mayúsculas/minúsculas)
                    try (ResultSet rs = meta.getTables(null, null, "categorias", new String[]{"TABLE"})) {
                        if (rs.next()) categoriasExists = true;
                    }
                    if (!categoriasExists) {
                        try (ResultSet rs2 = meta.getTables(null, null, "CATEGORIAS", new String[]{"TABLE"})) {
                            if (rs2.next()) categoriasExists = true;
                        }
                    }

                    if (!categoriasExists) {
                        LOGGER.info("Tabla 'categorias' no encontrada en la BD. Ejecutando script de esquema desde classpath: sql/schema.sql");
                        Resource r = new ClassPathResource("sql/schema.sql");
                        EncodedResource er = new EncodedResource(r);
                        ScriptUtils.executeSqlScript(conn, er);
                        LOGGER.info("Script de esquema ejecutado correctamente (pre-inicialización).");
                    } else {
                        LOGGER.info("Tabla 'categorias' ya existe. No se ejecuta script de esquema.");
                    }
                } catch (Exception e) {
                    LOGGER.warn("No se pudo comprobar/ejecutar el script de esquema antes de arrancar Spring: {}", e.toString());
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error al intentar ejecutar script inicial previo: {}", e.toString());
        }

        // Arrancamos la aplicación Spring
        SpringApplication.run(Application.class, args);

        // Nota: la lógica para detener un contenedor Docker está desactivada por defecto
        // para evitar fallos en entornos donde Docker no está disponible.
        // Si realmente necesita detener el contenedor al cerrar la app, puede activar
        // la propiedad `app.docker.stopOnShutdown=true` en `application.properties`.
        // La comprobación y el shutdown hook se registran aquí sólo si la propiedad está presente.
        // Usamos una propiedad de entorno/argumento para evitar dependencias de contextos Spring
        // dentro del `main`.
        String stopOnShutdown = System.getProperty("app.docker.stopOnShutdown", System.getenv("APP_DOCKER_STOP_ON_SHUTDOWN"));
        if ("true".equalsIgnoreCase(stopOnShutdown)) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    System.out.println("Deteniendo contenedor Docker 'motores_bits'...");
                    ProcessBuilder pb = new ProcessBuilder("docker", "stop", "motores_bits");
                    pb.inheritIO();
                    Process process = pb.start();
                    int exitCode = process.waitFor();

                    if (exitCode == 0) {
                        System.out.println("Contenedor detenido correctamente.");
                    } else {
                        System.err.println("Error al detener el contenedor (código " + exitCode + ").");
                    }
                } catch (Exception e) {
                    LOGGER.error("No se pudo detener el contenedor Docker:", e);
                }
            }));
        }
    }
}
