package com.acc.somsomparty.global.exception;

import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class CustomExceptionHandler {

    @ExceptionHandler(CustomException.class)
    protected ResponseEntity<ErrorResponseEntity> handleCustomException(CustomException e) {
        return ErrorResponseEntity.toResponseEntity(e.getErrorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponseEntity> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ErrorResponseEntity errorResponse = ErrorResponseEntity.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Validation failed")
                .errors(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Producer가 3번의 재시도를 모두 실패했을 때 (네트워크/연결 오류)
     * 이 핸들러가 AmqpException을 캐치합니다.
     */
    @ExceptionHandler(AmqpException.class)
    protected ResponseEntity<ErrorResponseEntity> handleAmqpException(AmqpException e) {

        ErrorResponseEntity errorResponse = ErrorResponseEntity.builder()
                .status(HttpStatus.SERVICE_UNAVAILABLE.value()) // 503 (서비스 일시 장애)
                .message("일시적인 네트워크 오류가 발생했습니다. 잠시 후 다시 시도해주세요.") // 2. 사용자 친화적 메시지
                .errors(Map.of("error", "RabbitMQ connection failed after 3 retries")) // 3. (선택) 개발자용 로그
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponseEntity> handleAllExceptions(Exception e) {
        ErrorResponseEntity errorResponse = ErrorResponseEntity.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("An unexpected error occurred")
                .errors(Map.of("error", e.getMessage()))
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}