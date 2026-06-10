package com.hsms.backend.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class HsmsExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(HsmsExceptionHandler.class);

    @ExceptionHandler(HsmsException.class)
    public ResponseEntity<ApiError> hsmsException(HsmsException exception) {
        return ResponseEntity.status(exception.status())
                .body(new ApiError(exception.getMessage(), exception.action(), exception.status(), Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError("Некорректные данные запроса", "Проверьте обязательные поля формы.", 400, Instant.now()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadableRequest(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError("Некорректное тело запроса", "Проверьте JSON и типы передаваемых полей.", 400, Instant.now()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(403)
                .body(new ApiError("Недостаточно прав для операции", "Войдите пользователем с подходящей ролью.", 403, Instant.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> fallback(Exception exception) {
        log.error("Unhandled HSMS exception", exception);
        return ResponseEntity.internalServerError()
                .body(new ApiError("Внутренняя ошибка СУСХ", "Повторите операцию или обратитесь к администратору.", 500, Instant.now()));
    }
}
