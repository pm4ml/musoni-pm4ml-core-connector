package com.modusbox.client.router;

import com.modusbox.client.exception.RouteExceptionHandlingConfigurer;
import com.modusbox.client.processor.*;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;


public class TransfersRouter extends RouteBuilder {

    private RouteExceptionHandlingConfigurer exceptionHandlingConfigurer = new RouteExceptionHandlingConfigurer();

    public void configure() {
        // Add our global exception handling strategy
        exceptionHandlingConfigurer.configureExceptionHandling(this);
        // POST /transfers to send the bill payment
        from("direct:postTransfers")
                .routeId("com.modusbox.postTransfers")
                .log("Request transfer API called (loan repayment)")
                .log("POST transfers")
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Request received, POST /Transfer', " +
                        "null, null, 'Input Payload: ${body}')")
        ;



    }
}
