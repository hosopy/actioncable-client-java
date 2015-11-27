package com.hosopy.actioncable;

import java.net.URI;

/**
 * The Consumer establishes the connection to a server-side Ruby Connection object.
 * Once established, the ConnectionMonitor will ensure that its properly maintained through heartbeats and checking for stale updates.
 * The Consumer instance is also the gateway to establishing subscriptions to desired channels.
 * <pre>{@code
 * // Default Subscription Interface
 * Channel appearanceChannel = new Channel("AppearanceChannel");
 * Subscription subscription = consumer.getSubscriptions().create(appearanceChannel);
 * // Custom Subscription Interface
 * ChatSubscription chatSubscription = consumer.getSubscriptions().create(appearanceChannel, ChatSubscription.class);
 * consumer.open();
 * }</pre>
 *
 * @author hosopy
 */
public class Consumer {

    public static class Options extends Connection.Options {
    }

    private Connection connection;

    private ConnectionMonitor connectionMonitor;

    private Subscriptions subscriptions;

    /*package*/ Consumer(URI uri, Options options) {
        this.subscriptions = new Subscriptions(this);
        this.connection = new Connection(uri, options);
        this.connectionMonitor = new ConnectionMonitor(connection, options);
        this.connection.setListener(new Connection.Listener() {
            @Override
            public void onOpen() {
                connectionMonitor.connected();
                subscriptions.reload();
            }

            @Override
            public void onFailure(Exception e) {
                subscriptions.notifyFailed(new ActionCableException(e));
            }

            @Override
            public void onMessage(String string) {
                final Message message = Message.fromJson(string);
                if (message.isPing()) {
                    connectionMonitor.pingReceived();
                } else if (message.isConfirmation()) {
                    subscriptions.notifyConnected(message.getIdentifier());
                } else if (message.isRejection()) {
                    subscriptions.reject(message.getIdentifier());
                } else {
                    subscriptions.notifyReceived(message.getIdentifier(), message.getMessage());
                }
            }

            @Override
            public void onClose() {
                subscriptions.notifyDisconnected();
                connectionMonitor.disconnected();
            }
        });
    }

    /*package*/ Consumer(URI uri) {
        this(uri, new Options());
    }

    /**
     * Get subscriptions container.
     *
     * @return {@link Subscriptions} instance
     */
    public Subscriptions getSubscriptions() {
        return subscriptions;
    }

    /**
     * Establish connection.
     */
    public void open() {
        connection.open();
        connectionMonitor.start();
    }

    /**
     * Close the underlying connection.
     */
    public void close() {
        connection.close();
        connectionMonitor.stop();
    }

    /*package*/ boolean send(Command command) {
        return connection.send(command.toJson());
    }

    /*package*/ Connection getConnection() {
        return connection;
    }
}
