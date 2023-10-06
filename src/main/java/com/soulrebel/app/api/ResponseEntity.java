package com.soulrebel.app.api;

import com.sun.net.httpserver.Headers;
import lombok.Value;

@Value
public class ResponseEntity<T> {

    T body;
    Headers headers;
    StatusCode statusCode;
}
