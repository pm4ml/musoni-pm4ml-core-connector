package com.modusbox.client.processor;

import com.modusbox.client.model.QuoteRequest;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class SetPropertiesForPostQuote implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {

        QuoteRequest quoteRequest = exchange.getIn().getBody(QuoteRequest.class);
        String quoteId=  quoteRequest.getQuoteId();
        String transactionId= quoteRequest.getTransactionId();
        String amount = quoteRequest.getAmount();
        String expiration = quoteRequest.getExpiration();

        exchange.setProperty("quoteId",quoteId);
        exchange.setProperty("transactionId", transactionId);
        exchange.setProperty("amount", amount);
        exchange.setProperty("expiration", expiration);
    }
}
