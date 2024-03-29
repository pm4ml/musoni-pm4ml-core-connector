package com.modusbox.client.processor;

import com.modusbox.client.common.StringUtils;
import com.modusbox.client.customexception.CCCustomException;
import com.modusbox.client.enums.ErrorCode;
import com.modusbox.log4j2.message.CustomJsonMessage;
import com.modusbox.log4j2.message.CustomJsonMessageImpl;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import javax.ws.rs.InternalServerErrorException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

@Component("customErrorProcessor")
public class CustomErrorProcessor implements Processor {

    CustomJsonMessage customJsonMessage = new CustomJsonMessageImpl();

    @Override
    public void process(Exchange exchange) throws Exception {
        String reasonText = "{ \"statusCode\": \"5000\"," +
                "\"message\": \"Unknown\" }";
        String statusCode = "5000";
        int httpResponseCode = 500;

        JSONObject errorResponse = null;
        boolean errorFlag = false;

        String errorDescription = "Downstream API failed.";
        // The exception may be in 1 of 2 places
        Exception exception = exchange.getException();
        if (exception == null) {
            exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        }

        if (exception != null) {
            if (exception instanceof HttpOperationFailedException) {
                HttpOperationFailedException e = (HttpOperationFailedException) exception;
                try {
                    if (null != e.getResponseBody()) {
                        /* Below if block needs to be changed as per the error object structure specific to
                            CBS back end API that is being integrated in Core Connector. */
                        JSONObject respObject = new JSONObject(e.getResponseBody());

                        if (respObject.has("returnStatus")) {
                            statusCode = String.valueOf(respObject.getInt("returnCode"));
                            errorDescription = respObject.getString("returnStatus");
                        } else if (e.getStatusCode() == 401 || e.getStatusCode() == 403) {
                            errorFlag = true;
                            errorResponse = new JSONObject(ErrorCode.getErrorResponse(ErrorCode.DESTINATION_COMMUNICATION_ERROR, StringUtils.parseJsonString(respObject.getString("message"))));
                        }else if (respObject.has("errors")) {
                                errorFlag = true;
                            JSONArray arayObject = respObject.getJSONArray("errors");
                            JSONObject errorObject = (JSONObject) arayObject.get(0);
                            errorDescription = errorObject.getString("defaultUserMessage");
                            if(errorDescription.contains("PaymentType") && errorDescription.contains("does not exist"))
                            {
                                errorResponse = new JSONObject(ErrorCode.getErrorResponse(ErrorCode.PAYMENT_TYPE_NOT_FOUND, StringUtils.parseJsonString(errorDescription)));
                            }
                            else
                            {

                                errorResponse = new JSONObject(ErrorCode.getErrorResponse(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYEE, StringUtils.parseJsonString(errorDescription)));
                            }
                        }
                         else if(respObject.has("message") && respObject.has("transferState")){
                            statusCode = String.valueOf(respObject.getInt("statusCode"));
                            try {
                                errorDescription = respObject.getJSONObject("transferState").getJSONObject("lastError").getJSONObject("mojaloopError").getJSONObject("errorInformation").getString("errorDescription");
                            }catch (JSONException ex) {
                                errorDescription = "Unknown - no mojaloopError message present";
                            }

                        } else {
                            errorResponse = new JSONObject(ErrorCode.getErrorResponse(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYEE));
                        }
                    }
                } finally {
                    if (errorFlag) {
                        httpResponseCode = errorResponse.getInt("errorCode");
                        errorResponse = errorResponse.getJSONObject("errorInformation");
                        statusCode = String.valueOf(errorResponse.getInt("statusCode"));
                        errorDescription = errorResponse.getString("description");
                    }
                    reasonText = "{ \"statusCode\": \"" + statusCode + "\"," +
                            "\"message\": \"" + errorDescription + "\"} ";
                }
            } else {
                try {
                    if (exception instanceof CCCustomException) {
                        errorResponse = new JSONObject(exception.getMessage());
                    } else if (exception instanceof InternalServerErrorException) {
                        errorResponse = new JSONObject(ErrorCode.getErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR));
                    } else if (exception instanceof ConnectTimeoutException || exception instanceof SocketTimeoutException || exception instanceof HttpHostConnectException) {
                        errorResponse = new JSONObject(ErrorCode.getErrorResponse(ErrorCode.SERVER_TIMED_OUT));
                    } else if (exception instanceof JSONException) {
                        errorResponse = new JSONObject(ErrorCode.getErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, StringUtils.parseJsonString(exception.getMessage())));
                    }
                    else if (exception instanceof UnknownHostException)
                    {
                        errorResponse = new JSONObject(ErrorCode.getErrorResponse(ErrorCode.UNKNOWN_URI));
                    }
                    else {
                        errorResponse = new JSONObject(ErrorCode.getErrorResponse(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYEE));
                    }
                } finally {
                    httpResponseCode = errorResponse.getInt("errorCode");
                    errorResponse = errorResponse.getJSONObject("errorInformation");
                    statusCode = String.valueOf(errorResponse.getInt("statusCode"));
                    errorDescription = errorResponse.getString("description");
                    reasonText = "{ \"statusCode\": \"" + statusCode + "\"," +
                            "\"message\": \"" + errorDescription + "\"} ";
                }
            }
            customJsonMessage.logJsonMessage("error", String.valueOf(exchange.getIn().getHeader("X-CorrelationId")),
                    "Processing the exception at CustomErrorProcessor", null, null,
                    exception.getMessage());
        }

        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, httpResponseCode);
        exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, "application/json");
        exchange.getMessage().setBody(reasonText);
    }
}