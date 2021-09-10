package com.modusbox.client.processor;

import com.modusbox.client.common.Constants;
import com.modusbox.client.common.StringUtils;
import com.modusbox.client.customexception.CCCustomException;
import com.modusbox.client.enums.ErrorCode;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class TrimIdValueFromHeader implements Processor {

    public void process(Exchange exchange) throws Exception {
        String idValueTrimmed = (String) exchange.getIn().getHeader("idValue");
        // MFI code
        String mfiCode = StringUtils.trimMfiCode(idValueTrimmed, Constants.TRIM_COUNT);
        exchange.getIn().setHeader("mfiCode", mfiCode);
        // Trim off first 3 chars
        idValueTrimmed = StringUtils.trimIdValue(idValueTrimmed, Constants.TRIM_COUNT);
        exchange.getIn().setHeader("idValueTrimmed", idValueTrimmed);

        //Remove Leading Zeros of idValueTrimmed
        String idValueWithoutZeros = StringUtils.removeLeadingZeros(idValueTrimmed);
        //Set Property for idValue without leading Zeros
        exchange.setProperty("idValueWithoutZeros", idValueWithoutZeros);

        // Check ACCOUNT_NOT_EXIST ERROR
        if (mfiCode.equals("") || idValueTrimmed.equals("")) {
            throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_ID_NOT_FOUND, "Account does not exist."));
        } else {
            // Check idVlaue is with only Digits
            boolean isDigits = StringUtils.isOnlyDigits(idValueTrimmed);
            // Check MALFORMED_INPUT ERROR
            if (!isDigits) {
                throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.MALFORMED_SYNTAX, "Invalid Account Number Format"));
            }
        }
    }
}