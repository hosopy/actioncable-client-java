package com.hosopy.actioncable;

import com.hosopy.concurrent.EventLoop;
import com.hosopy.util.QueryStringUtils;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import okhttp3.CookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.Buffer;
import okio.ByteString;


public class Connection {

    private enum State {
        CONNECTING,
        OPEN,
        CLOSING,
        CLOSED
    }

    /*package*/ interface Listener {
        void onOpen();

        void onFailure(Exception e);

        void onMessage(String string);

        void onClosing();

        void onClosed();
    }

    /**
     * Options for Connection.
     */
    public static class Options {
        /**
         * SSLContext
         */
        public SSLContext sslContext;
        /**
         * HostnameVerifier
         */
        public HostnameVerifier hostnameVerifier;
        /**
         * CookieHandler
         */
        public CookieJar cookieHandler;
        /**
         * Query parameters
         */
        public Map<String, String> query;
        /**
         * HTTP Headers
         */
        public Map<String, String> headers;
        /**
         * Whether to reconnect automatically.
         * <p/>
         * <p>If reconnection is true, the client attempts to reconnect to the server when underlying connection is stale.</p>
         */
        public boolean reconnection = false;
        /**
         * The maximum number of attempts to reconnect.
         * <p/>
         * <p>The maximum number of attempts to reconnect.</p>
         */
        public int reconnectionMaxAttempts = 30;
        public int reconnectionDelay = 3;
        public int reconnectionDelayMax = 30;
        /**
         * OkHttpClientFactory
         * <p/>
         * <p>To use your own OkHttpClient, set this option.</p>
         */
        public OkHttpClientFactory okHttpClientFactory;

        /**
         * The ping interval on how often a ping is sent over the websocket connection
         * <p/>
         * <p>To use your own ping interval, set this option.</p>
         */
        public Long pingInterval;
        public TimeUnit pingTimeUnit;

        public interface OkHttpClientFactory {
            OkHttpClient createOkHttpClient();
        }
    }

    private State state = State.CONNECTING;

    private URI uri;

    private Options options;

    private Listener listener;

    private WebSocket webSocket;

    private boolean isReopening = false;

    /*package*/ Connection(URI uri, Options options) {
        this.uri = uri;
        this.options = options;
    }


    /*package*/ void setListener(Listener listener) {
        this.listener = listener;
    }

    /*package*/ void open() {
        EventLoop.execute(new Runnable() {
            @Override
            public void run() {
                if (isOpen()) {
                    fireOnFailure(new IllegalStateException("Must close existing connection before opening"));
                } else {
                    doOpen();
                }
            }
        });
    }

    /*package*/ void close() {
        EventLoop.execute(new Runnable() {
            @Override
            public void run() {
                if (webSocket != null) {
                    try {
                        // http://tools.ietf.org/html/rfc6455#section-7.4.1
                        if (!isState(State.CLOSING, State.CLOSED)) {
                            webSocket.close(1000, "connection closed manually");
                            state = State.CLOSING;
                        }
                    } catch (IllegalStateException e) {
                        fireOnFailure(e);
                    }
                }
            }
        });
    }

    /*package*/ void reopen() {
        if (isState(State.CLOSED)) {
            open();
        } else {
            isReopening = true;
            close();
        }
    }

    /*package*/ boolean isOpen() {
        return webSocket != null && isState(State.OPEN);
    }

    /*package*/ boolean send(final String data) {
        if (isOpen()) {
            EventLoop.execute(new Runnable() {
                @Override
                public void run() {
                    doSend(data);
                }
            });
            return true;
        } else {
            return false;
        }
    }

    private void doOpen() {
        state = State.CONNECTING;

        OkHttpClient client;
        if (options.okHttpClientFactory != null) {
            client = options.okHttpClientFactory.createOkHttpClient();
        } else {
            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

            if (options.sslContext != null) {
                final SSLSocketFactory factory = options.sslContext.getSocketFactory();
                clientBuilder.sslSocketFactory(factory);
            }

            if (options.hostnameVerifier != null) {
                clientBuilder.hostnameVerifier(options.hostnameVerifier);
            }

            if (options.cookieHandler != null) {
                clientBuilder.cookieJar(options.cookieHandler);
            }

            if (options.pingInterval != null && options.pingTimeUnit != null) {
                clientBuilder.pingInterval(options.pingInterval, options.pingTimeUnit);
            }

            client = clientBuilder.build();
        }

        String url = uri.toString();
        if (options.query != null) {
            url = url + "?" + QueryStringUtils.encode(options.query);
        }

        final Request.Builder builder = new Request.Builder().url(url);

        if (options.headers != null) {
            for (Map.Entry<String, String> entry : options.headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        final Request request = builder.build();


        client.newWebSocket(request, webSocketListener);
        client.dispatcher().executorService().shutdown();
    }

    private void doSend(String data) {
        if (webSocket != null) {
            webSocket.send(data);
        }
    }

    private boolean isState(State... states) {
        for (State state : states) {
            if (this.state == state) {
                return true;
            }
        }
        return false;
    }

    private void fireOnFailure(Exception e) {
        if (listener != null) {
            listener.onFailure(e);
        }
    }

    private WebSocketListener webSocketListener = new WebSocketListener() {
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            Connection.this.state = State.OPEN;
            Connection.this.webSocket = webSocket;
            EventLoop.execute(new Runnable() {
                @Override
                public void run() {
                    if (listener != null) {
                        listener.onOpen();
                    }
                }
            });
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            EventLoop.execute(new Runnable() {
                @Override
                public void run() {
                    state = State.CLOSED;

                    if (listener != null) {
                        listener.onFailure((Exception) t);
                    }
                }
            });
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            EventLoop.execute(new Runnable() {
                @Override
                public void run() {
                    if (text != null && listener != null) {
                        listener.onMessage(text);
                    }
                }
            });
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            Connection.this.state = State.CLOSING;

            EventLoop.execute(new Runnable() {
                @Override
                public void run() {
                    state = State.CLOSING;

                    if (listener != null) {
                        listener.onClosing();
                    }

                    if (isReopening) {
                        isReopening = false;
                        open();
                    }
                }
            });
        }


        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            Connection.this.state = State.CLOSED;

            EventLoop.execute(new Runnable() {
                @Override
                public void run() {
                    state = State.CLOSED;

                    if (listener != null) {
                        listener.onClosed();
                    }

                    if (isReopening) {
                        isReopening = false;
                        open();
                    }
                }
            });
        }
    };
}
