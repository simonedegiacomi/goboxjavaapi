package it.simonedegiacomi.goboxapi.myws;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.neovisionaries.ws.client.*;
import it.simonedegiacomi.goboxapi.myws.annotations.WSEvent;
import it.simonedegiacomi.goboxapi.myws.annotations.WSQuery;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.lang.annotation.IncompleteAnnotationException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

/**
 * This is an implementation of handlers socket based onEvent
 * the library 'TooTallNate/Java-WebSocket'. This
 * class add some feature like event handling and query
 * concept.
 * Created on 28/12/15.
 *
 * @author Degiacomi Simone
 */
public class MyWSClient {

    // TODO: Check if used
    public static final int DEFAULT_PING_INTERVAL = 30 * 1000;

    /**
     * Logger of the class
     */
    private static final Logger log = Logger.getLogger(MyWSClient.class.getName());

    /**
     * Websocket factory
     */
    private static final WebSocketFactory factory = new WebSocketFactory();

    /**
     * Web Socket connection
     */
    private final WebSocket server;

    /**
     * Status of the connection
     */
    private boolean connected = false;

    /**
     * Parser for incoming json
     */
    private final JsonParser parser = new JsonParser();

    /**
     * Map that contains the events listener. A event is formed by a event
     * name and his data. The listener is called when a event with a speicified
     * name is received
     * The association:
     * Name of the event => Listener for this event
     */
    private final HashMap<String, WSEventListener> events;

    /**
     * Map that contains the listeners of query received. Do not
     * confuse this map with the 'queryResponses':  that map contains
     * the listener for the RESPONSE of the query MADE, not RECEIVED.
     * Association:
     * Name of the query => Listener that answer this query
     */
    private final HashMap<String, WSQueryHandler> queryAnswers;

    /**
     * This map contains the listener for the pending request made, Do not
     * confuse this map with the 'queryAnswer': The QueryAnswer ANSWER the
     * incoming query.
     * Association:
     * Query id of a made query => Response listener fot this query
     */
    private final HashMap<String, WSQueryResponseListener> queryResponses;

