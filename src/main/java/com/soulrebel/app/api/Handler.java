package com.soulrebel.app.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soulrebel.app.errors.ApplicationExceptions;
import com.soulrebel.app.errors.GlobalExceptionHandler;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import io.vavr.control.Try;

import java.io.InputStream;

public abstract class Handler {

    private final ObjectMapper objectMapper;
    private final GlobalExceptionHandler exceptionHandler;

    protected Handler(ObjectMapper objectMapper, GlobalExceptionHandler exceptionHandler) {
        this.objectMapper = objectMapper;
        this.exceptionHandler = exceptionHandler;
    }

    public void handle(HttpExchange exchange) {
        Try.run (() -> execute (exchange))
                .onFailure (throwable -> exceptionHandler.handle (throwable, exchange));
    }

    protected abstract void execute(HttpExchange exchange) throws Exception;

    protected <T> T readRequest(InputStream is, Class<T> type) {
        return Try.of (() -> objectMapper.readValue (is, type))
                .getOrElseThrow (ApplicationExceptions.invalidRequest ());
    }

    protected <T> byte[] writeResponse(T response) {
        return Try.of (() -> objectMapper.writeValueAsBytes (response))
                .getOrElseThrow (ApplicationExceptions.invalidRequest ());
    }

    protected static Headers getHeader() {
        var headers = new Headers ();
        headers.set (Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);
        return headers;
    }
}
