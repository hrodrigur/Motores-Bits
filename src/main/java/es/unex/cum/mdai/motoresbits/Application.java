package es.unex.cum.mdai.motoresbits;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
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
                System.err.println("No se pudo detener el contenedor Docker:");
                e.printStackTrace();
            }
        }));
    }
}

