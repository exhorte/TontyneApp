package sn.isi.tontyn.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import sn.isi.tontyn.dto.ApiError;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/** Traduit les exceptions en reponses JSON coherentes. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 404 - ressource inexistante. */
    @ExceptionHandler({RessourceIntrouvableException.class, NoSuchElementException.class,
                       NoHandlerFoundException.class})
    public ResponseEntity<ApiError> introuvable(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    /** 409 - regle de gestion violee. */
    @ExceptionHandler(ConflitMetierException.class)
    public ResponseEntity<ApiError> conflit(ConflitMetierException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    /** 409 - contrainte d'unicite / d'integrite en base. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integrite(DataIntegrityViolationException ex,
                                              HttpServletRequest req) {
        log.warn("Violation d'integrite : {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT,
                "Operation impossible : contrainte d'integrite violee.", req);
    }

    /** 400 - donnees invalides (validation des DTO annotes @Valid). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex,
                                               HttpServletRequest req) {
        Map<String, String> champs = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> champs.putIfAbsent(e.getField(), e.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(e -> champs.putIfAbsent(e.getObjectName(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(ApiError.validation(
                HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Donnees invalides.", req.getRequestURI(), champs));
    }

    /** 400 - arguments incorrects, JSON illisible, type de parametre errone. */
    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class,
                       HttpMessageNotReadableException.class,
                       MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> requeteInvalide(Exception ex, HttpServletRequest req) {
        String message = (ex instanceof HttpMessageNotReadableException)
                ? "Corps de requete illisible ou mal forme."
                : ex.getMessage();
        return build(HttpStatus.BAD_REQUEST, message, req);
    }

    /** 403 - role insuffisant. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accesRefuse(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN,
                "Acces refuse : privileges insuffisants pour cette operation.", req);
    }

    /** 500 - filet de securite. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> erreurInterne(Exception ex, HttpServletRequest req) {
        log.error("Erreur inattendue sur {} : ", req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Une erreur interne est survenue.", req);
    }

    private ResponseEntity<ApiError> build(HttpStatus statut, String message,
                                           HttpServletRequest req) {
        return ResponseEntity.status(statut).body(ApiError.of(
                statut.value(), statut.getReasonPhrase(), message, req.getRequestURI()));
    }
}
