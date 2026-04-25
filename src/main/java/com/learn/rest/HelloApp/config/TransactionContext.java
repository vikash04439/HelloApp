package com.learn.rest.HelloApp.config;

/**
 * Holds the transaction ID for the current request thread.
 */
public class TransactionContext {

    private static final ThreadLocal<String> transactionId = new ThreadLocal<>();

    public static void set(String id) {
        transactionId.set(id);
    }

    public static String get() {
        return transactionId.get();
    }

    public static void clear() {
        transactionId.remove();
    }
}

