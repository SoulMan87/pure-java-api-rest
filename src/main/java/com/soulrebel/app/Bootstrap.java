package com.soulrebel.app;

import com.soulrebel.app.api.user.RegistrationHandler;
import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import static com.soulrebel.app.Configuration.getErrorHandler;
import static com.soulrebel.app.Configuration.getObjectMapper;
import static com.soulrebel.app.Configuration.getUserService;
import static com.soulrebel.app.api.ApiUtils.splitQuery;

public class Bootstrap {

    public static void main(String[] args) throws IOException {
        int serverPort = 8000;
        HttpServer server = HttpServer.create (new InetSocketAddress (serverPort), 0);

        RegistrationHandler registrationHandler = new RegistrationHandler (getUserService (), getObjectMapper (),
                getErrorHandler ());
        server.createContext ("/api/users/register", registrationHandler::handle);

        HttpContext context = server.createContext ("/api/hello", (exchange -> {

            if ("GET".equals (exchange.getRequestMethod ())) {
                var params = splitQuery (exchange.getRequestURI ().getRawQuery ());
                final String noNameText = "Anonymous";
                final String name = params.getOrDefault ("name", List.of (noNameText))
                        .stream ().findFirst ().orElse (noNameText);
                final String respText = String.format ("Hello %s!", name);
                exchange.sendResponseHeaders (200, respText.getBytes ().length);
                var output = exchange.getResponseBody ();
                output.write (respText.getBytes ());
                output.flush ();
            } else {
                exchange.sendResponseHeaders (405, -1);
            }
            exchange.close ();
        }));
        context.setAuthenticator (new BasicAuthenticator ("myrealm") {
            @Override
            public boolean checkCredentials(String username, String password) {
                return username.equals ("admin") && password.equals ("admin");
            }
        });
        server.setExecutor (null);
        server.start ();
    }
}
