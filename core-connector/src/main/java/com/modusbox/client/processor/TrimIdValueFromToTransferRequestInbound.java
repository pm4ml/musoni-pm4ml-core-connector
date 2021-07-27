package com.modusbox.client.processor;

import com.modusbox.client.model.FulfilNotification;
import com.modusbox.client.model.TransferRequestInbound;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;

public class TrimIdValueFromToTransferRequestInbound implements Processor {
    @Override
    public void process(Exchange exchange) throws Exception {
        FulfilNotification fulfilNotification = exchange.getIn().getBody(FulfilNotification.class);
        LinkedHashMap<String,Object> map  = (LinkedHashMap<String, Object>) fulfilNotification.getQuote().getInternalRequest();
        LinkedHashMap<String,String> to = (LinkedHashMap<String, String>) map.get("to");
        String idValueTrimmed = to.get("idValue");
        String mfiCode = idValueTrimmed.substring(0, 3);
        exchange.getIn().setHeader("mfiCode", mfiCode);
        idValueTrimmed = idValueTrimmed.substring(3);
        exchange.getIn().setHeader("idValueTrimmed", idValueTrimmed);

    }
}
