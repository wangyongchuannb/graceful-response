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
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
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
    @Test void preservesResponseBodyAdviceForPlainControllerAdvice() throws Exception {
        mvc.perform(get("/release/plain-advice")).andExpect(status().isOk())
                .andExpect(header().string("X-Processed", "yes"))
                .andExpect(jsonPath("$.error_message").value("plain advice"));
    }
    @Test void selectsJsonExceptionHandlerForJsonRequest() throws Exception {
        mvc.perform(get("/release/media").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error_message").value("json handler"));
    }
    @Test void selectsHtmlExceptionHandlerForHtmlRequest() throws Exception {
        mvc.perform(get("/release/media").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string("<p>html handler</p>"));
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableGracefulResponse
    @Import({Endpoints.class, OutsideAdvice.class, OutOfScopeAdvice.class, PlainAdvice.class, HeaderAdvice.class, MediaAdvice.class})
    static class Application { }

    @RestController
    @RequestMapping("/release")
    static class Endpoints {
        @GetMapping("/normal") ErrorDetail normal() { return new ErrorDetail("normal"); }
        @GetMapping("/body") Object body() { throw new IllegalArgumentException("body"); }
        @GetMapping("/entity") Object entity() { throw new EntityException(); }
        @GetMapping("/text") Object text() { throw new TextException(); }
        @GetMapping("/plain-advice") Object plainAdvice() { throw new PlainException(); }
        @GetMapping("/media") Object media() { throw new MediaException(); }
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
    @RestControllerAdvice(assignableTypes = UnrelatedController.class)
    @Order(900)
    static class OutOfScopeAdvice {
        @ExceptionHandler(IllegalArgumentException.class)
        ErrorDetail handle(IllegalArgumentException exception) { return new ErrorDetail("wrong advice"); }
    }
    static class UnrelatedController { }
    @ControllerAdvice
    @Order(950)
    static class PlainAdvice {
        @ExceptionHandler(PlainException.class)
        ErrorDetail handle(PlainException exception) { return new ErrorDetail("plain advice"); }
    }
    @RestControllerAdvice
    @Order(940)
    static class MediaAdvice {
        @ExceptionHandler(value = MediaException.class, produces = MediaType.APPLICATION_JSON_VALUE)
        ErrorDetail json(MediaException exception) { return new ErrorDetail("json handler"); }
        @ExceptionHandler(value = MediaException.class, produces = MediaType.TEXT_HTML_VALUE)
        ResponseEntity<String> html(MediaException exception) {
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body("<p>html handler</p>");
        }
    }
    @ControllerAdvice
    static class HeaderAdvice implements ResponseBodyAdvice<Object> {
        @Override public boolean supports(MethodParameter type, Class<? extends HttpMessageConverter<?>> converter) {
            return true;
        }
        @Override public Object beforeBodyWrite(Object body, MethodParameter type, MediaType media,
                Class<? extends HttpMessageConverter<?>> converter, ServerHttpRequest request, ServerHttpResponse response) {
            response.getHeaders().set("X-Processed", "yes");
            return body;
        }
    }
    public record ErrorDetail(String errorMessage) { }
    static class EntityException extends IllegalArgumentException { }
    static class TextException extends IllegalArgumentException { }
    static class PlainException extends IllegalArgumentException { }
    static class MediaException extends IllegalArgumentException { }
}
