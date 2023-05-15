package com.modusbox.client.processor;

import com.modusbox.client.model.Currency;
import com.modusbox.client.model.QuoteRequest;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import com.modusbox.client.customexception.CCCustomException;
import com.modusbox.client.enums.ErrorCode;

import javax.validation.constraints.NotNull;

public class SetPropertiesForPostQuote implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        
		QuoteRequest quoteRequest = exchange.getIn().getBody(QuoteRequest.class);
        String quoteId=  quoteRequest.getQuoteId();
        String transactionId= quoteRequest.getTransactionId();
        String amount = quoteRequest.getAmount();
        String expiration = quoteRequest.getExpiration();
        @NotNull Currency currency = quoteRequest.getCurrency();

        exchange.setProperty("quoteId",quoteId);
        exchange.setProperty("transactionId", transactionId);
        exchange.setProperty("amount", amount);
        exchange.setProperty("expiration", expiration);
        exchange.setProperty("currency", currency);

        if (amount.equals("0")){
            throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.PAYEE_LIMIT_ERROR, "Transfer amount cannot be zero value"));
        }
    }
}
