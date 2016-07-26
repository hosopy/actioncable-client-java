package com.hosopy.actioncable;

import java.net.URI;

/**
 * ActionCable is an entry point to create consumers.
 *
 * <pre>{@code
 * Consumer consumer = ActionCable.createConsumer(new URI("ws://cable.example.com"));
 * }</pre>
 *
 * @author hosopy
 */
public class ActionCable {
    /**
     * Create a consumer with uri and options.
     *
     * @param uri URI to connect
     * @param options Options for consumer
     * @return {@link Consumer} instance
     */
    public static Consumer createConsumer(URI uri, Consumer.Options options) {
        return new Consumer(uri, options);
    }

    /**
     * Create a consumer with uri.
     *
     * @param uri URI to connect
     * @return {@link Consumer} instance
     */
    public static Consumer createConsumer(URI uri) {
        return new Consumer(uri);
    }
}
