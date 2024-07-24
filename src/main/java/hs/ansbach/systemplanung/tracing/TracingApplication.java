package hs.ansbach.systemplanung.tracing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@Slf4j
public class TracingApplication implements ApplicationRunner {

    public static void main(String[] args) {
        SpringApplication.run(TracingApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Command-Line-Arguments: " + String.join("", args.getSourceArgs()));
    }
}
