package com.modusbox.client.router;

import com.modusbox.client.exception.RouteExceptionHandlingConfigurer;
import com.modusbox.client.processor.*;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

public class TransferRouterPut extends RouteBuilder {

    private RouteExceptionHandlingConfigurer exceptionHandlingConfigurer = new RouteExceptionHandlingConfigurer();
    private GenerateTimestamp generateTimestamp = new GenerateTimestamp();
    private final TrimIdValueFromToTransferRequestInbound trimMFICode = new TrimIdValueFromToTransferRequestInbound();
    private final SetPropertiesForMakerCheckerRepayment SetPropertiesForMakerCheckerRepayment = new SetPropertiesForMakerCheckerRepayment();
    @Override
    public void configure() throws Exception {

        exceptionHandlingConfigurer.configureExceptionHandling(this);
        // POST /transfers to send the bill payment
        from("direct:putTransfers")
                .routeId("com.modusbox.transfersTransferIdPut")
                .log("Request transfer API called (loan repayment)")
                .log("PUT /transfers/{transferId}")
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Request received, PUT /transfers/{transferId}', " +
                        "null, null, 'Input Payload: ${body}')")
                .process(trimMFICode)
                // Prepare Request Body for Make Repayment Call
                // Generate timeStamp which we need in mapping
                .process(generateTimestamp)
                .setHeader("MMDWalletChannelId",constant("{{dfsp.channel-id}}"))
                .log("MMDWalletChannelId: ${header.MMDWalletChannelId}")
                .bean("postTransfersRequest")
                .log("postTransfersRequest : ${body}")

                // Get a valid Authorization header for oAuth
                // Prepare downstream call
                .to("direct:getAuthHeader")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .toD("{{dfsp.host}}/v1/loans/${header.idValueTrimmed}/transactions?command=repayment")
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Response from Musoni API, MakeLoanRepaymentResponse: ${body}', " +
                        "'Tracking the loan repayment response', 'Verify the response', null)")

                // Format the response
                .bean("makeRepaymentResponse")
                .process(SetPropertiesForMakerCheckerRepayment)
                .log("commandId: ${exchangeProperty.commandId}")
                .log("isMakerChecker: ${exchangeProperty.isMakerChecker}")
                .log("loanTransactionId in Header: ${exchangeProperty.loanTransactionId}")
                .log("makeRepaymentResponse,${body}")

                .choice()
                    .when().simple("${exchangeProperty.isMakerChecker} == 1")
                        .log("MakerChecker On")
                        .setBody(simple("{}"))
                        .to("direct:getAuthHeader")
                        .log("set auth header..... ")
                        .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                        .toD("{{dfsp.host}}/v1/makercheckers/${exchangeProperty.commandId}?command=approve")
                        .log("After MakerChecker APi")
                        .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                                "'Response from Musoni API, ApproveRepayment (MakerCheckerResponse): ${body}', " +
                                "'Tracking the MakerChecker Response', 'Verify the response', null)")
                        .bean("postTransfersResponse")
                        .log("Done postTransfersResponseWithMakerChecker")

                    .when().simple("${exchangeProperty.isMakerChecker} == 0 ")
                        .log("MakerChecker Off")
                        .bean("postTransfersResponse")
                        .log("Done postTransfersResponseWithoutMakerChecker.")
                    .otherwise()
                        .log("Error in MakeRepayment......")
                .end()
        ;

    }
}
