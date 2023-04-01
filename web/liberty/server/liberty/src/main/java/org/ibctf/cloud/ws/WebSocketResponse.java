package org.ibctf.cloud.ws;

public class WebSocketResponse<T> {

    private WebSocketStatus statusCode;
    private T message;

    public WebSocketResponse() {
    }

    public WebSocketResponse(WebSocketStatus statusCode, T message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public WebSocketStatus getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(WebSocketStatus statusCode) {
        this.statusCode = statusCode;
    }

    public T getMessage() {
        return message;
    }

    public void setMessage(T message) {
        this.message = message;
    }
}
