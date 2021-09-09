package com.modusbox.client.processor;

import com.modusbox.client.common.Constants;
import com.modusbox.client.common.StringUtils;
import com.modusbox.client.model.FulfilNotification;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class TrimIdValueFromToTransferRequestInbound implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        FulfilNotification fulfilNotification = exchange.getIn().getBody(FulfilNotification.class);
        String idValueTrimmed = fulfilNotification.getQuote().getInternalRequest().getTo().getIdValue();
        String mfiCode = StringUtils.trimMfiCode(idValueTrimmed, Constants.TRIM_COUNT);
        exchange.getIn().setHeader("mfiCode", mfiCode);
        idValueTrimmed = StringUtils.trimIdValue(idValueTrimmed, Constants.TRIM_COUNT);
        exchange.getIn().setHeader("idValueTrimmed", idValueTrimmed);
    }
}
