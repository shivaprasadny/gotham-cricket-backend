package com.gotham.cricket.exception;

import com.gotham.cricket.dto.ChatErrorResponse;
import com.gotham.cricket.controller.ChatRestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ChatRestController.class)
public class ChatExceptionHandler {

    @ExceptionHandler(ChatNotFoundException.class)
    public ResponseEntity<ChatErrorResponse> handleNotFound(ChatNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "CHAT_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ChatErrorResponse> handleForbidden(AccessDeniedException exception) {
        return response(HttpStatus.FORBIDDEN, "CHAT_FORBIDDEN", exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ChatErrorResponse> handleBadRequest(Exception exception) {
        String message = exception instanceof MethodArgumentNotValidException validationException
                ? validationException.getBindingResult().getAllErrors().stream()
                        .findFirst()
                        .map(error -> error.getDefaultMessage())
                        .orElse("Invalid chat request")
                : exception.getMessage();
        return response(HttpStatus.BAD_REQUEST, "CHAT_BAD_REQUEST", message);
    }

    private ResponseEntity<ChatErrorResponse> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ChatErrorResponse.of(code, message));
    }
}
