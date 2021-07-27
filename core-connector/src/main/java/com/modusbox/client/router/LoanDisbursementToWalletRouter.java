package com.modusbox.client.router;

import com.modusbox.client.exception.RouteExceptionHandlingConfigurer;
import com.modusbox.client.processor.*;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

public class LoanDisbursementToWalletRouter extends RouteBuilder {

    private RouteExceptionHandlingConfigurer exceptionHandlingConfigurer = new RouteExceptionHandlingConfigurer();



    @Override
    public void configure() throws Exception {

        exceptionHandlingConfigurer.configureExceptionHandling(this);
        // POST /transfers to send the bill payment
        from("direct:postOriginalDisbursement")
                .routeId("com.modusbox.postOriginalDisbursement")
                .log("Request transfer API called (loan disbursement)")
                .log("POST /postOriginalDisbursement")
                //.process(setHeadersLoanDisbursement)
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Request received, POST /transfers', " +
                        "null, null, 'Input Payload: ${body}')")
                //.marshal().json()
                // Prepare request body
                .bean("postOriginalDisbursementRequest")
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Calling outbound API, postTransfers, " +
                        "POST {{ml-conn.outbound.host}}', " +
                        "'Tracking the request', 'Track the response', 'Input Payload: ${body}')")
                // Prepare downstream call
                .removeHeaders("CamelHttp*")
                .setHeader("Content-Type", constant("application/json"))
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .toD("{{ml-conn.outbound.host}}/transfers?bridgeEndpoint=true")
                .unmarshal().json()
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Response from Wallet, LoanDisbursementResponse: ${body}', " +
                        "'Tracking the loan repayment response', 'Verify the response', null)")

                .log("Wallet response,${body}")
                .setProperty("postSendMoneyInitial", body())
                // Send request to accept the party instead of hard coding AUTO_ACCEPT_PARTY: true
                .to("direct:putTransfersAcceptParty")
                .log("postOriginalDisbursementResponse,${body}")
                .setHeader("transferId", simple("${exchangeProperty.postSendMoneyInitial?.get('transferId')}"))
                .to("direct:putSendMoneyByTransferId")
        ;

    }
}
