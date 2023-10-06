package com.soulrebel.app.errors;

public class ResourceNotFoundException extends ApplicationException{
    ResourceNotFoundException(int code, String message) {
        super (code, message);
    }
}
