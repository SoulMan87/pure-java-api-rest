package com.soulrebel.data;

import com.soulrebel.domain.NewUser;
import com.soulrebel.domain.User;
import com.soulrebel.domain.UserRepository;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUserRepository implements UserRepository {

    private static final Map USER_STORE = new ConcurrentHashMap ();

    @Override
    public String create(NewUser newUser) {
        final var id = UUID.randomUUID ().toString ();
        var user = User.builder ()
                .id (id)
                .login (newUser.getLogin ())
                .password (newUser.getPassword ())
                .build ();
        USER_STORE.put (newUser.getLogin (), user);
        return id;
    }
}
