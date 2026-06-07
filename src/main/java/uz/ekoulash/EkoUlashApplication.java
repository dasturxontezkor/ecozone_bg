package uz.ekoulash;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EkoUlashApplication {
    public static void main(String[] args) {
        SpringApplication.run(EkoUlashApplication.class, args);
    }
}
