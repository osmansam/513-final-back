package edu.iit.cs513.adventure.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;
import java.util.Map;

public final class ApiResponses {
    private static final String JSON = "application/json; charset=utf-8";
    static final Gson GSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();

    private ApiResponses() {
    }

    public static Response json(Response.Status status, Object body) {
        return NanoHTTPD.newFixedLengthResponse(status, JSON, GSON.toJson(body));
    }

    public static Response error(Response.Status status, String message) {
        return json(status, Map.of("error", message));
    }

    public static Response noContent() {
        return NanoHTTPD.newFixedLengthResponse(Response.Status.NO_CONTENT, JSON, "");
    }

    public static Response preflight(String methods) {
        Response response = noContent();
        response.addHeader("Access-Control-Allow-Methods", methods);
        response.addHeader("Access-Control-Allow-Headers", "Content-Type");
        return response;
    }

    public static Response methodNotAllowed(String methods) {
        Response response = error(Response.Status.METHOD_NOT_ALLOWED, "Method not allowed");
        response.addHeader("Allow", methods);
        return response;
    }
}
