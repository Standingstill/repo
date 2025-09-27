package com.ensureback.developer;

import java.io.IOException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/developer")
public class DeveloperPublicController {

    private static final MediaType YAML_MEDIA_TYPE = MediaType.parseMediaType("application/yaml");

    @GetMapping("/docs")
    public ResponseEntity<InputStreamResource> docs() throws IOException {
        ClassPathResource resource = new ClassPathResource("dev/docs.yaml");
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        InputStreamResource body = new InputStreamResource(resource.getInputStream());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(YAML_MEDIA_TYPE);
        headers.setContentDispositionFormData("inline", "ensureback-openapi.yaml");
        return ResponseEntity.ok()
                .headers(headers)
                .body(body);
    }
}