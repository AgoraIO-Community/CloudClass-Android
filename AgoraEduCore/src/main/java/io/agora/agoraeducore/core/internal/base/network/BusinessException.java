package io.agora.agoraeducore.core.internal.base.network;

import androidx.annotation.Nullable;

public class BusinessException extends RuntimeException {
    private final int code;
    private final String message;
    private final int httpCode;

    public BusinessException(int code, @Nullable String message) {
        this.code = code;
        this.message = message;
        this.httpCode = -1;
    }

    public BusinessException(int code, @Nullable String message, int httpCode) {
        this.code = code;
        this.message = message;
        this.httpCode = httpCode;
    }

    public BusinessException(@Nullable String message) {
        this.code = -1;
        this.message = message;
        this.httpCode = -1;
    }

    public int getCode() {
        return code;
    }

    @Nullable
    @Override
    public String getMessage() {
        return message;
    }

    public int getHttpCode() {
        return httpCode;
    }
}
