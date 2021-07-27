package com.modusbox.client.processor;

import com.modusbox.client.model.FulfilNotification;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SetPropertiesForMakerCheckerRepayment implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {

        HashMap<String,Object>  bodymap = (HashMap<String, Object>) exchange.getIn().getBody();
        Integer commandId= (Integer) bodymap.get("commandId");
        Integer loanTransactionId = (Integer) bodymap.get("loanTransactionId");
        Integer isMakerChecker = (Integer) bodymap.get("isMakerChecker");

        exchange.setProperty("loanTransactionId", loanTransactionId);
        exchange.setProperty("commandId",commandId);
        exchange.setProperty("isMakerChecker",isMakerChecker);
    }
}