    /**
     * Executor used to use the java FutureTask
     */
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 8, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(8));

    /**
     * Create a new client without connecting to the sever
     *
     * @param uri URI of the server
     * @throws IOException Error while creating the ws socket with the websocket factory
     */
    public MyWSClient(URI uri) throws IOException {

        // Initialize the maps
        events = new HashMap<>();
        queryAnswers = new HashMap<>();
        queryResponses = new HashMap<>();

        server = factory.createSocket(uri);
        server.setPingInterval(DEFAULT_PING_INTERVAL);
        server.addListener(new WebSocketAdapter() {
            @Override
            public void onTextMessage(WebSocket websocket, final String message) throws Exception {

                executor.submit(() -> {

                    // Parse the message
                    JsonObject json = (JsonObject) parser.parse(message);

                    String event = json.get("event").getAsString();

                    // If the message has not the queryId parameter
                    // is an simple event
                    if (!json.has("_queryId") || json.get("_queryId").getAsString().length() <= 0) {
                        if (!events.containsKey(event)) {
                            log.warn("Received unknown event: " + event);
                            return;
                        }

                        events.get(event).onEvent(json.get("data"));

                        return;
                    }

                    // get the _queryId
                    String queryId = json.get("_queryId").getAsString();

                    // Now, check if is a query response
                    // If is a query response i MUST have an listener onEvent the
                    // 'queryResponse' map, so check here:
                    if (event.equals("queryResponse")) {

                        if (!queryResponses.containsKey(queryId)) {
                            log.warn("Unknown query response received");
                            return;
                        }

                        // Get and remove the response listener
                        queryResponses.remove(queryId).onResponse(json.get("data"));
                        return;
                    }

                    // If is not a query response neither, is a query made to this program, so
                    // find the object that will answer this query.

                    if (!queryAnswers.containsKey(event)) {
                        log.warn("Unknown query received: " + event);
                        return;
                    }

                    // Prepare the response with teh same query Id
                    JsonObject response = new JsonObject();
                    response.addProperty("event", "queryResponse");
                    response.addProperty("_queryId", json.get("_queryId").getAsString());

                    // Call the handler
                    try {
                        JsonElement answer = queryAnswers.get(event).onQuery(json.get("data"));
                        response.add("data", answer);
                    } catch (Exception ex) {
                        log.warn("WS Query Handler Exception: " + ex.toString());
                        JsonObject errorAnswer = new JsonObject();
                        errorAnswer.addProperty("error", ex.toString());
                        response.add("data", errorAnswer);
                    }
                    server.sendText(response.toString());
                });
            }

            @Override
            public void onConnected(WebSocket ws, Map<String, List<String>> headers) {
                log.info("Websocket connection established");
                connected = true;

                // If there is an event listener for the open event, call it
                WSEventListener open = events.get("open");
                if (open != null)
                    open.onEvent(null);
            }


            @Override
            public void onError(WebSocket ws, WebSocketException ex) {
                connected = false;
                WSEventListener errorListener = events.get("error");
                if (connected && errorListener != null)
                    errorListener.onEvent(null);
            }

            @Override
            public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) {
                connected = false;
                WSEventListener errorListener = events.get("close");
                if (errorListener != null)
                    errorListener.onEvent(null);
            }
        });

    }

    /**
     * This static method allows you to set a proxy that will be used
     * for the new instances of this class
     *
     * @param host IP of the proxy
     * @param port Port of the proxy
     */
    public static void setProxy (String host, int port) {
        ProxySettings proxy = factory.getProxySettings();
        proxy.setHost(host);
        proxy.setPort(port);
    }

    /**
     * Start the connection to the server
     *
     * @throws WSException Error while connecting to the server
     */
    public void connect() throws WSException {
        try {
            server.connect();
        } catch (WebSocketException ex) {
            throw new WSException(ex.toString());
        }
    }

    /**
     * Add a new http header, useful to set the Authorization header
     *
     * @param header Name of the header
     * @param value  Value of the header
     */
    public void addHttpHeader(String header, String value) {
        server.addHeader(header, value);
    }

    /**
     * This method allows you to register a new event listener
     *
     * @param event    Name of the event.
     * @param listener Listener of this event
     */
    public void onEvent(String event, WSEventListener listener) {
        events.put(event, listener);
    }

    /**
     * Register an event handler, like 'onEvent', but you don't need to specify the
     * event name. To make this work you need to put the WSEvent notation before the 'onEvent'
     * method.
     *
     * @param handler Handler to register.
     */
    public void addEventHandler(WSEventListener handler) {
        try {
            Class[] methodArgs = {JsonElement.class};
            Method method = handler.getClass().getMethod("onEvent", methodArgs);
            WSEvent annotation = method.getAnnotation(WSEvent.class);
            this.onEvent(annotation.name(), handler);
        } catch (Exception ex) {
            ex.printStackTrace();
            // TODO: Find a better Exception or declare a new one
            throw new IncompleteAnnotationException(WSEvent.class, "Annotation not found");
        }
    }

    /**
     * This method allows you to register an method that answer a response
     *
     * @param queryName Name of the query that the method will answer
     * @param listener  Listener that will call to answer the query
     */
    public void onQuery(String queryName, WSQueryHandler listener) {
        queryAnswers.put(queryName, listener);
    }

    public void addQueryHandler(WSQueryHandler handler) {
        try {
            Class[] methodArgs = {JsonElement.class};
            Method method = handler.getClass().getMethod("onQuery", methodArgs);
            WSQuery annotation = method.getAnnotation(WSQuery.class);
            this.onQuery(annotation.name(), handler);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IncompleteAnnotationException(WSEvent.class, "Annotation not found");
        }
    }

    /**
     * Make a new Query.
     *
     * @param queryName        Name of the query
     * @param query            Data of the query
     * @param responseListener Listener that will call when the response of the query
     *                         is retrieve.
     */
    public void makeQuery(String queryName, JsonElement query, WSQueryResponseListener responseListener) {
        JsonObject json = new JsonObject();
        String queryId = String.valueOf(new Random().nextInt() * System.currentTimeMillis());
        try {
            json.addProperty("event", queryName);
            json.add("data", query);
            json.addProperty("_queryId", queryId);
        } catch (Exception ex) {
            log.warn(ex.toString(), ex);
        }
        queryResponses.put(queryId, responseListener);
        server.sendText(json.toString());
    }

    /**
     * Same ad make query, but this will return immediately a futureTask.
     *
     * @param queryName Name of the query
     * @param query     Parameters of the quey
     * @return FutureTask, completed when the response  retriver
     */
    public FutureTask<JsonElement> makeQuery(String queryName, JsonElement query) {

        // Create a new wscallable
        final WSCallable callback = new WSCallable();

        // Create a new FutureTask, that will be used to synchronize the incoming
        // response
        final FutureTask<JsonElement> future = new FutureTask<>(callback);

        makeQuery(queryName, query, new WSQueryResponseListener() {
            @Override
            public void onResponse(JsonElement response) {
                // And when the result is retrieved, set the response to the
                // callable
                callback.setResponse(response);

                // And execute the future task

                executor.submit(future);
            }
        });


        // Return the new future task
        return future;
    }

    /**
     * Send a new event
     *
     * @param event Name of the event
     * @param data  Data that will be sent with the event
     */
    public void sendEvent(String event, JsonElement data) {
        JsonObject json = new JsonObject();
        try {
            json.addProperty("event", event);
            json.add("data", data);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        server.sendText(json.toString());
    }

    /**
     * Send an event, but specify that is for all the clients
     *
     * @param event Event name
     * @param data  Event data
     */
    public void sendEventBroadcast(String event, JsonElement data) {
        JsonObject json = new JsonObject();
        try {
            json.addProperty("event", event);
            json.add("data", data);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        log.info("New broadcast message sent");
        server.sendText(json.toString());
    }

    /**
     * Remove a event listener
     *
     * @param syncEvent Name of the event listener to remove
     */
    public void removeListener(String syncEvent) {
        events.remove(syncEvent);
    }

    /**
     * Return the state of the connection
     *
     * @return State of the connection
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Disconnect the websocket client
     */
    public void disconnect() {
        server.disconnect();
    }
}
