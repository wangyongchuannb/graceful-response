package com.feiniaojin.gracefulresponse.boot4;

import com.feiniaojin.gracefulresponse.EnableGracefulResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = ReleasedExceptionTest.Application.class, properties = {
        "graceful-response.exclude-exception-types[0]=java.lang.IllegalArgumentException",
        "spring.jackson.property-naming-strategy=SNAKE_CASE"
})
@AutoConfigureMockMvc
class ReleasedExceptionTest {
    @Autowired MockMvc mvc;

    @Test void usesApplicationJsonConfigurationForNormalResponse() throws Exception {
        mvc.perform(get("/release/normal")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.error_message").value("normal"));
    }
    @Test void releasesExceptionWithoutWrappingAndKeepsApplicationJsonConfiguration() throws Exception {
        mvc.perform(get("/release/body")).andExpect(status().isOk())
                .andExpect(jsonPath("$.error_message").value("released"))
                .andExpect(jsonPath("$.code").doesNotExist());
    }
    @Test void releasesResponseEntityWithStatusAndHeaders() throws Exception {
        mvc.perform(get("/release/entity")).andExpect(status().isConflict())
                .andExpect(header().string("X-Released", "yes"))
                .andExpect(jsonPath("$.error_message").value("entity"))
                .andExpect(jsonPath("$.code").doesNotExist());
    }
    @Test void releasesPlainTextUsingApplicationConverters() throws Exception {
        mvc.perform(get("/release/text")).andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("released text"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableGracefulResponse
    @Import({Endpoints.class, OutsideAdvice.class})
    static class Application { }

    @RestController
    @RequestMapping("/release")
    static class Endpoints {
        @GetMapping("/normal") ErrorDetail normal() { return new ErrorDetail("normal"); }
        @GetMapping("/body") Object body() { throw new IllegalArgumentException("body"); }
        @GetMapping("/entity") Object entity() { throw new EntityException(); }
        @GetMapping("/text") Object text() { throw new TextException(); }
    }

    @RestControllerAdvice
    @Order(1000)
    static class OutsideAdvice {
        @ExceptionHandler(IllegalArgumentException.class)
        ErrorDetail body(IllegalArgumentException exception) { return new ErrorDetail("released"); }
        @ExceptionHandler(EntityException.class)
        ResponseEntity<ErrorDetail> entity(EntityException exception) {
            return ResponseEntity.status(409).header("X-Released", "yes").body(new ErrorDetail("entity"));
        }
        @ExceptionHandler(TextException.class)
        ResponseEntity<String> text(TextException exception) {
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body("released text");
        }
    }
    public record ErrorDetail(String errorMessage) { }
    static class EntityException extends IllegalArgumentException { }
    static class TextException extends IllegalArgumentException { }
}
