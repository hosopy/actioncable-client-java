package com.hosopy.actioncable.annotation;

import com.google.gson.JsonObject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Data annotation defines parameters to pass to {@link com.hosopy.actioncable.Subscription#perform(String, JsonObject)}.
 *
 * <pre>{@code
 * public interface ChatSubscription extends Subscription {
 *     {@literal @Perform("join")}
 *     void join();
 *
 *     {@literal @Perform("send_message")}
 *     void sendMessage(@Data("body") String body, @Data("private") boolean isPrivate);
 * }
 * }</pre>
 *
 * @author hosopy (https://github.com/hosopy)
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Data {
    String value();
}
