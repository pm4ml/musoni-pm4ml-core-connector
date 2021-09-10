package com.modusbox.client.router;

import com.modusbox.client.customexception.CCCustomException;
import com.modusbox.client.exception.RouteExceptionHandlingConfigurer;
import com.modusbox.client.processor.SetPropertiesForMakerCheckerRepayment;
import com.modusbox.client.processor.TrimIdValueFromToTransferRequestInbound;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.json.JSONException;


public class TransfersRouter extends RouteBuilder {

    private static final String TIMER_NAME_POST = "histogram_post_transfers_timer";
    private static final String TIMER_NAME_PUT = "histogram_put_transfers_timer";

    public static final Counter reqCounterPost = Counter.build()
            .name("counter_post_transfers_requests_total")
            .help("Total requests for POST /transfers.")
            .register();

    public static final Counter reqCounterPut = Counter.build()
            .name("counter_put_transfers_requests_total")
            .help("Total requests for PUT /transfers.")
            .register();

    private static final Histogram reqLatencyPost = Histogram.build()
            .name("histogram_post_transfers_request_latency")
            .help("Request latency in seconds for POST /transfers.")
            .register();

    private static final Histogram reqLatencyPut = Histogram.build()
            .name("histogram_put_transfers_request_latency")
            .help("Request latency in seconds for PUT /transfers.")
            .register();

    private final RouteExceptionHandlingConfigurer exceptionHandlingConfigurer = new RouteExceptionHandlingConfigurer();
    private final TrimIdValueFromToTransferRequestInbound trimMFICode = new TrimIdValueFromToTransferRequestInbound();
    private final SetPropertiesForMakerCheckerRepayment SetPropertiesForMakerCheckerRepayment = new SetPropertiesForMakerCheckerRepayment();

    public void configure() {

        // Add our global exception handling strategy
        exceptionHandlingConfigurer.configureExceptionHandling(this);

        // POST /transfers to send the bill payment
        from("direct:postTransfers").routeId("com.modusbox.postTransfers").doTry()
                .process(exchange -> {
                    reqCounterPost.inc(1); // increment Prometheus Counter metric
                    exchange.setProperty(TIMER_NAME_POST, reqLatencyPost.startTimer()); // initiate Prometheus Histogram metric
                })
                /*
                 * BEGIN processing
                 */
                .log("Request transfer API called (loan repayment)")
                .log("POST transfers")
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Request received, POST /Transfer', " +
                        "null, null, 'Input Payload: ${body}')")
                /*
                 * END processing
                 */


                .doFinally().process(exchange -> {
                    ((Histogram.Timer) exchange.getProperty(TIMER_NAME_POST)).observeDuration(); // stop Prometheus Histogram metric
                }).end()
        ;


        // POST /transfers to send the bill payment
        from("direct:putTransfersByTransferId").routeId("com.modusbox.putTransfersByTransferId").doTry()
                .process(exchange -> {
                    reqCounterPut.inc(1); // increment Prometheus Counter metric
                    exchange.setProperty(TIMER_NAME_PUT, reqLatencyPut.startTimer()); // initiate Prometheus Histogram metric
                })
                /*
                 * BEGIN processing
                 */
                .log("Request transfer API called (loan repayment)")
                .log("PUT /transfers/{transferId}")
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Request received, PUT /transfers/{transferId}', " +
                        "null, null, 'Input Payload: ${body}')")
                .log("Header transferID,${header.transferId}")
                .process(trimMFICode)
                // Prepare Request Body for Make Repayment Call
                .setHeader("MMDWalletChannelId",constant("{{dfsp.channel-id}}"))
                .log("MMDWalletChannelId: ${header.MMDWalletChannelId}")

                .marshal().json()
                .transform(datasonnet("resource:classpath:mappings/postTransfersRequest.ds"))
                .setBody(simple("${body.content}"))
                .marshal().json()
                .log("postTransfersRequest : ${body}")

                // Get a valid Authorization header for oAuth
                // Prepare downstream call
                .to("direct:getAuthHeader")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .toD("{{dfsp.host}}/v1/loans/${header.idValueTrimmed}/transactions?command=repayment")
                .unmarshal().json()
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Response from Musoni API, MakeLoanRepaymentResponse: ${body}', " +
                        "'Tracking the loan repayment response', 'Verify the response', null)")

                // Format the response
                .marshal().json()
                .transform(datasonnet("resource:classpath:mappings/makeRepaymentResponse.ds"))
                .setBody(simple("${body.content}"))
                .marshal().json()

                .process(SetPropertiesForMakerCheckerRepayment)
                .log("commandId: ${exchangeProperty.commandId}")
                .log("isMakerChecker: ${exchangeProperty.isMakerChecker}")
                .log("loanTransactionId in Header: ${exchangeProperty.loanTransactionId}")
                .log("makeRepaymentResponse,${body}")
                .to("direct:choiceRoute")

                .doCatch(HttpOperationFailedException.class,CCCustomException.class, JSONException.class)
                    .log("HttpOperationFailedException Caught")
                    .to("direct:extractCustomErrors")
        /*
         * END processing
         */
                .doFinally().process(exchange -> {
                    ((Histogram.Timer) exchange.getProperty(TIMER_NAME_PUT)).observeDuration(); // stop Prometheus Histogram metric
                }).end()
        ;

        from("direct:choiceRoute")
                .choice()
                    .when(simple("${exchangeProperty.isMakerChecker} == 1"))
                        .log("MakerChecker On")
                        .setBody(simple("{}"))
			            .removeHeaders("*")
                        .to("direct:getAuthHeader")
                        .log("set auth header..... ")
                        .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                        .toD("{{dfsp.host}}/v1/makercheckers/${exchangeProperty.commandId}?command=approve")
                        // .toD("{{dfsp.host}}/v1/makercheckers/3402?command=approve")
                        .log("After MakerChecker APi")
                        .unmarshal().json()

                        .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                                "'Response from Musoni API, ApproveRepayment (MakerCheckerResponse): ${body}', " +
                                "'Tracking the MakerChecker Response', 'Verify the response', null)")
                        .marshal().json()
                        .transform(datasonnet("resource:classpath:mappings/postTransfersResponse.ds"))
                        .setBody(simple("${body.content}"))
                        .marshal().json()
                        .log("Done postTransfersResponseWithMakerChecker")

                    .when(simple("${exchangeProperty.isMakerChecker} == 0 "))
                        .log("MakerChecker Off")
                        .transform(datasonnet("resource:classpath:mappings/postTransfersResponse.ds"))
                        .setBody(simple("${body.content}"))
                        .marshal().json()
                        .log("Done postTransfersResponseWithoutMakerChecker.")
                    .otherwise()
                        .log("Error in MakeRepayment......")
                .endChoice()
        ;

    }
}
