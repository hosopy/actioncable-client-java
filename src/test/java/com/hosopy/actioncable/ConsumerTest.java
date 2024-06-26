package com.hosopy.actioncable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import okio.BufferedSource;
import okio.ByteString;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class ConsumerTest {

    private static final int TIMEOUT = 10000;

    @Test
    public void createWithValidUri() throws URISyntaxException {
        final Consumer consumer = new Consumer(new URI("ws://example.com:28080"));
        assertThat(consumer, isA(Consumer.class));
    }

    @Test
    public void createWithValidUriAndOptions() throws URISyntaxException {
        final Consumer consumer = new Consumer(new URI("ws://example.com:28080"), new Consumer.Options());
        assertThat(consumer, isA(Consumer.class));
    }

    @Test
    public void getSubscriptions() throws URISyntaxException {
        final URI uri = new URI("ws://example.com:28080");
        final Consumer consumer = new Consumer(uri);
        assertThat(consumer.getSubscriptions(), notNullValue());
    }

    @Test
    public void getConnection() throws URISyntaxException {
        final URI uri = new URI("ws://example.com:28080");
        final Consumer consumer = new Consumer(uri);
        assertThat(consumer.getConnection(), notNullValue());
    }

    @Test(timeout = TIMEOUT)
    public void connect() throws IOException, InterruptedException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        final MockWebServer mockWebServer = new MockWebServer();
        final MockResponse response = new MockResponse();
        response.withWebSocketUpgrade(new DefaultWebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                events.offer("onOpen");
            }
        });
        mockWebServer.enqueue(response);

        mockWebServer.start();

        final Consumer consumer = new Consumer(mockWebServer.url("/").uri());
        consumer.connect();

        assertThat(events.take(), is("onOpen"));

        mockWebServer.shutdown();
    }

    @Test(timeout = TIMEOUT)
    public void disconnect() throws IOException, InterruptedException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        final MockWebServer mockWebServer = new MockWebServer();
        final MockResponse response = new MockResponse();
        response.withWebSocketUpgrade(new DefaultWebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                events.offer("onOpen");
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                webSocket.close(code, reason);
                events.offer("onClosing");
            }
        });
        mockWebServer.enqueue(response);

        mockWebServer.start();

        Thread.sleep(1000);

        final Consumer consumer = new Consumer(mockWebServer.url("/").uri());
        consumer.connect();

        events.take(); // onOpen

        consumer.disconnect();

        assertThat(events.take(), is("onClosing"));

        mockWebServer.shutdown();
    }

    @Test(timeout = TIMEOUT)
    public void send() throws IOException, InterruptedException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        final MockWebServer mockWebServer = new MockWebServer();
        final MockResponse response = new MockResponse();
        response.withWebSocketUpgrade(new DefaultWebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                events.offer("onOpen");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                events.offer("onMessage:" + text);
            }
        });
        mockWebServer.enqueue(response);
        mockWebServer.start();

        final Consumer consumer = new Consumer(mockWebServer.url("/").uri());
        consumer.connect();

        events.take(); // onOpen

        // At this point, the server has received onOpen, but the connection has not always received onOpen.
        // Continue sending until connection succeeds sending.
        while (!consumer.send(Command.subscribe("identifier"))) {
            Thread.sleep(100);
        }

        assertThat(events.take(), is("onMessage:{\"command\":\"subscribe\",\"identifier\":\"identifier\"}"));

        mockWebServer.shutdown();
    }

    private static class DefaultWebSocketListener extends WebSocketListener {
    }
}
