package es.unex.cum.mdai.motoresbits;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.InetSocketAddress;
import java.net.Socket;

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
