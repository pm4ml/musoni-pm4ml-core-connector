package com.modusbox.client.router;

import com.modusbox.client.exception.RouteExceptionHandlingConfigurer;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

public class SendMoneyRouter extends RouteBuilder {

    private final RouteExceptionHandlingConfigurer exceptionHandlingConfigurer = new RouteExceptionHandlingConfigurer();

    public void configure() {
        // Add our global exception handling strategy
        exceptionHandlingConfigurer.configureExceptionHandling(this);

        from("direct:postSendMoney")
            .routeId("com.modusbox.postSendMoney")
            .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                    "'Request received, POST /sendmoney', " +
                    "null, null, 'Input Payload: ${body}')")
            .setProperty("origPayload", simple("${body}"))
.process(exchange -> System.out.println())
            .removeHeaders("CamelHttp*")
            .setHeader(Exchange.HTTP_METHOD, constant("POST"))
            .setHeader("Content-Type", constant("application/json"))
            // Prune empty items from the request
            .process("postSendMoneyRequest")
            .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                    "'Calling outbound API, postTransfers, " +
                    "POST {{ml-conn.outbound.host}}', " +
                    "'Tracking the request', 'Track the response', 'Input Payload: ${body}')")
//            .marshal().json(JsonLibrary.Gson)
//.process(exchange -> System.out.println())
            .toD("{{ml-conn.outbound.host}}/transfers?bridgeEndpoint=true")
            .unmarshal().json()
            .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                    "'Response from outbound API, postTransfers: ${body}', " +
                    "'Tracking the response', 'Verify the response', null)")
//.process(exchange -> System.out.println())
            .setProperty("postSendMoneyInitial", body())
            // Send request to accept the party instead of hard coding AUTO_ACCEPT_PARTY: true
            .to("direct:putTransfersAcceptParty")
        ;

        from("direct:putTransfersAcceptParty")
                .routeId("com.modusbox.putTransfersAcceptParty")
                .removeHeaders("CamelHttp*")
                .setHeader(Exchange.HTTP_METHOD, constant("PUT"))
                .setHeader("Content-Type", constant("application/json"))
//.process(exchange -> System.out.println())
                .process("putTransfersAcceptPartyRequest")
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Calling outbound API, putTransfersAcceptParty, " +
                        "PUT {{ml-conn.outbound.host}}/transfers/${exchangeProperty.postSendMoneyInitial?.get('transferId')}', " +
                        "'Tracking the request', 'Track the response', 'Input Payload: ${body}')")
//.process(exchange -> System.out.println())
//                .marshal().json()
                // Instead of having to do a DataSonnet transformation
                .toD("{{ml-conn.outbound.host}}/transfers/${exchangeProperty.postSendMoneyInitial?.get('transferId')}?bridgeEndpoint=true")
                .unmarshal().json()
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Response from outbound API, putTransfersAcceptParty: ${body}', " +
                        "'Tracking the response', 'Verify the response', null)")
                ;

        from("direct:putSendMoneyByTransferId")
                .routeId("com.modusbox.putSendMoneyByTransferId")
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Request received, PUT /sendmoney/${header.transferId}', " +
                        "null, null, 'Input Payload: ${body}')")
                .setProperty("origPayload", simple("${body}"))
                .removeHeaders("CamelHttp*")
                .setHeader(Exchange.HTTP_METHOD, constant("PUT"))
                .setHeader("Content-Type", constant("application/json"))
                // Will convert to JSON and only take the accept quote section
                .process("putTransfersAcceptQuoteRequest")
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Calling outbound API, putTransfersById', " +
                        "'Tracking the request', 'Track the response', " +
                        "'Request sent to PUT https://{{ml-conn.outbound.host}}/transfers/${header.transferId}')")
.process(exchange -> System.out.println())
//                .marshal().json(JsonLibrary.Gson)
//                .marshal().json()
                .toD("{{ml-conn.outbound.host}}/transfers/${header.transferId}?bridgeEndpoint=true")
//                .unmarshal().json(JsonLibrary.Gson)
                .unmarshal().json()
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Response from outbound API, putTransfersById: ${body}', " +
                        "'Tracking the response', 'Verify the response', null)")
        ;
    }
}
