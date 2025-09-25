package com.app84soft.check_in.exceptions;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Setter
@Getter
public class BusinessException extends RuntimeException {

    private HttpStatus status = HttpStatus.BAD_REQUEST;

    private Object data;

    private String[] errors = new String[]{};

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public BusinessException(String message, HttpStatus status, String[] errors) {
        super(message);
        this.status = status;
        this.errors = errors;
    }

    public BusinessException(Object data, String message) {
        super(message);
        this.data = data;
    }

    public BusinessException(Object data, String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.data = data;
    }

    public BusinessException(Object data, String message, HttpStatus status, String[] errors) {
        super(message);
        this.status = status;
        this.errors = errors;
        this.data = data;
    }

}
