## Creating a REST API in Java Without Utilizing Frameworks

This is a demonstration application crafted in Java 11, harnessing the power of the
[`jdk.httpserver`](https://docs.oracle.com/javase/10/docs/api/com/sun/net/httpserver/package-summary.html) module and supplemented with select Java libraries, including [vavr](http://www.vavr.io/), [lombok](https://projectlombok.org/).

## Origins of this Endeavor
As a dedicated Spring developer in my day-to-day work, I've become deeply accustomed to the framework. However, I recently found myself contemplating what it would be like to temporarily set Spring aside and embark on the journey of crafting a pure Java application from the ground up.

The idea of this endeavor intrigued me, not only from a learning perspective but also as a means of rejuvenation in my development routine. As I delved into this undertaking, I encountered numerous instances where I yearned for the convenient features Spring readily offers.

In such moments, rather than resorting to the familiar Spring toolbox, I challenged myself to reconsider and handcraft those functionalities. It became evident that for real-world business scenarios, I would likely lean towards using Spring to save time and effort rather than reinventing the wheel.

Nonetheless, I found this exercise to be an incredibly enriching experience, one that broadened my understanding of Java development and provided valuable insights into the inner workings of the Spring framework.

## Commencement
I will guide you through each step of this exercise, although I won't always provide the complete code within the text. However, you can easily access the code for each step.

The starting point for this journey is an empty Application main class:

## First endpoint

The starting point of the web application is `com.sun.net.httpserver.HttpServer` class.
The most simple `/api/hello` endpoint could look as below:

```java
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

class Application {

    public static void main(String[] args) throws IOException {
        int serverPort = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(serverPort), 0);
        server.createContext("/api/hello", (exchange -> {
            String respText = "Hello!";
            exchange.sendResponseHeaders(200, respText.getBytes().length);
            OutputStream output = exchange.getResponseBody();
            output.write(respText.getBytes());
            output.flush();
            exchange.close();
        }));
        server.setExecutor(null); // creates a default executor
        server.start();
    }
}
```
Upon executing the main program, it will initiate a web server on port `8000`, making our inaugural endpoint accessible. This initial endpoint is quite straightforward; it merely responds with a cheerful "Hello!" message. You can interact with it, for instance, by using the curl command:

```bash
curl localhost:8000/api/hello
```


## Facilitating Varied HTTP Methods
Our initial endpoint functions perfectly, but you'll observe that it responds identically regardless of the HTTP method employed. For instance:

```bash
curl -X POST localhost:8000/api/hello
curl -X PUT localhost:8000/api/hello
```

One of the initial challenges when constructing the API from scratch, devoid of a framework's assistance, is the requirement to incorporate our custom code to differentiate between HTTP methods. For example:

```java
        server.createContext("/api/hello", (exchange -> {

            if ("GET".equals(exchange.getRequestMethod())) {
                String respText = "Hello!";
                exchange.sendResponseHeaders(200, respText.getBytes().length);
                OutputStream output = exchange.getResponseBody();
                output.write(respText.getBytes());
                output.flush();
            } else {
                exchange.sendResponseHeaders(405, -1);// 405 Method Not Allowed
            }
            exchange.close();
        }));
```

Now, let's make another request:
```bash
curl -v -X POST localhost:8000/api/hello
```
here's what the response will look like:

```bash
> POST /api/hello HTTP/1.1
> Host: localhost:8000
> User-Agent: curl/7.61.0
> Accept: */*
> 
< HTTP/1.1 405 Method Not Allowed
```

Additionally, there are a couple of important considerations to keep in mind, such as ensuring that we flush the output and close the exchange each time we return from the API. When working with Spring, these details are handled automatically, sparing us from having to worry about them.

## Parsing Request Parameters
Parsing request parameters is yet another "feature" that we'll need to implement ourselves, as opposed to relying on a framework's built-in capabilities. Suppose we desire our hello API to respond with a customized greeting based on a name provided as a parameter, for example:

```bash
curl localhost:8000/api/hello?name=Marcin

Hello Marcin!

```
We can handle parameter parsing using a method like:

```java
public static Map<String, List<String>> splitQuery(String query) {
        if (query == null || "".equals(query)) {
            return Collections.emptyMap();
        }

        return Pattern.compile("&").splitAsStream(query)
            .map(s -> Arrays.copyOf(s.split("="), 2))
            .collect(groupingBy(s -> decode(s[0]), mapping(s -> decode(s[1]), toList())));

    }
```

here's how we can utilize it:

```java
 Map<String, List<String>> params = splitQuery(exchange.getRequestURI().getRawQuery());
String noNameText = "Anonymous";
String name = params.getOrDefault("name", List.of(noNameText)).stream().findFirst().orElse(noNameText);
String respText = String.format("Hello %s!", name);
           
```

Likewise, if we intend to utilize path parameters, for instance:

```bash
curl localhost:8000/api/items/1
```
To retrieve an item with a specific ID, such as 1, we must manually parse the path to extract the ID, and this process can become rather cumbersome.

## Securing the Endpoint
In nearly every REST API, a common requirement is to safeguard certain endpoints with credentials, often accomplished through mechanisms like basic authentication. To achieve this, we can establish an authenticator for each server context, as demonstrated below:

```java
HttpContext context =server.createContext("/api/hello", (exchange -> {
  // this part remains unchanged
}));
context.setAuthenticator(new BasicAuthenticator("myrealm") {
    @Override
    public boolean checkCredentials(String user, String pwd) {
        return user.equals("admin") && pwd.equals("admin");
    }
});
```



The term "myrealm" within the BasicAuthenticator corresponds to a realm name. A realm serves as a virtual identifier that helps differentiate various authentication spaces. You can delve deeper into this concept by referring to [RFC 1945](https://tools.ietf.org/html/rfc1945#section-11).

To access this protected endpoint, you can include an "Authorization" header in your request, like so:

```bash
curl -v localhost:8000/api/hello?name=Marcin -H 'Authorization: Basic YWRtaW46YWRtaW4='
```



The text following `Basic` in the header represents a Base64-encoded form of `admin:admin`, which are the hardcoded credentials in our example code. In a real-world application, the authentication process typically involves extracting these credentials from the header and then comparing them with the username and password stored in a database.

Should you omit the "Authorization" header, the API will respond with an appropriate status code to indicate the lack of authentication.
```
HTTP/1.1 401 Unauthorized

```

## Handling JSON and Exception Scenarios, and More

Now, let's delve into a more intricate example.

Drawing from my prior experience in software development, I've found that the most frequently encountered API scenario involves the exchange of JSON data.

In this case, we'll embark on creating an API for user registration, with an in-memory database serving as our data store.

Our user domain object will be characterized by its simplicity:

```java
@Value
@Builder
public class User {

    String id;
    String login;
    String password;
}

```
I've employed Lombok annotations to spare myself from the tedium of crafting constructors and getters manually; they'll be automatically generated during the build process.

Within the context of our REST API, we aim to transmit only the login and password. To accommodate this, I've introduced a distinct domain object:

```java
@Value
@Builder
public class NewUser {

    String login;
    String password;
}

```

User creation will be managed by a service that we'll employ within our API handler. The service method, in its current form, focuses solely on storing the user data.

In a fully-fledged application, this service could encompass additional functionalities, such as triggering events following successful user registration.

```java
public String create(NewUser user) {
    return userRepository.create(user);
}
```

Our in-memory repository implementation is structured as follows:
```java

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.consulner.domain.user.NewUser;
import com.consulner.domain.user.User;
import com.consulner.domain.user.UserRepository;

public class InMemoryUserRepository implements UserRepository {

    private static final Map USERS_STORE = new ConcurrentHashMap();

    @Override
    public String create(NewUser newUser) {
        String id = UUID.randomUUID().toString();
        User user = User.builder()
            .id(id)
            .login(newUser.getLogin())
            .password(newUser.getPassword())
            .build();
        USERS_STORE.put(newUser.getLogin(), user);

        return id;
    }
}
```
Finally, let's bring everything together in our handler:

```java
protected void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            throw new UnsupportedOperationException();
        }

        RegistrationRequest registerRequest = readRequest(exchange.getRequestBody(), RegistrationRequest.class);

        NewUser user = NewUser.builder()
            .login(registerRequest.getLogin())
            .password(PasswordEncoder.encode(registerRequest.getPassword()))
            .build();

        String userId = userService.create(user);

        exchange.getResponseHeaders().set(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);
        exchange.sendResponseHeaders(StatusCode.CREATED.getCode(), 0);

        byte[] response = writeResponse(new RegistrationResponse(userId));

        OutputStream responseBody = exchange.getResponseBody();
        responseBody.write(response);
        responseBody.close();
    }
```

It translates JSON request into `RegistrationRequest` object:

```java
@Value
class RegistrationRequest {

    String login;
    String password;
}
```

which I later map to domain object `NewUser` to finally save it in database and write response as JSON.

I need to translate `RegistrationResponse` object back to JSON string.

Marshalling and unmarshalling JSON is done with Jackson object mapper (`com.fasterxml.jackson.databind.ObjectMapper`).

And this is how I instantiate the new handler in application main method:

```java
 public static void main(String[] args) throws IOException {
        int serverPort = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(serverPort), 0);

        RegistrationHandler registrationHandler = new RegistrationHandler(getUserService(), getObjectMapper(),
            getErrorHandler());
        server.createContext("/api/users/register", registrationHandler::handle);
        
        // here follows the rest.. 

 }
```

You can run the application and try one of the example requests below:

```bash
curl -X POST localhost:8000/api/users/register -d '{"login": "test" , "password" : "test"}'
```

response:
```bash
{"id":"395eab24-1fdd-41ae-b47e-302591e6127e"}
```

```bash
curl -v -X POST localhost:8000/api/users/register -d '{"wrong": "request"}'
```

response:
```bash
< HTTP/1.1 400 Bad Request
< Date: Sat, 29 Dec 2018 00:11:21 GMT
< Transfer-encoding: chunked
< Content-type: application/json
< 
* Connection #0 to host localhost left intact
{"code":400,"message":"Unrecognized field \"wrong\" (class com.consulner.app.api.user.RegistrationRequest), not marked as ignorable (2 known properties: \"login\", \"password\"])\n at [Source: (sun.net.httpserver.FixedLengthInputStream); line: 1, column: 21] (through reference chain: com.consulner.app.api.user.RegistrationRequest[\"wrong\"])"}
```

Also, by chance I encountered a project [java-express](https://github.com/Simonwep/java-express)
which is a Java counterpart of Node.js [Express](https://expressjs.com/) framework
and is using jdk.httpserver as well, so all the concepts covered in this article you can find in real-life application framework :)
which is also small enough to digest the codes quickly.