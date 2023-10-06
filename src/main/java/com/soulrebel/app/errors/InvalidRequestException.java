package com.soulrebel.app.errors;

public class InvalidRequestException extends ApplicationException {
    InvalidRequestException(int code, String message) {
        super (code, message);
    }
}
