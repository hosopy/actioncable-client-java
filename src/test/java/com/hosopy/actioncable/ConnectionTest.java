package com.hosopy.actioncable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.ByteString;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class ConnectionTest {

    private static final int TIMEOUT = 10000;

    MockWebServer mockWebServer;

    @Before
    public void setUp() {
        mockWebServer = new MockWebServer();
    }

    @After
    public void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    public void createUriAndOptions() throws URISyntaxException {
        new Connection(new URI("ws://example.com:28080"), new Consumer.Options());
    }

    @Test
    public void setListener() throws URISyntaxException {
        new Connection(new URI("ws://example.com:28080"), new Consumer.Options()).setListener(new DefaultConnectionListener());
    }

    @Test(timeout = TIMEOUT)
    public void shouldFireOnOpenWhenConnected() throws InterruptedException, IOException {
        final MockResponse response = new MockResponse();
        response.withWebSocketUpgrade(new DefaultWebSocketListener());
        mockWebServer.enqueue(response);
        mockWebServer.start();

        final URI uri = mockWebServer.url("/").uri();
        final Connection connection = new Connection(uri, new Consumer.Options());

        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        connection.setListener(new DefaultConnectionListener() {
            @Override
            public void onOpen() {
                events.offer("onOpen");
            }
        });
        connection.open();

        assertThat(events.take(), is("onOpen"));
    }

    @Test(timeout = TIMEOUT)
    public void shouldFireOnMessageWhenMessageReceived() throws InterruptedException, IOException {
        final MockResponse response = new MockResponse();
        response.withWebSocketUpgrade(new DefaultWebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                webSocket.send("{}");
            }
        });
        mockWebServer.enqueue(response);
        mockWebServer.start();
        Thread.sleep(1000);

        final URI uri = mockWebServer.url("/").uri();
        final Connection connection = new Connection(uri, new Consumer.Options());

        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        connection.setListener(new DefaultConnectionListener() {
            @Override
            public void onMessage(String textMessage) {
                events.offer("onMessage:" + textMessage);
            }
        });
        connection.open();

        assertThat(events.take(), is("onMessage:{}"));
    }

    @Test(timeout = TIMEOUT)
    public void shouldFireOnCloseWhenDisconnectedByClient() throws InterruptedException, IOException {
        final MockResponse response = new MockResponse();
        response.withWebSocketUpgrade(new DefaultWebSocketListener());
        mockWebServer.enqueue(response);
        mockWebServer.start();

        final URI uri = mockWebServer.url("/").uri();
        final Connection connection = new Connection(uri, new Consumer.Options());

        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        connection.setListener(new DefaultConnectionListener() {
            @Override
            public void onOpen() {
                events.offer("onOpen");
            }

            @Override
            public void onClosed() {
                super.onClosed();
                events.offer("onClosed");
            }
        });

        connection.open();
        assertThat(events.take(), is("onOpen"));
        assertThat(connection.isOpen(), is(true));

        connection.close();
        assertThat(events.take(), is("onClosed"));
    }

    @Test(timeout = TIMEOUT)
    public void shouldFireOnCloseWhenDisconnectedByServer() throws InterruptedException, IOException {
        final MockResponse response = new MockResponse();
        response.withWebSocketUpgrade(new DefaultWebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                webSocket.close(1000, "Reason");
            }
        });
        mockWebServer.enqueue(response);
        mockWebServer.start();

        final URI uri = mockWebServer.url("/").uri();
        final Connection connection = new Connection(uri, new Consumer.Options());

        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        connection.setListener(new DefaultConnectionListener() {
            @Override
            public void onClosing() {
                events.offer("onClosing");
            }
        });
        connection.open();

        assertThat(events.take(), is("onClosing"));
    }

    @Test(timeout = TIMEOUT)
    public void shouldFireOnFailureWhenInternalServerErrorReceived() throws InterruptedException, IOException {
        final MockResponse response = new MockResponse();
        response.setResponseCode(500);
        response.setStatus("HTTP/1.1 500 Internal Server Error");
        mockWebServer.enqueue(response);
        mockWebServer.start();

        final URI uri = mockWebServer.url("/").uri();
        final Connection connection = new Connection(uri, new Consumer.Options());

        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        connection.setListener(new DefaultConnectionListener() {
            @Override
            public void onFailure(Exception e) {
                events.offer("onFailed");
            }
        });
        connection.open();

        assertThat(events.take(), is("onFailed"));
    }

    @Test(timeout = TIMEOUT)
    public void isOpenWhenOnOpenAndCloseByClient() throws InterruptedException, IOException {
        final MockResponse response = new MockResponse();
        response.withWebSocketUpgrade(new DefaultWebSocketListener());
        mockWebServer.enqueue(response);
        mockWebServer.start();

        final URI uri = mockWebServer.url("/").uri();
        final Connection connection = new Connection(uri, new Consumer.Options());

        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        connection.setListener(new DefaultConnectionListener() {
            @Override
            public void onOpen() {
                events.offer("onOpen");
            }

            @Override
            public void onClosed() {
                events.offer("onClosed");
            }
        });

        assertThat(connection.isOpen(), is(false));

        connection.open();

        assertThat(events.take(), is("onOpen"));

        assertThat(connection.isOpen(), is(true));

        connection.close();

        assertThat(events.take(), is("onClosed"));

        assertThat(connection.isOpen(), is(false));
    }

    @Test(timeout = TIMEOUT)
    public void isOpenWhenOnOpenAndCloseByServer() throws InterruptedException, IOException {
        final MockResponse response = new MockResponse();
        response.withWebSocketUpgrade(new DefaultWebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                webSocket.close(1000, "Reason");
            }
        });
        mockWebServer.enqueue(response);
        mockWebServer.start();

        Thread.sleep(1000);

        final URI uri = mockWebServer.url("/").uri();
        final Connection connection = new Connection(uri, new Consumer.Options());

        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        connection.setListener(new DefaultConnectionListener() {
            @Override
            public void onOpen() {
                events.offer("onOpen");
            }

            @Override
            public void onClosed() {
                events.offer("onClosed");
            }
        });

        Thread.sleep(1000);
        assertThat(connection.isOpen(), is(false));

        connection.open();

        Thread.sleep(1000);
        assertThat(events.take(), is("onOpen"));

        // I'm not really sure why this connnection should still look opened, since it was closed by
        // the server? Maybe Okhttp 3.5 already checks if the connection is still open and returns
        // (rightfully) false?
        assertThat(connection.isOpen(), is(true));

        connection.close();

        assertThat(events.take(), is("onClosed"));

        assertThat(connection.isOpen(), is(false));
    }

    @Test(timeout = TIMEOUT)
    public void isOpenWhenOnOpenAndFailure() throws InterruptedException, IOException {
        final MockResponse response = new MockResponse();
        response.setResponseCode(500);
        response.setStatus("HTTP/1.1 500 Internal Server Error");
        mockWebServer.enqueue(response);
        mockWebServer.start();

        final URI uri = mockWebServer.url("/").uri();
        final Connection connection = new Connection(uri, new Consumer.Options());

        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        connection.setListener(new DefaultConnectionListener() {
            @Override
            public void onOpen() {
                events.offer("onOpen");
            }

            @Override
            public void onFailure(Exception e) {
                events.offer("onFailed");
            }
        });

        assertThat(connection.isOpen(), is(false));

        connection.open();

        assertThat(events.take(), is("onFailed"));

        assertThat(connection.isOpen(), is(false));
    }

    private static class DefaultConnectionListener implements Connection.Listener {


        @Override
        public void onOpen() {
        }

        @Override
        public void onFailure(Exception e) {
        }

        @Override
        public void onMessage(String textMessage) {
        }

        @Override
        public void onClosing() {
        }

        @Override
        public void onClosed() {
        }
    }

    private static class DefaultWebSocketListener extends WebSocketListener {

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.close(code, null);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        }
    }
}
