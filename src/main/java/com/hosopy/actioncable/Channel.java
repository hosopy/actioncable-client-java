package com.hosopy.actioncable;

import com.google.gson.*;

/**
 * Channel is a descriptor of a channel.
 *
 * <pre>{@code
 * Channel appearanceChannel = new Channel("AppearanceChannel");
 *
 * // With parameter
 * Channel chatChannel = new Channel("AppearanceChannel");
 * chatChannel.addParam("room", "Best Room");
 * }</pre>
 *
 * @author hosopy
 */
public class Channel {

    private static final Gson GSON = new GsonBuilder().create();

    private static final String KEY_CHANNEL = "channel";

    private final JsonObject params = new JsonObject();

    private String identifier;

    /**
     * Constructor
     *
     * @param channel Channel name
     */
    public Channel(String channel) {
        params.addProperty(KEY_CHANNEL, channel);
    }

    /**
     * Add String parameter.
     *
     * @param key Parameter key
     * @param value Parameter value
     */
    public void addParam(String key, String value) {
        addParamInternal(key, new JsonPrimitive(value));
    }

    /**
     * Add Number parameter.
     *
     * @param key Parameter key
     * @param value Parameter value
     */
    public void addParam(String key, Number value) {
        addParamInternal(key, new JsonPrimitive(value));
    }

    /**
     * Add Boolean parameter.
     *
     * @param key Parameter key
     * @param value Parameter value
     */
    public void addParam(String key, Boolean value) {
        addParamInternal(key, new JsonPrimitive(value));
    }

    /**
     * Add JsonElement parameter.
     *
     * @param key Parameter key
     * @param value Parameter value
     */
    public void addParam(String key, JsonElement value) {
        addParamInternal(key, value);
    }

    /*package*/ String toIdentifier() {
        synchronized (params) {
            if (identifier == null) {
                identifier = GSON.toJson(params);
            }
            return identifier;
        }
    }

    private void addParamInternal(String key, JsonElement value) {
        if (KEY_CHANNEL.equals(key)) {
            throw new IllegalArgumentException("The name '" + KEY_CHANNEL + "' is not allowed to use as a param key.");
        }
        synchronized (params) {
            params.add(key, value);
            identifier = null;
        }
    }
}
