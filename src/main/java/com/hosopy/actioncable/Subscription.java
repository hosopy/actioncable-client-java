package com.hosopy.actioncable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Subscription provides a number of callbacks and a method for calling remote procedure calls
 * on the corresponding Channel instance on the server side.
 *
 * @author hosopy
 */
public interface Subscription {

    /**
     * Return the identifier of this subscription.
     *
     * @return Identifier string
     */
    String getIdentifier();

    /**
     * Set {@link ConnectedCallback}
     *
     * @param callback {@link ConnectedCallback} instance
     * @return {@link Subscription} instance
     */
    Subscription onConnected(ConnectedCallback callback);

    /**
     * Set {@link DisconnectedCallback}
     *
     * @param callback {@link DisconnectedCallback} instance
     * @return {@link Subscription} instance
     */
    Subscription onDisconnected(DisconnectedCallback callback);

    /**
     * Set {@link RejectedCallback}
     *
     * @param callback {@link RejectedCallback} instance
     * @return {@link Subscription} instance
     */
    Subscription onRejected(RejectedCallback callback);

    /**
     * Set {@link ReceivedCallback}
     *
     * @param callback {@link ReceivedCallback} instance
     * @return {@link Subscription} instance
     */
    Subscription onReceived(ReceivedCallback callback);

    /**
     * Set {@link FailedCallback}
     *
     * @param callback {@link FailedCallback} instance
     * @return {@link Subscription} instance
     */
    Subscription onFailed(FailedCallback callback);

    /**
     * Call remote procedure calls on the corresponding Channel instance on the server.
     *
     * @param action Procedure name to perform
     * @param data Parameters passed to procedure
     */
    void perform(String action, JsonObject data);

    /**
     * Call remote procedure calls on the corresponding Channel instance on the server.
     *
     * @param action Procedure name to perform
     */
    void perform(String action);

    interface SimpleCallback {
        void call();
    }

    /**
     * Callback called when the subscription has been successfully completed.
     */
    interface ConnectedCallback extends SimpleCallback {
    }

    /**
     * Callback called when the subscription has been closed.
     */
    interface DisconnectedCallback extends SimpleCallback {
    }

    /**
     * Callback called when the subscription is rejected by the server.
     */
    interface RejectedCallback extends SimpleCallback {
    }

    /**
     * Callback called when the subscription receives data from the server.
     */
    interface ReceivedCallback {
        /**
         * Callback method
         *
         * @param data Received data
         */
        void call(JsonElement data);
    }

    /**
     * Callback called when the subscription encounters any error.
     */
    interface FailedCallback {
        /**
         * Callback method
         *
         * @param e {@link ActionCableException} instance
         */
        void call(ActionCableException e);
    }
}
