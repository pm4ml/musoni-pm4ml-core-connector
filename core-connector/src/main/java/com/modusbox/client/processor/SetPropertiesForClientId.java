package com.modusbox.client.processor;

import com.modusbox.client.common.ErrorUtils;
import com.modusbox.client.enums.ErrorCode;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.json.JSONObject;


public class SetPropertiesForClientId implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {

        String body = exchange.getIn().getBody(String.class);
        JSONObject respObject = new JSONObject(body);
        String entityId = respObject.getString("entityId");

        if (!entityId.isEmpty())
        {
            exchange.setProperty("entityId", entityId);
        }
        else
        {
            ErrorCode ec = ErrorCode.ACCOUNT_NOT_EXIST;
            ErrorUtils.throwCustomException(ec);
        }

    }
}
