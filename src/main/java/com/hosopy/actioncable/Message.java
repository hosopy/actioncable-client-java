package com.hosopy.actioncable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

class Message {

    private static final Gson GSON = new Gson();

    @SuppressWarnings("unused")
    private String identifier;

    @SuppressWarnings("unused")
    private String type;

    @SuppressWarnings("unused")
    private JsonElement message;

    /*package*/ static Message fromJson(String json) {
        return GSON.fromJson(json, Message.class);
    }

    /*package*/ String getIdentifier() {
        return identifier;
    }

    /*package*/ String getType() {
        return type;
    }

    /*package*/ JsonElement getMessage() {
        return message;
    }

    /*package*/ boolean isPing() {
        return "_ping".equals(getIdentifier());
    }

    /*package*/ boolean isConfirmation() {
        return "confirm_subscription".equals(getType());
    }

    /*package*/ boolean isRejection() {
        return "reject_subscription".equals(getType());
    }
}
