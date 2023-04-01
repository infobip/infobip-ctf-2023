package org.ibctf.cloud.ws;

import io.micronaut.core.annotation.Introspected;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Introspected
public class WebSocketRequest {

    protected static final int REQUEST_QUIT = 0;
    protected static final int REQUEST_NEW_FILE = 1;
    protected static final int REQUEST_DELETE_FILE = 2;
    protected static final int REQUEST_ABORT = 3;
    protected static final int REQUEST_LIST_FILES = 4;
    protected static final int REQUEST_LIST_UUID = 5;

    @NotNull
    private int requestType;
    @NotNull
    @NotEmpty
    @Min(4)
    @Max(255)
    private String message;

    public WebSocketRequest() {
    }

    public WebSocketRequest(int requestType, String message) {
        this.requestType = requestType;
        this.message = message;
    }

    public int getRequestType() {
        return requestType;
    }

    public void setRequestType(int requestType) {
        this.requestType = requestType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
