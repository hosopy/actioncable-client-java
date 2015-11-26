package com.hosopy.actioncable;

import com.google.gson.JsonObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class CommandTest {

    @Test
    public void subscribe() {
        final Command command = Command.subscribe("identifier");
        assertThat(command.toJson(), is("{\"command\":\"subscribe\",\"identifier\":\"identifier\"}"));
    }

    @Test
    public void unsubscribe() {
        final Command command = Command.unsubscribe("identifier");
        assertThat(command.toJson(), is("{\"command\":\"unsubscribe\",\"identifier\":\"identifier\"}"));
    }

    @Test
    public void message() {
        final JsonObject data = new JsonObject();
        data.addProperty("foo", "bar");
        final Command command = Command.message("identifier", data);
        assertThat(command.toJson(), is("{\"command\":\"message\",\"identifier\":\"identifier\",\"data\":\"{\\\"foo\\\":\\\"bar\\\"}\"}"));
    }
}
