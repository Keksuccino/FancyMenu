package de.keksuccino.fancymenu.util.auth;

public class AuthException extends Exception {

    public AuthException(String message, Exception exception) {
        super(message, exception);
    }

    public AuthException(String message) {
        super(message);
    }

}
