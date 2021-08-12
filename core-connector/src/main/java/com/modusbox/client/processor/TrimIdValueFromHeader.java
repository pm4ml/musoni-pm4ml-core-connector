package com.modusbox.client.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;


public class TrimIdValueFromHeader implements Processor {

    public void process(Exchange exchange) throws Exception {
        String idValueTrimmed = (String) exchange.getIn().getHeader("idValue");
        // MFI code
        String mfiCode = StringUtils.trimMfiCode(idValueTrimmed,3);
        exchange.getIn().setHeader("mfiCode", mfiCode);
        // Trim off first 3 chars
        idValueTrimmed = StringUtils.trimIdValue(idValueTrimmed,3);
        exchange.getIn().setHeader("idValueTrimmed", idValueTrimmed);
    }

}
