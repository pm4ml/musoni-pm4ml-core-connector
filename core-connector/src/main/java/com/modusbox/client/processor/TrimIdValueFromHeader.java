package com.modusbox.client.processor;

import com.modusbox.client.common.Constants;
import com.modusbox.client.common.ErrorUtils;
import com.modusbox.client.common.StringUtils;
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

        // Check REQUIRED_FIELD_MISSING ERROR
        if (mfiCode.equals("") || idValueTrimmed.equals("")) {
            ErrorCode ec = ErrorCode.REQUIRED_FIELD_MISSING;
            ErrorUtils.throwCustomException(ec);
        }
        else {
            // Check idVlaue is with only Digits
            boolean isDigits = StringUtils.isOnlyDigits(idValueTrimmed);
            // Check MALFORMED_INPUT ERROR
            if (isDigits == false)
            {
                ErrorCode ec = ErrorCode.MALFORMED_INPUT;
                ErrorUtils.throwCustomException(ec);
            }
        }
    }
}