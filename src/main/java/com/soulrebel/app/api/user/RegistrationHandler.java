package com.soulrebel.app.api.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soulrebel.app.api.Constants;
import com.soulrebel.app.api.Handler;
import com.soulrebel.app.api.ResponseEntity;
import com.soulrebel.app.api.StatusCode;
import com.soulrebel.app.errors.ApplicationExceptions;
import com.soulrebel.app.errors.GlobalExceptionHandler;
import com.soulrebel.domain.NewUser;
import com.soulrebel.domain.UserService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;

public class RegistrationHandler extends Handler {

    private final UserService userService;

    public RegistrationHandler(UserService userService, ObjectMapper objectMapper,
                               GlobalExceptionHandler exceptionHandler) {
        super (objectMapper, exceptionHandler);
        this.userService = userService;
    }

    @Override
    protected void execute(HttpExchange exchange) throws IOException {
        byte[] response;
        if ("POST".equals (exchange.getRequestMethod ())) {
            var entity = doPost (exchange.getRequestBody ());
            exchange.getResponseHeaders ().putAll (entity.getHeaders ());
            exchange.sendResponseHeaders (entity.getStatusCode ().getCode (), 0);
            response = super.writeResponse (entity.getBody ());
        } else {
            throw ApplicationExceptions.methodNotAllowed (
                    "Method " + exchange.getRequestMethod () + " is not allowed for " + exchange.getRequestURI ()).get ();
        }

        var outputStream = exchange.getResponseBody ();
        outputStream.write (response);
        outputStream.close ();
    }

    private ResponseEntity<RegistrationResponse> doPost(InputStream inputStream) {
        var registrationRequest = super.readRequest (inputStream, RegistrationRequest.class);

        var user = NewUser.builder ()
                .login (registrationRequest.getLogin ())
                .password (PasswordEncoder.encodePassword (registrationRequest.getPassword ()))
                .build ();
        final String userId = userService.create (user);

        var response = new RegistrationResponse (userId);

        return new ResponseEntity<> (response,
                getHeader (Constants.CONTENT_TYPE, Constants.APPLICATION_JSON), StatusCode.OK);
    }
}
