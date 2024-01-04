package org.trade;

public record ApiErrorResponse(ApiError error, String data, String message) {
}
