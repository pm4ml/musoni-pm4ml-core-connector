package com.modusbox.client.processor;

import com.modusbox.client.customexception.CCCustomException;
import com.modusbox.client.enums.ErrorCode;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.json.JSONObject;

public class SetPropertiesForMakerCheckerRepayment implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {

        String body = exchange.getIn().getBody(String.class);
        JSONObject respObject = new JSONObject(body);

        String commandId = respObject.getString("commandId");
        Integer loanTransactionId = respObject.getInt("loanTransactionId");
        Integer isMakerChecker = respObject.getInt("isMakerChecker");

        if (commandId == null || commandId == "" || loanTransactionId == null) {
            throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.MISSING_MANDATORY_ELEMENT));
        } else if (isMakerChecker.equals(2)) {
            throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR));
        } else {
            exchange.setProperty("loanTransactionId", loanTransactionId);
            exchange.setProperty("commandId", commandId);
            exchange.setProperty("isMakerChecker", isMakerChecker);
        }
    }
}
