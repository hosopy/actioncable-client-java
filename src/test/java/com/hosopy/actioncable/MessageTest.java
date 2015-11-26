package com.hosopy.actioncable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class MessageTest {

    @Test
    public void fromJson() {
        final Message message = Message.fromJson(
                "{\"identifier\":\"{\\\"channel\\\":\\\"CommentsChannel\\\"}\",\"message\":{\"foo\":\"bar\"}}");

        assertThat(message.getIdentifier(), is("{\"channel\":\"CommentsChannel\"}"));
        assertThat(message.getMessage().toString(), is("{\"foo\":\"bar\"}"));
    }
}
