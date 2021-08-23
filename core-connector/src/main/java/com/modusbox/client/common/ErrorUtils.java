package com.modusbox.client.common;

import com.modusbox.client.enums.ErrorCode;
import org.apache.camel.http.base.HttpOperationFailedException;

import java.util.Map;

public final class ErrorUtils {
    public static void throwCustomException(ErrorCode ec) throws HttpOperationFailedException {

        String uri = "";
        Map<String, String> responseHeaders = null;
        int statusCode = ec.getStatusCode();
        int httpCode = ec.getHttpResponseCode();
        String statusText = ec.getDefaultMessage();
        String redirectLocation = "";
        String responseBody = String.format("{ \"code\": \"%s\",\"message\": \"%s\",\"httpCode\": \"%s\"}", statusCode, statusText, httpCode);

        throw new HttpOperationFailedException(uri, statusCode, statusText, redirectLocation, responseHeaders, responseBody);

    }
}
