package edu.iit.cs513.adventure.api;

import fi.iki.elonen.NanoHTTPD.Response.Status;

/** Expected request failures, separated from unexpected server errors. */
public final class ApiException extends RuntimeException {
    private final Status status;

    public ApiException(Status status, String message) {
        super(message);
        this.status = status;
    }

    public Status status() {
        return status;
    }
}
