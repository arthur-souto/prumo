package io.github.arthursouto.prumo.infrastructure.adapter.in.web;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The error answers that no controller test reaches: a controller made for the test throws each kind of failure, so
 * the status, the detail and above all what must not leak to the client are checked in one place.
 */
class GlobalExceptionHandlerTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    record Draft(@NotBlank String title, @Size(max = 3) String code) {
    }

    @RestController
    static class FailingController {

        @GetMapping("/integrity")
        void integrity() {
            throw new DataIntegrityViolationException("duplicate key value violates unique constraint \"uq_secret_table\"");
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("connection string with password=hunter2");
        }

        @GetMapping("/constraints")
        void constraints() {
            throw new ConstraintViolationException(VALIDATOR.validate(new Draft(" ", "toolong")));
        }

        @GetMapping("/parameters")
        void parameters(@RequestParam @Max(100) int size) {
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void aDatabaseConstraintIsAConflictWithoutNamingTheConstraint() throws Exception {
        mockMvc.perform(get("/integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("The request conflicts with existing data"))
                .andExpect(content().string(not(containsString("uq_secret_table"))));
    }

    @Test
    void anUnexpectedErrorIsA500WithoutItsMessage() throws Exception {
        mockMvc.perform(get("/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Unexpected error"))
                .andExpect(content().string(not(containsString("hunter2"))));
    }

    @Test
    void constraintViolationsListEachFieldInOrder() throws Exception {
        mockMvc.perform(get("/constraints"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[0].field").value("code"))
                .andExpect(jsonPath("$.errors[1].field").value("title"));
    }

    @Test
    void anInvalidRequestParameterIsReportedByName() throws Exception {
        mockMvc.perform(get("/parameters").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("size"))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty());
    }
}
