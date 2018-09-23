package com.hosopy.actioncable;

import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.ws.WebSocket;
import okhttp3.ws.WebSocketListener;
import okio.Buffer;
import okio.BufferedSource;
import okio.ByteString;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class ConnectionTest {

    private static final int TIMEOUT = 10000;

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
        final MockWebServer mockWebServer = new MockWebServer();
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

        mockWebServer.shutdown();
    }

    @Test(timeout = TIMEOUT)
    public void shouldFireOnMessageWhenMessageReceived() throws InterruptedException, IOException {
        final MockWebServer mockWebServer = new MockWebServer();
        final MockResponse response = new MockResponse();
        response.withWebSocketUpgrade(new DefaultWebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                try {
                    RequestBody body = RequestBody.create(WebSocket.TEXT, ByteString.encodeUtf8("{}"));
                    webSocket.sendMessage(body);
                } catch (IOException ignored) {
                }
            }
        });
        mockWebServer.enqueue(response);
        mockWebServer.start();

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

        mockWebServer.shutdown();
    }

    @Test(timeout = TIMEOUT)
    public void shouldFireOnCloseWhenDisconnectedByClient() throws InterruptedException, IOException {
        final MockWebServer mockWebServer = new MockWebServer();
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
            public void onClose() {
                events.offer("onClose");
            }
        });

        connection.open();
        assertThat(events.take(), is("onOpen"));

        connection.close();
        assertThat(events.take(), is("onClose"));

        mockWebServer.shutdown();
    }

    @Test(timeout = TIMEOUT)
    public void shouldFireOnCloseWhenDisconnectedByServer() throws InterruptedException, IOException {
        final MockWebServer mockWebServer = new MockWebServer();
        final MockResponse response = new MockResponse();
        response.withWebSocketUpgrade(new DefaultWebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                try {
                    webSocket.close(1000, "Reason");
                } catch (IOException ignored) {
                }
            }
        });
        mockWebServer.enqueue(response);
        mockWebServer.start();

        final URI uri = mockWebServer.url("/").uri();
        final Connection connection = new Connection(uri, new Consumer.Options());

        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        connection.setListener(new DefaultConnectionListener() {
            @Override
            public void onClose() {
                events.offer("onClose");
            }
        });
        connection.open();

        assertThat(events.take(), is("onClose"));

        mockWebServer.shutdown();
    }

    @Test(timeout = TIMEOUT)
    public void shouldFireOnFailureWhenInternalServerErrorReceived() throws InterruptedException, IOException {
        final MockWebServer mockWebServer = new MockWebServer();
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

        mockWebServer.shutdown();
    }

    @Test(timeout = TIMEOUT)
    public void isOpenWhenOnOpenAndCloseByClient() throws InterruptedException, IOException {
        final MockWebServer mockWebServer = new MockWebServer();
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
            public void onClose() {
                events.offer("onClose");
            }
        });

        assertThat(connection.isOpen(), is(false));

        connection.open();

        assertThat(events.take(), is("onOpen"));

        assertThat(connection.isOpen(), is(true));

        connection.close();

        assertThat(events.take(), is("onClose"));

        assertThat(connection.isOpen(), is(false));

        mockWebServer.shutdown();
    }

    @Test(timeout = TIMEOUT)
    public void isOpenWhenOnOpenAndCloseByServer() throws InterruptedException, IOException {
        final MockWebServer mockWebServer = new MockWebServer();
        final MockResponse response = new MockResponse();
        response.withWebSocketUpgrade(new DefaultWebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                try {
                    webSocket.close(1000, "Reason");
                } catch (IOException ignored) {
                }
            }
        });
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
            public void onClose() {
                events.offer("onClose");
            }
        });

        assertThat(connection.isOpen(), is(false));

        connection.open();

        assertThat(events.take(), is("onOpen"));

        assertThat(connection.isOpen(), is(true));

        connection.close();

        assertThat(events.take(), is("onClose"));

        assertThat(connection.isOpen(), is(false));

        mockWebServer.shutdown();
    }

    @Test(timeout = TIMEOUT)
    public void isOpenWhenOnOpenAndFailure() throws InterruptedException, IOException {
        final MockWebServer mockWebServer = new MockWebServer();
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

        mockWebServer.shutdown();
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
        public void onClose() {
        }
    }

    private static class DefaultWebSocketListener implements WebSocketListener {

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
        }

        @Override
        public void onFailure(IOException e, Response response) {
        }

        @Override
        public void onMessage(ResponseBody message) {

        }

        @Override
        public void onPong(Buffer payload) {
        }

        @Override
        public void onClose(int code, String reason) {
        }
    }
}
