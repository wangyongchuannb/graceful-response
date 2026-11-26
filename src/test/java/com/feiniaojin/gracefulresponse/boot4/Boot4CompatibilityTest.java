package com.feiniaojin.gracefulresponse.boot4;

import com.feiniaojin.gracefulresponse.EnableGracefulResponse;
import com.feiniaojin.gracefulresponse.GracefulResponseException;
import com.feiniaojin.gracefulresponse.advice.AdviceSupport;
import com.feiniaojin.gracefulresponse.api.ExcludeFromGracefulResponse;
import com.feiniaojin.gracefulresponse.api.ResponseFactory;
import com.feiniaojin.gracefulresponse.api.ValidationStatusCode;
import com.feiniaojin.gracefulresponse.data.Response;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = Boot4CompatibilityTest.Application.class, properties = {
        "graceful-response.exclude-urls[0]=/boot4/excluded-url",
        "graceful-response.default-validate-error-code=VALIDATION"
})
@AutoConfigureMockMvc
class Boot4CompatibilityTest {
    @Autowired MockMvc mvc;
    @Autowired AdviceSupport adviceSupport;

    @Test void recognizesJackson3Converter() {
        assertThat(adviceSupport.isJsonHttpMessageConverter(JacksonJsonHttpMessageConverter.class)).isTrue();
    }
    @Test void wrapsObjectResponse() throws Exception {
        mvc.perform(get("/boot4/object")).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.name").value("boot4"));
    }
    @Test void wrapsVoidResponse() throws Exception {
        mvc.perform(get("/boot4/void")).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }
    @Test void wrapsNullResponse() throws Exception {
        mvc.perform(get("/boot4/null")).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }
    @Test void doesNotWrapResponseTwice() throws Exception {
        mvc.perform(get("/boot4/response")).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.name").value("original"))
                .andExpect(jsonPath("$.data.status").doesNotExist());
    }
    @Test void preservesResponseEntityStatusAndHeaders() throws Exception {
        mvc.perform(get("/boot4/entity")).andExpect(status().isCreated())
                .andExpect(header().string("X-Boot", "4"))
                .andExpect(jsonPath("$.data.name").value("entity"));
    }
    @Test void preservesPlainText() throws Exception {
        mvc.perform(get("/boot4/text")).andExpect(status().isOk()).andExpect(content().string("plain"));
    }
    @Test void honorsExclusionAnnotation() throws Exception {
        mvc.perform(get("/boot4/excluded")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("raw")).andExpect(jsonPath("$.status").doesNotExist());
    }
    @Test void honorsUrlExclusion() throws Exception {
        mvc.perform(get("/boot4/excluded-url")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("raw")).andExpect(jsonPath("$.status").doesNotExist());
    }
    @Test void handlesBusinessException() throws Exception {
        mvc.perform(get("/boot4/business-error")).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("BUSINESS"))
                .andExpect(jsonPath("$.msg").value("business failed"));
    }
    @Test void handlesUnexpectedException() throws Exception {
        mvc.perform(get("/boot4/error")).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1"))
                .andExpect(jsonPath("$.msg").value("error"));
    }
    @Test void handlesRequestBodyValidation() throws Exception {
        mvc.perform(post("/boot4/validate").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.msg").value("name required"));
    }
    @Test void honorsValidationAnnotation() throws Exception {
        mvc.perform(post("/boot4/validate-annotation").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("FIELD"));
    }
    @Test void handlesMethodParameterValidation() throws Exception {
        mvc.perform(get("/boot4/method-validation").param("count", "0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.msg").value("count must be positive"));
    }
    @Test void honorsMethodValidationAnnotation() throws Exception {
        mvc.perform(get("/boot4/method-validation-annotation").param("count", "0"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("METHOD"));
    }
    @Test void treatsInvalidReturnValueAsServerError() throws Exception {
        mvc.perform(get("/boot4/invalid-return"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("1"))
                .andExpect(jsonPath("$.msg").value("error"));
    }
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableGracefulResponse
    @Import(Endpoints.class)
    static class Application { }

    @RestController
    @RequestMapping("/boot4")
    static class Endpoints {
        private final ResponseFactory factory;
        Endpoints(ResponseFactory factory) { this.factory = factory; }
        @GetMapping("/object") Map<String, String> object() { return Map.of("name", "boot4"); }
        @GetMapping("/void") void empty() { }
        @GetMapping("/null") Map<String, String> nullBody() { return null; }
        @GetMapping("/response") Response response() { return factory.newSuccessInstance(Map.of("name", "original")); }
        @GetMapping("/entity") ResponseEntity<Map<String, String>> entity() {
            return ResponseEntity.status(201).header("X-Boot", "4").body(Map.of("name", "entity"));
        }
        @GetMapping(value = "/text", produces = MediaType.TEXT_PLAIN_VALUE) String text() { return "plain"; }
        @ExcludeFromGracefulResponse
        @GetMapping("/excluded") Map<String, String> excluded() { return Map.of("name", "raw"); }
        @GetMapping("/excluded-url") Map<String, String> excludedUrl() { return Map.of("name", "raw"); }
        @GetMapping("/business-error") Object businessError() { throw new GracefulResponseException("BUSINESS", "business failed"); }
        @GetMapping("/error") Object error() { throw new IllegalStateException("private detail"); }
        @PostMapping("/validate") Object validate(@Valid @RequestBody Input input) { return input; }
        @ValidationStatusCode(code = "FIELD", httpStatusCode = 422)
        @PostMapping("/validate-annotation") Object validateAnnotation(@Valid @RequestBody Input input) { return input; }
        @GetMapping("/method-validation") Object methodValidation(
                @RequestParam @Min(value = 1, message = "count must be positive") int count) { return count; }
        @ValidationStatusCode(code = "METHOD", httpStatusCode = 422)
        @GetMapping("/method-validation-annotation") Object methodValidationAnnotation(
                @RequestParam @Min(value = 1, message = "count must be positive") int count) { return count; }
        @NotNull(message = "private server contract")
        @GetMapping("/invalid-return") Object invalidReturn() { return null; }
    }
    public record Input(@NotBlank(message = "name required") String name) { }
}

