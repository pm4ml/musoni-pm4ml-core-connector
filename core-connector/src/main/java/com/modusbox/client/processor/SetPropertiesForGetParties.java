package com.modusbox.client.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.json.JSONObject;

public class SetPropertiesForGetParties implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {

        String body = exchange.getIn().getBody(String.class);
        JSONObject respObject = new JSONObject(body);
        String lastName = respObject.getString("lastName");
        String branchName = respObject.getString("branchName");
        String entityId = respObject.getString("entityId");
        String entityStatusID = respObject.getString("entityStatusID");
        String entityStatusValue = respObject.getString("entityStatusValue");

        exchange.setProperty("lastName", lastName);
        exchange.setProperty("branchName", branchName);
        exchange.setProperty("entityId", entityId);
        exchange.setProperty("entityStatusID", entityStatusID);
        exchange.setProperty("entityStatusValue", entityStatusValue);
    }
}
