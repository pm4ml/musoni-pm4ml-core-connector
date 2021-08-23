package com.modusbox.client.processor;

import com.modusbox.client.enums.ErrorCode;
import com.modusbox.log4j2.message.CustomJsonMessage;
import com.modusbox.log4j2.message.CustomJsonMessageImpl;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component("customErrorProcessor")
public class CustomErrorProcessor implements Processor {

    CustomJsonMessage customJsonMessage = new CustomJsonMessageImpl();
    private String statusCode;

    @Override
    public void process(Exchange exchange) throws Exception {

        String reasonText = "{ \"statusCode\": \"5000\"," +
                "\"message\": \"Unknown\" }";
        String statusCode = "5000";
        int httpResponseCode = 500;
        String errorDescription = "", message = "";

        // The exception may be in 1 of 2 places
        Exception exception = exchange.getException();
        if (exception == null) {
            exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        }

        if (exception != null) {
            if (exception instanceof HttpOperationFailedException) {
                HttpOperationFailedException e = (HttpOperationFailedException) exception;
                errorDescription = "Downstream API failed.";
                message = exception.getMessage();

                try {
                    if (null != e.getResponseBody()) {
            /* Below if block needs to be changed as per the error object structure specific to
                CBS back end API that is being integrated in Core Connector. */
                        JSONObject respObject = new JSONObject(e.getResponseBody());
                        if (respObject.has("returnStatus")) {
                            statusCode = String.valueOf(respObject.getInt("returnCode"));
                            errorDescription = respObject.getString("returnStatus");
                        } else if (respObject.has("errors")) {
                            JSONArray arayObject = respObject.getJSONArray("errors");
                            JSONObject errorObject = (JSONObject) arayObject.get(0);
                            if (errorObject.has("errorCode")) {
                                statusCode = String.valueOf(errorObject.getInt("errorCode"));
                                errorDescription = errorObject.getString("errorSource");
                            } else if (errorObject.has("defaultUserMessage")) {
                                ErrorCode ec = ErrorCode.CC_LOGICAL_TRANSFORMATION_ERROR;
                                statusCode = String.valueOf(ec.getStatusCode());
                                httpResponseCode = ec.getHttpResponseCode();
                                errorDescription = errorObject.getString("defaultUserMessage");
                            }
                        } else if (e.getStatusCode() == 500) {
                            ErrorCode ec = ErrorCode.CC_LOGICAL_TRANSFORMATION_ERROR;
                            statusCode = String.valueOf(ec.getStatusCode());
                            httpResponseCode = ec.getHttpResponseCode();
                            errorDescription = respObject.getString("message") + ". " + e.getMessage();
                        } else if (e.getStatusCode() == 401 || e.getStatusCode() == 403) {
                            ErrorCode ec = ErrorCode.INVALID_ACCESS_TOKEN;
                            statusCode = String.valueOf(ec.getStatusCode());
                            httpResponseCode = ec.getHttpResponseCode();
                            errorDescription = respObject.getString("message");
                        } else {
                            if (respObject.has("code")) {
                                statusCode = String.valueOf(respObject.getInt("code"));
                            }
                            if (respObject.has("message")) {
                                errorDescription = respObject.getString("message");
                            }
                            if (respObject.has("httpCode")) {
                                httpResponseCode = respObject.getInt("httpCode");
                            }
                        }
                    }
                } finally {
                    System.out.println("*****Status Code" + statusCode);
                    reasonText = "{ \"statusCode\": \"" + statusCode + "\"," +
                            "\"message\": \"" + errorDescription + "\"} ";
                }
            }
            customJsonMessage.logJsonMessage("error", String.valueOf(exchange.getIn().getHeader("X-CorrelationId")),
                    "Processing the exception at CustomErrorProcessor", null, null,
                    message);
        }
        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, httpResponseCode);
        exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, "application/json");
        exchange.getMessage().setBody(reasonText);
    }
}