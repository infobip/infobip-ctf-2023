package org.ibctf.cloud.model;

import io.micronaut.http.HttpMethod;

public class Notification {

    private String notifyUrl;
    private EventType type;
    private HttpMethod method;

    public Notification() {
    }

    public Notification(String notifyUrl, EventType type, HttpMethod method) {
        this.notifyUrl = notifyUrl;
        this.type = type;
        this.method = method;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public enum EventType {

        BOOT,
        SCRIPT,
        UPLOAD,
        CONFIG;
    }
}
