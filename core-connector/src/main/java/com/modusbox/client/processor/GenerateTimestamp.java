package com.modusbox.client.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class GenerateTimestamp implements Processor {

    public void process(Exchange exchange) throws Exception {
        exchange.getIn().setHeader("curDate", new SimpleDateFormat("dd-MM-yyyy").format(new Date()));
    }

}
