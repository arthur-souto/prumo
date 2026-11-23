package io.github.arthursouto.prumo.infrastructure.adapter.in.web;

import io.github.arthursouto.prumo.domain.exceptions.BusinessRuleException;
import io.github.arthursouto.prumo.domain.exceptions.ConflictException;
import io.github.arthursouto.prumo.domain.exceptions.DomainException;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Traduz exceções em respostas {@code application/problem+json} (RFC 9457). Falhas de validação trazem a
 * propriedade {@code errors} com campo e mensagem. Falhas inesperadas viram 500 sem expor detalhes internos.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomain(DomainException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setProperty("errors", exception.getConstraintViolations().stream()
                .map(violation -> error(violation.getPropertyPath().toString(), violation.getMessage()))
                .sorted(Comparator.comparing(error -> error.get("field")))
                .toList());

        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        LOGGER.error("Unexpected error", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(exception, body, headers, status, request);

        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            if (exception instanceof BindException bindException) {
                problem.setProperty("errors", fieldErrors(bindException));
            }
            if (exception instanceof HandlerMethodValidationException validationException) {
                problem.setProperty("errors", parameterErrors(validationException));
            }
        }

        return response;
    }

    private static List<Map<String, String>> fieldErrors(BindException exception) {
        return exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error(error.getField(), String.valueOf(error.getDefaultMessage())))
                .sorted(Comparator.comparing(error -> error.get("field")))
                .toList();
    }

    private static List<Map<String, String>> parameterErrors(HandlerMethodValidationException exception) {
        List<Map<String, String>> errors = new ArrayList<>();

        for (ParameterValidationResult result : exception.getValueResults()) {
            String parameter = String.valueOf(result.getMethodParameter().getParameterName());

            result.getResolvableErrors().forEach(error -> errors.add(error(parameter, String.valueOf(error.getDefaultMessage()))));
        }

        for (ParameterErrors result : exception.getBeanResults()) {
            result.getFieldErrors().forEach(error -> errors.add(error(error.getField(), String.valueOf(error.getDefaultMessage()))));
        }

        errors.sort(Comparator.comparing(error -> error.get("field")));

        return errors;
    }

    private static Map<String, String> error(String field, String message) {
        Map<String, String> error = new LinkedHashMap<>();

        error.put("field", field);
        error.put("message", message);

        return error;
    }

}
