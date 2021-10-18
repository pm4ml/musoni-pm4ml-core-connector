package com.modusbox.client.validator;

import com.modusbox.client.common.Constants;
import com.modusbox.client.customexception.CCCustomException;
import com.modusbox.client.enums.ErrorCode;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class RoundingValidator implements Processor {

    public void process(Exchange exchange) throws Exception {

        float amount =  Float.parseFloat((String) exchange.getProperty("amount"));
        System.out.println("Amount in request body:"+amount);
        System.out.println("Constant Rounding Value:"+Constants.ROUNDING_VALUE);

        if ((amount%Constants.ROUNDING_VALUE) != 0) {
            throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_ID_NOT_FOUND, "Rounding Error"));
        }
    }
}
