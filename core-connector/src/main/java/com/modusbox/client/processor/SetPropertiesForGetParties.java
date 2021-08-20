package com.modusbox.client.processor;

import com.modusbox.client.common.Constants;
import com.modusbox.client.common.ErrorUtils;
import com.modusbox.client.enums.ErrorCode;
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

        if (!entityId.isEmpty())
        {
            exchange.setProperty("lastName", lastName);
            exchange.setProperty("branchName", branchName);
            exchange.setProperty("entityId", entityId);
            exchange.setProperty("entityStatusID", entityStatusID);
            exchange.setProperty("entityStatusValue", entityStatusValue);
            exchange.setProperty("rejectCodes", Constants.REJECT_CODES);
        }
        else
        {
            ErrorCode ec = ErrorCode.ACCOUNT_NOT_EXIST;
            ErrorUtils.throwCustomException(ec);
        }

    }
}
