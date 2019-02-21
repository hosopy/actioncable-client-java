package com.hosopy.actioncable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hosopy.actioncable.annotation.Data;
import com.hosopy.actioncable.annotation.Perform;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import okio.BufferedSource;
import okio.ByteString;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class SubscriptionTest {

    private static final int TIMEOUT = 10000;

    @Test
    public void getIdentifierByDefaultInterface() throws URISyntaxException {
        final Consumer consumer = new Consumer(new URI("ws://example.com:28080"));
        final Channel channel = new Channel("CommentsChannel");
        final Subscription subscription = consumer.getSubscriptions().create(channel);

        assertThat(subscription.getIdentifier(), is(channel.toIdentifier()));
    }

    @Test
    public void getIdentifierByCustomInterface() throws URISyntaxException {
        final Consumer consumer = new Consumer(new URI("ws://example.com:28080"));
        final Channel channel = new Channel("CommentsChannel");
        final Subscription subscription = consumer.getSubscriptions().create(channel, CustomSubscription.class);

        assertThat(subscription.getIdentifier(), is(channel.toIdentifier()));
    }

    @Test
    public void onConnectedByDefaultInterface() throws URISyntaxException, InterruptedException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        final Consumer consumer = new Consumer(new URI("ws://example.com:28080"));
        final Channel channel = new Channel("CommentsChannel");
        final Subscription subscription = consumer.getSubscriptions().create(channel);

        final Subscription returned = subscription.onConnected(new Subscription.ConnectedCallback() {
            @Override
            public void call() {
                events.offer("onConnected");
            }
        });
        assertThat(returned, is(theInstance(subscription)));

        consumer.getSubscriptions().notifyConnected(subscription.getIdentifier());

        assertThat(events.take(), is("onConnected"));
    }

    @Test
    public void onConnectedByCustomInterface() throws URISyntaxException, InterruptedException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        final Consumer consumer = new Consumer(new URI("ws://example.com:28080"));
        final Channel channel = new Channel("CommentsChannel");
        final Subscription subscription = consumer.getSubscriptions().create(channel, CustomSubscription.class);

        final Subscription returned = subscription.onConnected(new Subscription.ConnectedCallback() {
            @Override
            public void call() {
                events.offer("onConnected");
            }
        });
        assertThat(returned, is(theInstance(subscription)));

        consumer.getSubscriptions().notifyConnected(subscription.getIdentifier());

        assertThat(events.take(), is("onConnected"));
    }

    @Test
    public void onDisconnectedByDefaultInterface() throws URISyntaxException, InterruptedException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        final Consumer consumer = new Consumer(new URI("ws://example.com:28080"));
        final Channel channel = new Channel("CommentsChannel");
        final Subscription subscription = consumer.getSubscriptions().create(channel);

        final Subscription returned = subscription.onDisconnected(new Subscription.DisconnectedCallback() {
            @Override
            public void call() {
                events.offer("onDisconnected");
            }
        });
        assertThat(returned, is(theInstance(subscription)));

        consumer.getSubscriptions().notifyDisconnected();

        assertThat(events.take(), is("onDisconnected"));
    }

    @Test
    public void onDisconnectedByCustomInterface() throws URISyntaxException, InterruptedException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        final Consumer consumer = new Consumer(new URI("ws://example.com:28080"));
        final Channel channel = new Channel("CommentsChannel");
        final Subscription subscription = consumer.getSubscriptions().create(channel, CustomSubscription.class);

        final Subscription returned = subscription.onDisconnected(new Subscription.DisconnectedCallback() {
            @Override
            public void call() {
                events.offer("onDisconnected");
            }
        });
        assertThat(returned, is(theInstance(subscription)));

        consumer.getSubscriptions().notifyDisconnected();

        assertThat(events.take(), is("onDisconnected"));
    }

    @Test
    public void onRejectedByDefaultInterface() throws URISyntaxException, InterruptedException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        final Consumer consumer = new Consumer(new URI("ws://example.com:28080"));
        final Channel channel = new Channel("CommentsChannel");
        final Subscription subscription = consumer.getSubscriptions().create(channel);

        final Subscription returned = subscription.onRejected(new Subscription.RejectedCallback() {
            @Override
            public void call() {
                events.offer("onRejected");
            }
        });
        assertThat(returned, is(theInstance(subscription)));

        consumer.getSubscriptions().reject(subscription.getIdentifier());

        assertThat(events.take(), is("onRejected"));
    }

    @Test
    public void onRejectedByCustomInterface() throws URISyntaxException, InterruptedException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        final Consumer consumer = new Consumer(new URI("ws://example.com:28080"));
        final Channel channel = new Channel("CommentsChannel");
        final Subscription subscription = consumer.getSubscriptions().create(channel, CustomSubscription.class);

        final Subscription returned = subscription.onRejected(new Subscription.RejectedCallback() {
            @Override
            public void call() {
                events.offer("onRejected");
            }
        });
        assertThat(returned, is(theInstance(subscription)));

        consumer.getSubscriptions().reject(subscription.getIdentifier());

        assertThat(events.take(), is("onRejected"));
    }

    @Test
    public void onReceivedByDefaultInterface() throws URISyntaxException, InterruptedException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        final Consumer consumer = new Consumer(new URI("ws://example.com:28080"));
        final Channel channel = new Channel("CommentsChannel");
        final Subscription subscription = consumer.getSubscriptions().create(channel);

        final Subscription returned = subscription.onReceived(new Subscription.ReceivedCallback() {
            @Override
            public void call(JsonElement data) {
                events.offer("onReceived:" + data.toString());
            }
        });
        assertThat(returned, is(theInstance(subscription)));

        final JsonObject data = new JsonObject();
        data.addProperty("foo", "bar");
        consumer.getSubscriptions().notifyReceived(subscription.getIdentifier(), data);

        assertThat(events.take(), is("onReceived:" + data.toString()));
    }

    @Test
    public void onReceivedByCustomInterface() throws URISyntaxException, InterruptedException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        final Consumer consumer = new Consumer(new URI("ws://example.com:28080"));
        final Channel channel = new Channel("CommentsChannel");
        final Subscription subscription = consumer.getSubscriptions().create(channel, CustomSubscription.class);

        final Subscription returned = subscription.onReceived(new Subscription.ReceivedCallback() {
            @Override
            public void call(JsonElement data) {
                events.offer("onReceived:" + data.toString());
            }
        });
        assertThat(returned, is(theInstance(subscription)));

        final JsonObject data = new JsonObject();
        data.addProperty("foo", "bar");
        consumer.getSubscriptions().notifyReceived(subscription.getIdentifier(), data);

        assertThat(events.take(), is("onReceived:" + data.toString()));
    }

    @Test
    public void onFailedByDefaultInterface() throws URISyntaxException, InterruptedException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        final Consumer consumer = new Consumer(new URI("ws://example.com:28080"));
        final Channel channel = new Channel("CommentsChannel");
        final Subscription subscription = consumer.getSubscriptions().create(channel);

        final Subscription returned = subscription.onFailed(new Subscription.FailedCallback() {
            @Override
            public void call(ActionCableException e) {
                events.offer("onFailed:" + e.getMessage());
            }
        });
        assertThat(returned, is(theInstance(subscription)));

        final ActionCableException e = new ActionCableException(new Exception("error"));
        consumer.getSubscriptions().notifyFailed(e);

        assertThat(events.take(), is("onFailed:" + e.getMessage()));
    }

    @Test
    public void onFailedByCustomInterface() throws URISyntaxException, InterruptedException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        final Consumer consumer = new Consumer(new URI("ws://example.com:28080"));
        final Channel channel = new Channel("CommentsChannel");
        final Subscription subscription = consumer.getSubscriptions().create(channel);

        final Subscription returned = subscription.onFailed(new Subscription.FailedCallback() {
            @Override
            public void call(ActionCableException e) {
                events.offer("onFailed:" + e.getMessage());
            }
        });
        assertThat(returned, is(theInstance(subscription)));

        final ActionCableException e = new ActionCableException(new Exception("error"));
        consumer.getSubscriptions().notifyFailed(e);

        assertThat(events.take(), is("onFailed:" + e.getMessage()));
    }

    @Test(timeout = TIMEOUT)
    public void performWithDataByDefaultInterface() throws URISyntaxException, InterruptedException, IOException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        final MockWebServer mockWebServer = new MockWebServer();
        final MockResponse response = new MockResponse();
        response.withWebSocketUpgrade(new DefaultWebSocketListener() {
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                events.offer(text);
            }
            @Override
            public void onMessage(WebSocket webSocket, ByteString text) {
                events.offer(text.utf8());
            }
        });
        mockWebServer.enqueue(response);
        mockWebServer.start();

        final Consumer consumer = new Consumer(mockWebServer.url("/").uri());
        final Subscription subscription = consumer.getSubscriptions().create(new Channel("CommentsChannel"));
        consumer.connect();

        events.take(); // { command: subscribe }

        final JsonObject data = new JsonObject();
        data.addProperty("foo", "bar");
        subscription.perform("follow", data);

        final JsonObject expected = new JsonObject();
        expected.addProperty("command", "message");
        expected.addProperty("identifier", subscription.getIdentifier());
        expected.addProperty("data", data.toString());
        assertThat(events.take(), is(expected.toString()));

        mockWebServer.shutdown();
    }

    @Test(timeout = TIMEOUT)
    public void performWithDataByCustomInterface() throws URISyntaxException, InterruptedException, IOException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        final MockWebServer mockWebServer = new MockWebServer();
        final MockResponse response = new MockResponse();
        response.withWebSocketUpgrade(new DefaultWebSocketListener() {
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                events.offer(text);
            }
        });
        mockWebServer.enqueue(response);
        mockWebServer.start();

        final Consumer consumer = new Consumer(mockWebServer.url("/").uri());
        final Subscription subscription = consumer.getSubscriptions().create(new Channel("CommentsChannel"), CustomSubscription.class);
        consumer.connect();

        events.take(); // { command: subscribe }

        final JsonObject data = new JsonObject();
        data.addProperty("foo", "bar");
        subscription.perform("follow", data);

        final JsonObject expected = new JsonObject();
        expected.addProperty("command", "message");
        expected.addProperty("identifier", subscription.getIdentifier());
        expected.addProperty("data", data.toString());
        assertThat(events.take(), is(expected.toString()));

        mockWebServer.shutdown();
    }

    @Test(timeout = TIMEOUT)
    public void performByDefaultInterface() throws URISyntaxException, InterruptedException, IOException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        final MockWebServer mockWebServer = new MockWebServer();
        final MockResponse response = new MockResponse();
        response.withWebSocketUpgrade(new DefaultWebSocketListener() {
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                events.offer(text);
            }
        });
        mockWebServer.enqueue(response);
        mockWebServer.start();

        final Consumer consumer = new Consumer(mockWebServer.url("/").uri());
        final Subscription subscription = consumer.getSubscriptions().create(new Channel("CommentsChannel"));
        consumer.connect();

        events.take(); // { command: subscribe }

        subscription.perform("follow");

        final JsonObject expected = new JsonObject();
        expected.addProperty("command", "message");
        expected.addProperty("identifier", subscription.getIdentifier());
        expected.addProperty("data", "{\"action\":\"follow\"}");
        assertThat(events.take(), is(expected.toString()));

        mockWebServer.shutdown();
    }

    @Test(timeout = TIMEOUT)
    public void performByCustomInterface() throws URISyntaxException, InterruptedException, IOException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        final MockWebServer mockWebServer = new MockWebServer();
        final MockResponse response = new MockResponse();
        response.withWebSocketUpgrade(new DefaultWebSocketListener() {
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                events.offer(text);
            }
        });
        mockWebServer.enqueue(response);
        mockWebServer.start();

        final Consumer consumer = new Consumer(mockWebServer.url("/").uri());
        final Subscription subscription = consumer.getSubscriptions().create(new Channel("CommentsChannel"), CustomSubscription.class);
        consumer.connect();

        events.take(); // { command: subscribe }

        subscription.perform("follow");

        final JsonObject expected = new JsonObject();
        expected.addProperty("command", "message");
        expected.addProperty("identifier", subscription.getIdentifier());
        expected.addProperty("data", "{\"action\":\"follow\"}");
        assertThat(events.take(), is(expected.toString()));

        mockWebServer.shutdown();
    }

    @Test(timeout = TIMEOUT)
    public void performByCustomInterfaceMethod() throws URISyntaxException, InterruptedException, IOException {
        final BlockingQueue<String> events = new LinkedBlockingQueue<String>();

        final MockWebServer mockWebServer = new MockWebServer();
        final MockResponse response = new MockResponse();
        response.withWebSocketUpgrade(new DefaultWebSocketListener() {
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                events.offer(text);
            }
        });
        mockWebServer.enqueue(response);
        mockWebServer.start();

        final Consumer consumer = new Consumer(mockWebServer.url("/").uri());
        final CustomSubscription subscription = consumer.getSubscriptions().create(new Channel("CommentsChannel"), CustomSubscription.class);
        consumer.connect();

        events.take(); // { command: subscribe }

        subscription.touch();

        JsonObject expected = new JsonObject();
        expected.addProperty("command", "message");
        expected.addProperty("identifier", subscription.getIdentifier());
        expected.addProperty("data", "{\"action\":\"touch\"}");
        assertThat(events.take(), is(expected.toString()));

        subscription.follow(1);
        expected = new JsonObject();
        expected.addProperty("command", "message");
        expected.addProperty("identifier", subscription.getIdentifier());
        expected.addProperty("data", "{\"user_id\":1,\"action\":\"follow\"}");
        assertThat(events.take(), is(expected.toString()));

        subscription.send("My name is");
        expected = new JsonObject();
        expected.addProperty("command", "message");
        expected.addProperty("identifier", subscription.getIdentifier());
        expected.addProperty("data", "{\"body\":\"My name is\",\"action\":\"send\"}");
        assertThat(events.take(), is(expected.toString()));

        subscription.setPrivate(true);
        expected = new JsonObject();
        expected.addProperty("command", "message");
        expected.addProperty("identifier", subscription.getIdentifier());
        expected.addProperty("data", "{\"private\":true,\"action\":\"set_private\"}");
        assertThat(events.take(), is(expected.toString()));

        final JsonObject params = new JsonObject();
        params.addProperty("foo", "bar");
        subscription.save(1, params);
        expected = new JsonObject();
        expected.addProperty("command", "message");
        expected.addProperty("identifier", subscription.getIdentifier());
        expected.addProperty("data", "{\"item_id\":1,\"params\":{\"foo\":\"bar\"},\"action\":\"save\"}");
        assertThat(events.take(), is(expected.toString()));

        mockWebServer.shutdown();
    }

    @Test(expected = IllegalArgumentException.class)
    public void performParametersMustBeAnnotated() throws URISyntaxException, InterruptedException, IOException {
        final Consumer consumer = new Consumer(new URI("ws://example.com:28080"));
        final Channel channel = new Channel("CommentsChannel");
        final NotAnnotatedParameterSubscription subscription = consumer.getSubscriptions().create(
                channel, NotAnnotatedParameterSubscription.class);

        subscription.save(1, "title");
    }

    @Test(expected = IllegalArgumentException.class)
    public void performParametersMustBeSupportedType() throws URISyntaxException, InterruptedException, IOException {
        final Consumer consumer = new Consumer(new URI("ws://example.com:28080"));
        final Channel channel = new Channel("CommentsChannel");
        final InvalidParameterTypeSubscription subscription = consumer.getSubscriptions().create(
                channel, InvalidParameterTypeSubscription.class);

        final ActionCableException e = new ActionCableException(new Exception("error"));
        consumer.getSubscriptions().notifyFailed(e);

        subscription.save(1, new HashMap<String, String>());
    }

    private interface CustomSubscription extends Subscription {
        @Perform("touch")
        void touch();

        @Perform("follow")
        void follow(@Data("user_id") int userId);

        @Perform("send")
        void send(@Data("body") String body);

        @Perform("set_private")
        void setPrivate(@Data("private") boolean isPrivate);

        @Perform("save")
        void save(@Data("item_id") int itemId, @Data("params") JsonElement params);
    }

    private interface NotAnnotatedParameterSubscription extends Subscription {
        @Perform("save")
        void save(@Data("item_id") int itemId, String title);
    }

    private interface InvalidParameterTypeSubscription extends Subscription {
        @Perform("save")
        void save(@Data("item_id") int itemId, @Data("params") Map<String, String> params);
    }

    private static class DefaultWebSocketListener extends WebSocketListener {

    }
}
