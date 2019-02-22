# actioncable-client-java

[![Build Status](https://travis-ci.org/hosopy/actioncable-client-java.svg)](https://travis-ci.org/hosopy/actioncable-client-java)
[![Release](https://jitpack.io/v/hosopy/actioncable-client-java.svg)](https://jitpack.io/#hosopy/actioncable-client-java)

This is the actioncable client library for Java.
Please see [Action Cable Overview](http://guides.rubyonrails.org/action_cable_overview.html) to understand actioncable itself.

## Usage

Gradle

```groovy
repositories {
    jcenter()
    maven { url "https://jitpack.io" }
}

dependencies {
    compile 'com.github.hosopy:actioncable-client-java:0.1.2'
}
```

This Library uses [google/gson](https://github.com/google/gson) to parse and compose JSON strings.

Please see [user guide](https://sites.google.com/site/gson/gson-user-guide) to know about GSON API.

### Basic

```java
// 1. Setup
URI uri = new URI("ws://cable.example.com");
Consumer consumer = ActionCable.createConsumer(uri);

// 2. Create subscription
Channel appearanceChannel = new Channel("AppearanceChannel");
Subscription subscription = consumer.getSubscriptions().create(appearanceChannel);

subscription
    .onConnected(new Subscription.ConnectedCallback() {
        @Override
        public void call() {
            // Called when the subscription has been successfully completed
        }
    }).onRejected(new Subscription.RejectedCallback() {
        @Override
        public void call() {
            // Called when the subscription is rejected by the server
        }
    }).onReceived(new Subscription.ReceivedCallback() {
        @Override
        public void call(JsonElement data) {
            // Called when the subscription receives data from the server
        }
    }).onDisconnected(new Subscription.DisconnectedCallback() {
        @Override
        public void call() {
            // Called when the subscription has been closed
        }
    }).onFailed(new Subscription.FailedCallback() {
        @Override
        public void call(ActionCableException e) {
            // Called when the subscription encounters any error
        }
    });

// 3. Establish connection
consumer.connect();

if(consumer.isConnected()) {
    System.out.println("Consumer connected!");
}

// 4. Perform any action
subscription.perform("away");

// 5. Perform any action using JsonObject(GSON)
JsonObject params = new JsonObject();
params.addProperty("foo", "bar");
subscription.perform("appear", params);
```

### Passing Parameters to Channel

```java
Channel chatChannel = new Channel("ChatChannel");
chatChannel.addParam("room", "Best Room");
Subscription subscription = consumer.getSubscriptions().create(chatChannel);
```

Supported parameter type is `Number`, `String`, `Boolean` and `JsonElement(GSON)`.

```java
chatChannel.addParam("room_id", 1);
chatChannel.addParam("room", "Best Room");
chatChannel.addParam("private", true);
chatChannel.addParam("params", new JsonObject());
```

### Custom Subscription Interface

You can perform any action by calling `Subscription#perform()`, but you can define custom interfaces having methods.

```java
public interface ChatSubscription extends Subscription {
    /*
     * Equivalent:
     *   perform("join")
     */
    @Perform("join")
    void join();

    /*
     * Equivalent:
     *   perform("send_message", JsonObjectFactory.fromJson("{body: \"...\", private: true}"))
     */
    @Perform("send_message")
    void sendMessage(@Data("body") String body, @Data("private") boolean isPrivate);
}
```

Supported parameter type is `Number`, `String`, `Boolean` and `JsonElement(GSON)`.

To instantiate the custom subscription, pass the interface when you create a subscription.

```java
Channel chatChannel = new Channel("ChatChannel");
ChatSubscription subscription = consumer.getSubscriptions().create(appearanceChannel, ChatSubscription.class);

consumer.open();

subscription.join();
subscription.sendMessage("Hello", true);
```

### Options

```java
URI uri = new URI("ws://cable.example.com");

Consumer.Options options = new Consumer.Options();
options.reconnection = true;

Consumer consumer = ActionCable.createConsumer(uri, options);
```

Below is a list of available options.

* sslContext
    
    ```java
    options.sslContext = yourSSLContextInstance;
    ```
    
* hostnameVerifier
    
    ```java
    options.hostnameVerifier = yourHostnameVerifier;
    ```
    
* cookieHandler
    
    ```java
    options.cookieHandler = yourCookieManagerInstance;
    ```
    
* query
    
    ```java
    Map<String, String> query = new HashMap();
    query.put("foo", "bar");
    options.query = query;
    ```
    
* headers
    
    ```java
    Map<String, String> headers = new HashMap();
    headers.put("X-FOO", "bar");
    headers.put("Origin", "https://your-origin.tld");
    options.headers = headers;
    ```
    
* reconnection
    * If reconnection is true, the client attempts to reconnect to the server when underlying connection is stale.
    * Default is `false`.
    
    ```java
    options.reconnection = false;
    ```
    
* reconnectionMaxAttempts
    * The maximum number of attempts to reconnect.
    * Default is `30`.
    
    ```java
    options.reconnectionMaxAttempts = 30;
    ```

* okHttpClientFactory
    * Factory instance to create your own OkHttpClient.
    * If `okHttpClientFactory` is not set, just create OkHttpClient by `new OkHttpClient()`.
    
    ```java
    options.okHttpClientFactory = new Connection.Options.OkHttpClientFactory() {
        @Override
        public OkHttpClient createOkHttpClient() {
            final OkHttpClient client = new OkHttpClient();
            client.networkInterceptors().add(new StethoInterceptor());
            return client;
        }
    };
    ```

### Authentication

How to authenticate a request depends on the architecture you choose.

#### Authenticate by HTTP Header

```java
Consumer.Options options = new Consumer.Options();

Map<String, String> headers = new HashMap();
headers.put("Authorization", "Bearer xxxxxxxxxxx");
options.headers = headers;

Consumer consumer = ActionCable.createConsumer(uri, options);
```

#### Authenticate by Query Params

```java
Consumer.Options options = new Consumer.Options();

Map<String, String> query = new HashMap();
query.put("access_token", "xxxxxxxxxx");
options.query = query;

Consumer consumer = ActionCable.createConsumer(uri, options);
```

#### Authenticate by Cookie

```java
CookieManager cookieManager = new CookieManager();
// Some setup
...
options.cookieHandler = cookieManager;

Consumer consumer = ActionCable.createConsumer(uri, options);
```

### Proguard Rules

```java
-keep class com.hosopy.actioncable.** { _; } 
-keep interface com.hosopy.actioncable._* { *; }
```

## License

MIT

