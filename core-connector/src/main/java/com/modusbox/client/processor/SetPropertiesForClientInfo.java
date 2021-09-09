package com.modusbox.client.processor;

import com.modusbox.client.customexception.CCCustomException;
import com.modusbox.client.enums.ErrorCode;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.json.JSONObject;

public class SetPropertiesForClientInfo implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {

        String body = exchange.getIn().getBody(String.class);
        JSONObject respObject = new JSONObject(body);
        String clientId = respObject.getString("clientId");
        String lastName = respObject.getString("lastName");
        String branchName = respObject.getString("branchName");

        if (!clientId.isEmpty()) {
            exchange.setProperty("clientId", clientId);
            exchange.setProperty("lastName", lastName);
            exchange.setProperty("branchName", branchName);
        } else {
            throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_ID_NOT_FOUND, "Client not found."));
        }

    }
}
