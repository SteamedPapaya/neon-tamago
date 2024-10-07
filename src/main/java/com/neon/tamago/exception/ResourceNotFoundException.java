package com.neon.tamago.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException( String message ) {
        super( message );
    }
}
