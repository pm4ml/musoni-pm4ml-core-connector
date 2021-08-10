package com.modusbox.client.router;

import com.modusbox.client.exception.RouteExceptionHandlingConfigurer;
import com.modusbox.client.processor.*;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

public class LoanDisbursementToWalletRouter extends RouteBuilder {

    private static final String TIMER_NAME = "histogram_post_original_disbursement_timer";

    public static final Counter reqCounter = Counter.build()
            .name("counter_post_original_disbursement_requests_total")
            .help("Total requests for POST /quoterequests.")
            .register();

    private static final Histogram reqLatency = Histogram.build()
            .name("histogram_post_original_disbursement_request_latency")
            .help("Request latency in seconds for POST /quoterequests.")
            .register();

    private RouteExceptionHandlingConfigurer exceptionHandlingConfigurer = new RouteExceptionHandlingConfigurer();

    @Override
    public void configure() throws Exception {

        exceptionHandlingConfigurer.configureExceptionHandling(this);

        // POST /transfers to send the bill payment
        from("direct:postOriginalDisbursement").routeId("com.modusbox.postOriginalDisbursement").doTry()
                .process(exchange -> {
                    reqCounter.inc(1); // increment Prometheus Counter metric
                    exchange.setProperty(TIMER_NAME, reqLatency.startTimer()); // initiate Prometheus Histogram metric
                })
                /*
                 * BEGIN processing
                 */
                .log("Request transfer API called (loan disbursement)")
                .log("POST /postOriginalDisbursement")
                //.process(setHeadersLoanDisbursement)
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Request received, POST /transfers', " +
                        "null, null, 'Input Payload: ${body}')")
                //.marshal().json()
                // Prepare request body
                .transform(datasonnet("resource:classpath:mappings/postOriginalDisbursementRequest.ds"))
                .setBody(simple("${body.content}"))
                .marshal().json()
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
                /*
                 * END processing
                 */
                .doFinally().process(exchange -> {
                    ((Histogram.Timer) exchange.getProperty(TIMER_NAME)).observeDuration(); // stop Prometheus Histogram metric
                }).end()
        ;

    }
}
