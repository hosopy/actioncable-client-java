package com.hosopy.actioncable.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Perform annotation defines methods whose calling is forwarded to {@link com.hosopy.actioncable.Subscription#perform(String)}.
 *
 * <pre>{@code
 * interface ChatSubscription extends Subscription {
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
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Perform {
    String value();
}
