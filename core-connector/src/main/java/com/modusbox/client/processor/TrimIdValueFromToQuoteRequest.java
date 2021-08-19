package com.modusbox.client.processor;

import com.modusbox.client.common.Constants;
import com.modusbox.client.common.StringUtils;
import com.modusbox.client.model.QuoteRequest;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class TrimIdValueFromToQuoteRequest implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        QuoteRequest quoteRequest = exchange.getIn().getBody(QuoteRequest.class);
        String idValueTrimmed =  quoteRequest.getTo().getIdValue();

        // MFI code
        String mfiCode = StringUtils.trimMfiCode(idValueTrimmed, Constants.TRIM_COUNT);
        exchange.getIn().setHeader("mfiCode", mfiCode);
        // Trim off first 3 chars
        idValueTrimmed = StringUtils.trimIdValue(idValueTrimmed, Constants.TRIM_COUNT);
        exchange.getIn().setHeader("idValueTrimmed", idValueTrimmed);
    }
}
