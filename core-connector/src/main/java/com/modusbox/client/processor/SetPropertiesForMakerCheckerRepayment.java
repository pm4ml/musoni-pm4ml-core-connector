package com.modusbox.client.processor;

import com.modusbox.client.common.ErrorUtils;
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
            ErrorCode ec = ErrorCode.REQUIRED_FIELD_MISSING;
            ErrorUtils.throwCustomException(ec);
        } else if (isMakerChecker.equals(2)) {
            ErrorCode ec = ErrorCode.CC_LOGICAL_TRANSFORMATION_ERROR;
            ErrorUtils.throwCustomException(ec);
        } else {
            exchange.setProperty("loanTransactionId", loanTransactionId);
            exchange.setProperty("commandId", commandId);
            exchange.setProperty("isMakerChecker", isMakerChecker);
        }
    }
}
