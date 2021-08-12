package com.modusbox.client.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.json.JSONObject;

public class SetPropertiesForMakerCheckerRepayment implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {

        String body = exchange.getIn().getBody(String.class);
        JSONObject respObject = new JSONObject(body);

        String commandId= respObject.getString("commandId");
        Integer loanTransactionId = respObject.getInt("loanTransactionId");
        Integer isMakerChecker = respObject.getInt("isMakerChecker");

        exchange.setProperty("loanTransactionId", loanTransactionId);
        exchange.setProperty("commandId",commandId);
        exchange.setProperty("isMakerChecker",isMakerChecker);
    }
}
