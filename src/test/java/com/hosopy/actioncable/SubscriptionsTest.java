package com.hosopy.actioncable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class SubscriptionsTest {

    private static final int TIMEOUT = 10000;

    @Test(timeout = TIMEOUT)
    public void createAfterOpeningConnection() throws URISyntaxException, IOException, InterruptedException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        final MockWebServer mockWebServer = new MockWebServer();
        final MockResponse response = new MockResponse();
        response.withWebSocketUpgrade(new DefaultWebSocketListener() {
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

        final Subscriptions subscriptions = consumer.getSubscriptions();
        final Subscription subscription = subscriptions.create(new Channel("CommentsChannel"));
        subscription.onConnected(new Subscription.ConnectedCallback() {
            @Override
            public void call() {
                events.offer("connected");
            }
        });

        // Callback test
        assertThat(events.take(), is("connected"));
        assertThat(events.take(), is("onMessage:" + Command.subscribe(subscription.getIdentifier()).toJson()));

        mockWebServer.shutdown();
    }

    @Test(timeout = TIMEOUT)
    public void createBeforeOpeningConnection() throws URISyntaxException, IOException, InterruptedException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        final MockWebServer mockWebServer = new MockWebServer();
        final MockResponse response = new MockResponse();
        response.withWebSocketUpgrade(new DefaultWebSocketListener() {
            @Override
            public void onMessage(BufferedSource payload, WebSocket.PayloadType type) throws IOException {
                events.offer("onMessage:" + payload.readUtf8());
                payload.close();
            }
        });
        mockWebServer.enqueue(response);
        mockWebServer.start();

        final Consumer consumer = new Consumer(mockWebServer.url("/").uri());

        final Subscriptions subscriptions = consumer.getSubscriptions();
        final Subscription subscription = subscriptions.create(new Channel("CommentsChannel"));
        subscription.onConnected(new Subscription.ConnectedCallback() {
            @Override
            public void call() {
                events.offer("connected");
            }
        });

        consumer.open();

        // Callback test
        assertThat(events.take(), is("connected"));
        assertThat(events.take(), is("onMessage:" + Command.subscribe(subscription.getIdentifier()).toJson()));

        mockWebServer.shutdown();
    }

    @Test
    public void getConsumer() throws URISyntaxException {
        final Consumer consumer = new Consumer(new URI("ws://example.com:28080"));
        final Subscriptions subscriptions = consumer.getSubscriptions();

        final Consumer _consumer = subscriptions.getConsumer();
        assertThat(_consumer, is(consumer));
    }

    @Test(timeout = TIMEOUT)
    public void removeWhenIdentifierIsUnique() throws IOException, InterruptedException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        final MockWebServer mockWebServer = new MockWebServer();
        final MockResponse response = new MockResponse();
        response.withWebSocketUpgrade(new DefaultWebSocketListener() {
            @Override
            public void onMessage(BufferedSource payload, WebSocket.PayloadType type) throws IOException {
                events.offer("onMessage:" + payload.readUtf8());
                payload.close();
            }
        });
        mockWebServer.enqueue(response);

        final Consumer consumer = new Consumer(mockWebServer.url("/").uri());
        final Subscriptions subscriptions = consumer.getSubscriptions();

        final Subscription subscription1 = subscriptions.create(new Channel("CommentsChannel"));
        subscription1.onConnected(new Subscription.ConnectedCallback() {
            @Override
            public void call() {
                events.offer("connected_1");
            }
        });

        final Subscription subscription2 = subscriptions.create(new Channel("NotificationChannel"));
        subscription2.onConnected(new Subscription.ConnectedCallback() {
            @Override
            public void call() {
                events.offer("connected_2");
            }
        });

        consumer.open();

        events.take(); // connected_1
        events.take(); // connected_2
        events.take(); // WebSocketListener#onMessage
        events.take(); // WebSocketListener#onMessage

        subscriptions.remove(subscription1);

        assertThat(subscriptions.contains(subscription1), is(false));

        assertThat(events.take(), is("onMessage:" + Command.unsubscribe(subscription1.getIdentifier()).toJson()));

        mockWebServer.shutdown();
    }

    @Test(timeout = TIMEOUT)
    public void removeWhenIdentifierIsNotUnique() throws IOException, InterruptedException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        final MockWebServer mockWebServer = new MockWebServer();
        final MockResponse response = new MockResponse();
        response.withWebSocketUpgrade(new DefaultWebSocketListener() {
            @Override
            public void onMessage(BufferedSource payload, WebSocket.PayloadType type) throws IOException {
                events.offer("onMessage:" + payload.readUtf8());
                payload.close();
            }
        });
        mockWebServer.enqueue(response);

        final Consumer consumer = new Consumer(mockWebServer.url("/").uri());
        final Subscriptions subscriptions = consumer.getSubscriptions();

        final Subscription subscription1 = subscriptions.create(new Channel("CommentsChannel"));
        subscription1.onConnected(new Subscription.ConnectedCallback() {
            @Override
            public void call() {
                events.offer("connected_1");
            }
        });

        // Channel is same as subscription1
        final Subscription subscription2 = subscriptions.create(new Channel("CommentsChannel"));
        subscription2.onConnected(new Subscription.ConnectedCallback() {
            @Override
            public void call() {
                events.offer("connected_2");
            }
        });

        consumer.open();

        events.take(); // connected_1
        events.take(); // connected_2
        events.take(); // WebSocketListener#onMessage
        events.take(); // WebSocketListener#onMessage

        subscriptions.remove(subscription1);

        assertThat(subscriptions.contains(subscription1), is(false));
        assertThat(subscriptions.contains(subscription2), is(true));

        assertThat(events.take(), is("onMessage:" + Command.unsubscribe(subscription1.getIdentifier()).toJson()));

        mockWebServer.shutdown();
    }

    @Test(timeout = TIMEOUT)
    public void notifyReceived() throws InterruptedException, URISyntaxException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        final Consumer consumer = new Consumer(new URI("ws://example.com:28080"));
        final Subscriptions subscriptions = consumer.getSubscriptions();

        final Subscription subscription1 = subscriptions.create(new Channel("CommentsChannel"));
        subscription1.onReceived(new Subscription.ReceivedCallback() {
            @Override
            public void call(JsonElement data) {
                events.offer("received1:" + data.toString());
            }
        });

        final Subscription subscription2 = subscriptions.create(new Channel("NotificationChannel"));
        subscription2.onReceived(new Subscription.ReceivedCallback() {
            @Override
            public void call(JsonElement data) {
                events.offer("received2:" + data.toString());
            }
        });

        consumer.open();

        final JsonObject data = new JsonObject();
        data.addProperty("foo", "bar");
        subscriptions.notifyReceived(subscription1.getIdentifier(), data);

        assertThat(events.take(), is("received1:" + data.toString()));

        // Any callback should be called
        assertThat(events.poll(1, TimeUnit.SECONDS), nullValue());
    }

    @Test(timeout = TIMEOUT)
    public void reject() throws IOException, InterruptedException, URISyntaxException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        final Consumer consumer = new Consumer(new URI("ws://example.com:28080"));
        final Subscriptions subscriptions = consumer.getSubscriptions();

        final Channel channel1 = new Channel("CommentsChannel");
        final Subscription subscription1 = subscriptions.create(channel1);
        subscription1.onRejected(new Subscription.RejectedCallback() {
            @Override
            public void call() {
                events.offer("rejected_1");
            }
        });

        final Channel channel2 = new Channel("NotificationChannel");
        final Subscription subscription2 = subscriptions.create(channel2);
        subscription2.onRejected(new Subscription.RejectedCallback() {
            @Override
            public void call() {
                events.offer("rejected_2");
            }
        });

        subscriptions.reject(channel1.toIdentifier());
        assertThat(subscriptions.contains(subscription1), is(false));
        assertThat(subscriptions.contains(subscription2), is(true));
        assertThat(events.take(), is("rejected_1"));

        subscriptions.reject(channel2.toIdentifier());
        assertThat(subscriptions.contains(subscription2), is(false));
        assertThat(events.take(), is("rejected_2"));
    }

    @Test(timeout = TIMEOUT)
    public void notifyConnected() throws IOException, InterruptedException, URISyntaxException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        final Consumer consumer = new Consumer(new URI("ws://example.com:28080"));
        final Subscriptions subscriptions = consumer.getSubscriptions();

        final Channel channel1 = new Channel("CommentsChannel");
        final Subscription subscription1 = subscriptions.create(channel1);
        subscription1.onConnected(new Subscription.ConnectedCallback() {
            @Override
            public void call() {
                events.offer("connected_1");
            }
        });

        final Channel channel2 = new Channel("NotificationChannel");
        final Subscription subscription2 = subscriptions.create(channel2);
        subscription2.onConnected(new Subscription.ConnectedCallback() {
            @Override
            public void call() {
                events.offer("connected_2");
            }
        });

        subscriptions.notifyConnected(channel1.toIdentifier());
        assertThat(events.take(), is("connected_1"));

        subscriptions.notifyConnected(channel2.toIdentifier());
        assertThat(events.take(), is("connected_2"));
    }

    @Test(timeout = TIMEOUT)
    public void notifyDisconnected() throws IOException, InterruptedException, URISyntaxException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        final Consumer consumer = new Consumer(new URI("ws://example.com:28080"));
        final Subscriptions subscriptions = consumer.getSubscriptions();

        final Subscription subscription1 = subscriptions.create(new Channel("CommentsChannel"));
        subscription1.onDisconnected(new Subscription.DisconnectedCallback() {
            @Override
            public void call() {
                events.offer("disconnected_1");
            }
        });

        final Subscription subscription2 = subscriptions.create(new Channel("NotificationChannel"));
        subscription2.onDisconnected(new Subscription.DisconnectedCallback() {
            @Override
            public void call() {
                events.offer("disconnected_2");
            }
        });

        subscriptions.notifyDisconnected();

        assertThat(events.take(), anyOf(is("disconnected_1"), is("disconnected_2")));
        assertThat(events.take(), anyOf(is("disconnected_1"), is("disconnected_2")));
    }

    @Test(timeout = TIMEOUT)
    public void notifyFailed() throws IOException, InterruptedException, URISyntaxException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        final Consumer consumer = new Consumer(new URI("ws://example.com:28080"));
        final Subscriptions subscriptions = consumer.getSubscriptions();

        final Subscription subscription1 = subscriptions.create(new Channel("CommentsChannel"));
        subscription1.onFailed(new Subscription.FailedCallback() {
            @Override
            public void call(ActionCableException e) {
                events.offer("failed_1:" + e.getMessage());
            }
        });

        final Subscription subscription2 = subscriptions.create(new Channel("NotificationChannel"));
        subscription2.onFailed(new Subscription.FailedCallback() {
            @Override
            public void call(ActionCableException e) {
                events.offer("failed_2:" + e.getMessage());
            }
        });

        final ActionCableException e = new ActionCableException(new Exception("error"));
        subscriptions.notifyFailed(e);

        assertThat(events.take(), anyOf(is("failed_1:" + e.getMessage()), is("failed_2:" + e.getMessage())));
        assertThat(events.take(), anyOf(is("failed_1:" + e.getMessage()), is("failed_2:" + e.getMessage())));
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
