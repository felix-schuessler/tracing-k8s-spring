package hs.ansbach.systemplanung.tracing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@RequiredArgsConstructor
@RestController
@Slf4j
public class RequestController {

    @Value("${downstream:}") private String downstream;
    @Value("${spring.application.name:}") private String name;
    private final RestTemplate restTemplate;

    @RequestMapping(path = "/")
    public String index() {
        return "";
    }

    @RequestMapping(path = "/generateTrace")
    public String generateTrace() {

        if (this.downstream.isEmpty()) return "Hello from " + this.name;
            Arrays.stream(this.downstream.split("\\.")).forEach(this::callDownstreamService);
        return "Hello from " + this.name;
    }

    private void callDownstreamService(final String service) {
        final String URL = "http://" + service + "-tracing" + ":80/generateTrace";
        final ResponseEntity<String> response = restTemplate.getForEntity(URL, String.class);
        log.info("Called Service: " + URL + " Received Response: " + response.getBody());
    }
}
