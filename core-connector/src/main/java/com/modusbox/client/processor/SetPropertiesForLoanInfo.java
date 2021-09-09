package com.modusbox.client.processor;

import com.modusbox.client.common.Constants;
import com.modusbox.client.customexception.CCCustomException;
import com.modusbox.client.enums.ErrorCode;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.json.JSONObject;

public class SetPropertiesForLoanInfo implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {

        String body = exchange.getIn().getBody(String.class);
        JSONObject respObject = new JSONObject(body);

        boolean isFoundLoans = respObject.getBoolean("isFoundLoans");
        // String  isMatch = respObject.getString("isMatchLoan");
        boolean isMatchedLoanWithPhNo = respObject.getBoolean("isMatchedLoanWithPhNo");
        String entityId = respObject.getString("entityId");
        String entityStatusID = respObject.getString("entityStatusID");
        String entityStatusValue = respObject.getString("entityStatusValue");

        if (isFoundLoans && isMatchedLoanWithPhNo) {
            exchange.setProperty("entityId", entityId);
            exchange.setProperty("entityStatusID", entityStatusID);
            exchange.setProperty("entityStatusValue", entityStatusValue);
            exchange.setProperty("rejectCodes", Constants.REJECT_CODES);
        } else if (!isFoundLoans) {
            throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_ID_NOT_FOUND, "Account does not exist."));
        } else {
            throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.PHONE_NUMBER_MISMATCH));
        }
    }
}
