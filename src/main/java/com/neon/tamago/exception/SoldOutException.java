package com.neon.tamago.exception;

public class SoldOutException extends Throwable {
    public SoldOutException(String message) {
        super(message);
    }
}
