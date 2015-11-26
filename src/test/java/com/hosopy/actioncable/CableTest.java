package com.hosopy.actioncable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

@RunWith(JUnit4.class)
public class CableTest {

    @Test
    public void createWithUri() throws URISyntaxException {
        final Consumer consumer = Cable.createConsumer(new URI("ws://example.com:28080"));
        assertThat(consumer, notNullValue());
    }

    @Test
    public void createWithUriAndOptions() throws URISyntaxException {
        final Consumer consumer = Cable.createConsumer(new URI("ws://example.com:28080"), new Consumer.Options());
        assertThat(consumer, notNullValue());
    }
}
