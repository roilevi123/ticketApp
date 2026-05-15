package com.ticketing.ticketapp.Appliction;

public class Response<T> {
    private final boolean success;
    private final String message;
    private final T data;

    private Response(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }


    public static <T> Response<T> success(T data) {
        return new Response<>(true, null, data);
    }

    public static <T> Response<T> success(String message, T data) {
        return new Response<>(true, message, data);
    }

    public static <T> Response<T> error(String message) {
        return new Response<>(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }
    
    public boolean isError() {
        return !success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}