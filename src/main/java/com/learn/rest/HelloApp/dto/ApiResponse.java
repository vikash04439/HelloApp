package com.learn.rest.HelloApp.dto;

import com.learn.rest.HelloApp.config.TransactionContext;

import java.time.LocalDateTime;

public class ApiResponse<T> {
    private String transactionId;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public ApiResponse(String message, T data) {
        this.transactionId = TransactionContext.get();
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.data = data;
    }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}

