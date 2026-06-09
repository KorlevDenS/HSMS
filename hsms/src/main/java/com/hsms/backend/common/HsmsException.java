package com.hsms.backend.common;

public class HsmsException extends RuntimeException {

    private final int status;
    private final String action;

    public HsmsException(int status, String message, String action) {
        super(message);
        this.status = status;
        this.action = action;
    }

    public int status() {
        return status;
    }

    public String action() {
        return action;
    }
}
