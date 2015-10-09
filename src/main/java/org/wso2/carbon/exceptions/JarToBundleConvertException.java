package org.wso2.carbon.exceptions;

public class JarToBundleConvertException extends Exception {
    private String message;

    public JarToBundleConvertException(String message, Exception exception) {
        super(message, exception);
        this.message = message;
    }

    public JarToBundleConvertException(String message) {
        super(message);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
