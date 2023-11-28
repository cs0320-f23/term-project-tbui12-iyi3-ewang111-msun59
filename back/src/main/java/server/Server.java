package server;

import static spark.Spark.after;

import ingredients.Carrots;
import soup.Soup;
import spark.Spark;
import java.util.HashSet;
import java.util.Set;

/**
 * Top-level class for this demo. Contains the main() method which starts Spark and runs the various handlers.
 *
 * We have two endpoints in this demo. They need to share state (a menu).
 * This is a great chance to use dependency injection, as we do here with the menu set. If we needed more endpoints,
 * more functionality classes, etc. we could make sure they all had the same shared state.
 */
public class Server {
    asdfadsfaadsfadsf
    public static void main(String[] args) {
        int port = 3232;
        Set<Soup> menu = new HashSet<>();

        Soup one = Soup.buildNoExceptions(true, Set.of(
                new Carrots(Carrots.CarrotChopType.SHREDDED, 4.0)
        ));
        one.setSoupName("thomas");

        Soup two = Soup.buildNoExceptions(true, Set.of(
                new Carrots(Carrots.CarrotChopType.MATCHSTICK, 4.0)
        ));
        two.setSoupName("jerm");

        Soup three = Soup.buildNoExceptions(true, Set.of(
                new Carrots(Carrots.CarrotChopType.UNCHOPPED, 3.0)
        ));
        three.setSoupName("elaine");

        menu.add(one);
        menu.add(two);
        menu.add(three);
        Spark.port(port);
        /*
            Setting CORS headers to allow cross-origin requests from the client; this is necessary for the client to
            be able to make requests to the server.

            By setting the Access-Control-Allow-Origin header to "*", we allow requests from any origin.
            This is not a good idea in real-world applications, since it opens up your server to cross-origin requests
            from any website. Instead, you should set this header to the origin of your client, or a list of origins
            that you trust.

            By setting the Access-Control-Allow-Methods header to "*", we allow requests with any HTTP method.
            Again, it's generally better to be more specific here and only allow the methods you need, but for
            this demo we'll allow all methods.

            We recommend you learn more about CORS with these resources:
                - https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS
                - https://portswigger.net/web-security/cors
         */
        after((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "*");
        });

        // Setting up the handler for the GET /order and /mock endpoints
        Spark.get("order", new OrderHandler(menu));
        Spark.get("mock", new MockHandler());
        Spark.init();
        Spark.awaitInitialization();

        // Notice this link alone leads to a 404... Why is that?
        System.out.println("Server started at http://localhost:" + port);
    }
}