package com.hosopy.actioncable;

import com.google.gson.JsonElement;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collection class for creating (and internally managing) channel subscriptions.
 * The only method intended to be triggered by the user
 * is {@link #create(Channel)} or {@link #create(Channel, Class)},
 * and it should be called through the consumer like so:
 * <pre>{@code
 * // Default Subscription Interface
 * Subscription subscription = consumer.getSubscriptions().create(appearanceChannel);
 * // Custom Subscription Interface
 * ChatSubscription subscription = consumer.getSubscriptions().create(appearanceChannel, ChatSubscription.class);
 * }</pre>
 *
 * @author hosopy
 */
public class Subscriptions {

    private Consumer consumer;

    private final Map<Subscription, SubscriptionProxy> subscriptionProxies = new ConcurrentHashMap<Subscription, SubscriptionProxy>();

    /*package*/ Subscriptions(Consumer consumer) {
        this.consumer = consumer;
    }

    /**
     * Create {@link Subscription} instance implements the specified interface.
     *
     * @param channel Channel to connect
     * @param subscription Interface extends {@link Subscription}
     * @return {@link Subscription} instance
     */
    public <T extends Subscription> T create(Channel channel, Class<T> subscription) {
        final SubscriptionProxy<T> subscriptionProxy = new SubscriptionProxy<T>(consumer, channel, subscription);
        add(subscriptionProxy);
        return subscriptionProxy.getProxy();
    }

    /**
     * Create {@link Subscription} instance implements {@link Subscription} interface.
     *
     * @param channel Channel to connect
     * @return {@link Subscription} instance
     */
    public Subscription create(Channel channel) {
        return create(channel, Subscription.class);
    }

    /**
     * Remove subscription from collection.
     *
     * @param subscription {@link Subscription} instance to remove
     */
    public void remove(Subscription subscription) {
        forget(subscription);
        if (!contains(subscription)) {
            consumer.send(Command.unsubscribe(subscription.getIdentifier()));
        }
    }

    /*package*/ Consumer getConsumer() {
        return consumer;
    }

    /*package*/ void reload() {
        for (final SubscriptionProxy subscriptionProxy : subscriptionProxies.values()) {
            sendSubscribeCommand(subscriptionProxy);
        }
    }

    /*package*/ void reject(String identifier) {
        for (final SubscriptionProxy subscriptionProxy : subscriptionProxies.values()) {
            if (subscriptionProxy.getIdentifier().equals(identifier)) {
                forget(subscriptionProxy.getProxy());
                subscriptionProxy.notifyRejected();
            }
        }
    }

    /*package*/ void notifyReceived(String identifier, JsonElement data) {
        for (final SubscriptionProxy subscriptionProxy : subscriptionProxies.values()) {
            if (subscriptionProxy.getIdentifier().equals(identifier)) {
                subscriptionProxy.notifyReceived(data);
            }
        }
    }

    /*package*/ void notifyConnected(String identifier) {
        for (SubscriptionProxy subscriptionProxy : subscriptionProxies.values()) {
            if (subscriptionProxy.getIdentifier().equals(identifier)) {
                subscriptionProxy.notifyConnected();
            }
        }
    }

    /*package*/ void notifyDisconnected() {
        for (SubscriptionProxy subscription : subscriptionProxies.values()) {
            subscription.notifyDisconnected();
        }
    }

    /*package*/ void notifyFailed(ActionCableException e) {
        for (SubscriptionProxy subscription : subscriptionProxies.values()) {
            subscription.notifyFailed(e);
        }
    }

    /*package*/ boolean contains(Subscription subscription) {
        return subscriptionProxies.containsKey(subscription);
    }

    private void add(SubscriptionProxy subscriptionProxy) {
        subscriptionProxies.put(subscriptionProxy.getProxy(), subscriptionProxy);
        sendSubscribeCommand(subscriptionProxy);
    }

    private void forget(Subscription subscription) {
        subscriptionProxies.remove(subscription);
    }

    private boolean sendSubscribeCommand(SubscriptionProxy subscriptionProxy) {
        return consumer.send(Command.subscribe(subscriptionProxy.getIdentifier()));
    }
}
