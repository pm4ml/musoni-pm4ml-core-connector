package com.modusbox.client.processor;

import com.modusbox.client.common.PhoneNumberUtils;
import com.modusbox.client.common.StringUtils;
import com.modusbox.client.customexception.CCCustomException;
import com.modusbox.client.enums.ErrorCode;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class ValidatePhoneNumber implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {

        String idSubvalue = (String) exchange.getIn().getHeader("idSubValue");
        if (idSubvalue.isEmpty()) {
            throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_ID_NOT_FOUND, "Phone Number cannot be blank"));
        } else {// Check idSubValue is with only Digits
            boolean isDigits = StringUtils.isOnlyDigits(idSubvalue);
            // Check MALFORMED_INPUT ERROR
            if (!isDigits) {
                throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.MALFORMED_SYNTAX, "Invalid Phone Number Format"));
            }
//            String strippedPhNo = PhoneNumberUtils.stripCode(idSubvalue);
//            exchange.setProperty("strippedPhNo", strippedPhNo);
        }
    }
}