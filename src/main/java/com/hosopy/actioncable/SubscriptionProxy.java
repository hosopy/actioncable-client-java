package com.hosopy.actioncable;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.hosopy.actioncable.annotation.Data;
import com.hosopy.actioncable.annotation.Perform;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class SubscriptionProxy<T extends Subscription> {

    private Consumer consumer;

    private Channel channel;

    private T proxy;

    private Subscription.ConnectedCallback onConnected;
    private Subscription.DisconnectedCallback onDisconnected;
    private Subscription.RejectedCallback onRejected;
    private Subscription.ReceivedCallback onReceived;
    private Subscription.FailedCallback onFailure;

    @SuppressWarnings("unchecked")
    /*package*/ SubscriptionProxy(Consumer consumer, Channel channel, Class<T> subscription) {
        this.consumer = consumer;
        this.channel = channel;
        // Create an implementation of the API defined by the Subscription interface.
        this.proxy = (T) Proxy.newProxyInstance(
                subscription.getClassLoader(),
                new Class<?>[]{subscription},
                new CustomInvocationHandler(this)
        );
    }

    /*package*/ T getProxy() {
        return proxy;
    }

    /*package*/ String getIdentifier() {
        return channel.toIdentifier();
    }

    /*package*/ void onConnected(Subscription.ConnectedCallback callback) {
        onConnected = callback;
    }

    /*package*/ void onDisconnected(Subscription.DisconnectedCallback callback) {
        onDisconnected = callback;
    }

    /*package*/ void onRejected(Subscription.RejectedCallback callback) {
        onRejected = callback;
    }

    /*package*/ void onReceived(Subscription.ReceivedCallback callback) {
        onReceived = callback;
    }

    /*package*/ void onFailure(Subscription.FailedCallback callback) {
        onFailure = callback;
    }

    /*package*/ void perform(String action, JsonObject data) {
        // TODO data cannot include action key...
        data.addProperty("action", action);
        consumer.send(Command.message(channel.toIdentifier(), data));
    }

    /*package*/ void perform(String action) {
        perform(action, new JsonObject());
    }

    /*package*/ void notifyConnected() {
        if (onConnected != null) {
            onConnected.call();
        }
    }

    /*package*/ void notifyRejected() {
        if (onRejected != null) {
            onRejected.call();
        }
    }

    /*package*/ void notifyReceived(JsonElement data) {
        if (onReceived != null) {
            onReceived.call(data);
        }
    }

    /*package*/ void notifyDisconnected() {
        if (onDisconnected != null) {
            onDisconnected.call();
        }
    }

    /*package*/ void notifyFailed(ActionCableException e) {
        if (onFailure != null) {
            onFailure.call(e);
        }
    }

    private static class CustomInvocationHandler implements InvocationHandler {

        private SubscriptionProxy subscriptionProxy;

        CustomInvocationHandler(SubscriptionProxy subscriptionProxy) {
            this.subscriptionProxy = subscriptionProxy;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Perform performAnnotation;
            if (method.getDeclaringClass() == Object.class) {
                // Methods from Object
                return method.invoke(this, args);
            } else if (method.getDeclaringClass() == Subscription.class) {

                if (method.getName().equals("getIdentifier")) {
                    return subscriptionProxy.getIdentifier();
                } else if (method.getName().equals("onConnected")) {
                    subscriptionProxy.onConnected((Subscription.ConnectedCallback) args[0]);
                    return proxy;
                } else if (method.getName().equals("onDisconnected")) {
                    subscriptionProxy.onDisconnected((Subscription.DisconnectedCallback) args[0]);
                    return proxy;
                } else if (method.getName().equals("onRejected")) {
                    subscriptionProxy.onRejected((Subscription.RejectedCallback) args[0]);
                    return proxy;
                } else if (method.getName().equals("onReceived")) {
                    subscriptionProxy.onReceived((Subscription.ReceivedCallback) args[0]);
                    return proxy;
                } else if (method.getName().equals("onFailed")) {
                    subscriptionProxy.onFailure((Subscription.FailedCallback) args[0]);
                    return proxy;
                } else if (method.getName().equals("perform") && method.getParameterTypes().length == 1) {
                    subscriptionProxy.perform((String) args[0]);
                } else if (method.getName().equals("perform") && method.getParameterTypes().length == 2) {
                    subscriptionProxy.perform((String) args[0], (JsonObject) args[1]);
                }
            } else if ((performAnnotation = getPerformAnnotation(method)) != null) {
                // TODO cache
                final String action = performAnnotation.value();
                final Class<?>[] parameterTypes = method.getParameterTypes();
                final Annotation[][] annotations = method.getParameterAnnotations();

                if (parameterTypes.length == 0 && annotations.length == 0) {
                    subscriptionProxy.perform(action);
                } else {
                    if (parameterTypes.length != annotations.length) {
                        throw new IllegalArgumentException("All method parameters must be annotated");
                    }

                    final JsonObject data = new JsonObject();

                    for (int i = 0; i < parameterTypes.length; i++) {
                        final Annotation[] currentAnnotations = annotations[i];
                        final Data dataAnnotation = getDataAnnotation(currentAnnotations);

                        if (dataAnnotation == null) {
                            throw new IllegalArgumentException(i + "th parameter of " + method.getDeclaringClass().getName() + "#" + method.getName() + " must be annotated by @Data.");
                        }

                        if (args[i] == null) {
                            data.add(dataAnnotation.value(), JsonNull.INSTANCE);
                        } else if (args[i] instanceof Number) {
                            data.addProperty(dataAnnotation.value(), (Number) args[i]);
                        } else if (args[i] instanceof Boolean) {
                            data.addProperty(dataAnnotation.value(), (Boolean) args[i]);
                        } else if (args[i] instanceof String || args[i] instanceof Character) {
                            data.addProperty(dataAnnotation.value(), (String) args[i]);
                        } else if (args[i] instanceof JsonElement) {
                            data.add(dataAnnotation.value(), (JsonElement) args[i]);
                        } else {
                            throw new IllegalArgumentException("Type of " + i + "th parameter of " + method.getDeclaringClass().getName() + "#" + method.getName() + " is not supported.");
                        }
                    }

                    subscriptionProxy.perform(action, data);
                }
            }
            return null;
        }

        private Perform getPerformAnnotation(Method method) {
            return method.getAnnotation(Perform.class);
        }

        private Data getDataAnnotation(Annotation[] annotations) {
            for (Annotation annotation : annotations) {
                if (annotation.annotationType() == Data.class) {
                    return (Data) annotation;
                }
            }
            return null;
        }
    }
}
