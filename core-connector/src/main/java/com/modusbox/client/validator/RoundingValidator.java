package com.modusbox.client.validator;

import com.modusbox.client.customexception.CCCustomException;
import com.modusbox.client.enums.ErrorCode;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class RoundingValidator implements Processor {

    public void process(Exchange exchange) throws Exception {

        float amount =  Float.parseFloat((String) exchange.getProperty("amount"));
        short roundingValue = Short.parseShort((String)exchange.getProperty("roundingvalue"));

        System.out.println("Amount in request body:"+amount);
        System.out.println ("Rounding Value:"+roundingValue);

        if ((amount%roundingValue) != 0) {
            throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.ROUNDING_VALUE_ERROR, ErrorCode.ROUNDING_VALUE_ERROR.getDefaultMessage().replace("XXXX",String.valueOf(roundingValue))));
        }
    }
}
