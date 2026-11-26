package com.feiniaojin.gracefulresponse.boot4;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = Boot4CompatibilityTest.Application.class, properties = {
        "graceful-response.response-style=0",
        "graceful-response.default-success-code=SUCCESS",
        "graceful-response.default-success-msg=done",
        "graceful-response.default-error-code=FAIL",
        "graceful-response.default-http-status-code-on-error=500",
        "graceful-response.default-validate-error-code=INVALID",
        "graceful-response.default-http-status-code-on-validation-error=400"
})
@AutoConfigureMockMvc
class ResponseConfigurationTest {
    @Autowired MockMvc mvc;
    @Test void keepsStyle0AndCustomSuccessStatus() throws Exception {
        mvc.perform(get("/boot4/object")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value("SUCCESS"))
                .andExpect(jsonPath("$.status.msg").value("done"))
                .andExpect(jsonPath("$.payload.name").value("boot4"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
    @Test void honorsGlobalErrorStatusCode() throws Exception {
        mvc.perform(get("/boot4/error")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status.code").value("FAIL"));
    }
    @Test void honorsGlobalValidationStatusCode() throws Exception {
        mvc.perform(post("/boot4/validate").contentType("application/json").content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status.code").value("INVALID"));
    }
}
