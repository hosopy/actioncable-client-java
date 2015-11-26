package com.hosopy.actioncable;

import com.google.gson.JsonObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class ChannelTest {

    @Test
    public void toIdentifier() {
        final Channel channel = new Channel("AppearanceChannel");
        assertThat(channel.toIdentifier(), is("{\"channel\":\"AppearanceChannel\"}"));
    }

    @Test
    public void addNumberParam() {
        final Channel channel = new Channel("ChatChannel");
        channel.addParam("room_id", 1);
        assertThat(channel.toIdentifier(), is("{\"channel\":\"ChatChannel\",\"room_id\":1}"));
    }

    @Test
    public void addStringParam() {
        final Channel channel = new Channel("ChatChannel");
        channel.addParam("nickname", "cat");
        assertThat(channel.toIdentifier(), is("{\"channel\":\"ChatChannel\",\"nickname\":\"cat\"}"));
    }

    @Test
    public void addBooleanParam() {
        final Channel channel = new Channel("ChatChannel");
        channel.addParam("private", true);
        assertThat(channel.toIdentifier(), is("{\"channel\":\"ChatChannel\",\"private\":true}"));
    }

    @Test
    public void addJsonObjectParam() {
        final Channel channel = new Channel("ChatChannel");
        final JsonObject params = new JsonObject();
        params.addProperty("id", 1);
        params.addProperty("name", "Bob");
        channel.addParam("user", params);
        assertThat(channel.toIdentifier(), is("{\"channel\":\"ChatChannel\",\"user\":{\"id\":1,\"name\":\"Bob\"}}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotAddChannelParam() {
        final Channel channel = new Channel("ChatChannel");
        channel.addParam("channel", "NotificationChannel");
    }
}
