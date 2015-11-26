package com.hosopy.actioncable;

import com.google.gson.*;
import com.google.gson.annotations.Expose;

class Command {

    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Expose
    @SuppressWarnings("unused")
    private final String command;

    @Expose
    @SuppressWarnings("unused")
    private final String identifier;

    @Expose
    @SuppressWarnings("unused")
    private final String data;

    private Command(String command, String identifier) {
        this(command, identifier, null);
    }

    private Command(String command, String identifier, String data) {
        this.command = command;
        this.identifier = identifier;
        this.data = data;
    }

    static Command subscribe(String identifier) {
        return new Command("subscribe", identifier);
    }

    static Command unsubscribe(String identifier) {
        return new Command("unsubscribe", identifier);
    }

    static Command message(String identifier, JsonObject params) {
        return new Command("message", identifier, params.toString());
    }

    /*package*/ String toJson() {
        return GSON.toJson(this);
    }
}
