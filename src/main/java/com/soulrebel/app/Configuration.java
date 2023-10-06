package com.soulrebel.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soulrebel.app.errors.GlobalExceptionHandler;
import com.soulrebel.data.InMemoryUserRepository;
import com.soulrebel.domain.UserRepository;
import com.soulrebel.domain.UserService;

public class Configuration {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper ();
    private static final UserRepository USER_REPOSITORY = new InMemoryUserRepository ();
    private static final UserService USER_SERVICE = new UserService (USER_REPOSITORY);
    private static final GlobalExceptionHandler GLOBAL_ERROR_HANDLER = new GlobalExceptionHandler (OBJECT_MAPPER);

    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    public static UserService getUserService() {
        return USER_SERVICE;
    }

    static UserRepository getUserRepository() {
        return USER_REPOSITORY;
    }

    public static GlobalExceptionHandler getErrorHandler() {
        return GLOBAL_ERROR_HANDLER;
    }
}
