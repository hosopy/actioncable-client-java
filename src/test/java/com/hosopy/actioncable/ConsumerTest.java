package com.hosopy.actioncable;

import com.squareup.okhttp.Response;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.ws.WebSocket;
import com.squareup.okhttp.ws.WebSocketListener;
import okio.Buffer;
import okio.BufferedSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

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
    public void open() throws IOException, InterruptedException {
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
        consumer.open();

        assertThat(events.take(), is("onOpen"));

        mockWebServer.shutdown();
    }

    @Test(timeout = TIMEOUT)
    public void close() throws IOException, InterruptedException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        final MockWebServer mockWebServer = new MockWebServer();
        final MockResponse response = new MockResponse();
        response.withWebSocketUpgrade(new DefaultWebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                events.offer("onOpen");
            }

            @Override
            public void onClose(int code, String reason) {
                events.offer("onClose");
            }
        });
        mockWebServer.enqueue(response);

        mockWebServer.start();

        final Consumer consumer = new Consumer(mockWebServer.url("/").uri());
        consumer.open();

        events.take(); // onOpen

        consumer.close();

        assertThat(events.take(), is("onClose"));

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
            public void onMessage(BufferedSource payload, WebSocket.PayloadType type) throws IOException {
                events.offer("onMessage:" + payload.readUtf8());
                payload.close();
            }
        });
        mockWebServer.enqueue(response);
        mockWebServer.start();

        final Consumer consumer = new Consumer(mockWebServer.url("/").uri());
        consumer.open();

        events.take(); // onOpen

        // At this point, the server has received onOpen, but the connection has not always received onOpen.
        // Continue sending until connection succeeds sending.
        while (!consumer.send(Command.subscribe("identifier"))) {
            Thread.sleep(100);
        }

        assertThat(events.take(), is("onMessage:{\"command\":\"subscribe\",\"identifier\":\"identifier\"}"));

        mockWebServer.shutdown();
    }

    private static class DefaultWebSocketListener implements WebSocketListener {

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
        }

        @Override
        public void onFailure(IOException e, Response response) {
        }

        @Override
        public void onMessage(BufferedSource payload, WebSocket.PayloadType type) throws IOException {
            payload.close();
        }

        @Override
        public void onPong(Buffer payload) {
        }

        @Override
        public void onClose(int code, String reason) {
        }
    }
}
