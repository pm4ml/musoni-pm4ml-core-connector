package com.modusbox.client.router;

import com.modusbox.client.exception.RouteExceptionHandlingConfigurer;
import com.modusbox.client.processor.SetPropertiesForClientId;
import com.modusbox.client.processor.SetPropertiesForGetParties;
import com.modusbox.client.processor.TrimIdValueFromHeader;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;

public class PartiesRouter extends RouteBuilder {

    private static final String TIMER_NAME = "histogram_get_parties_timer";

    public static final Counter reqCounter = Counter.build()
            .name("counter_get_parties_requests_total")
            .help("Total requests for GET /parties.")
            .register();

    private static final Histogram reqLatency = Histogram.build()
            .name("histogram_get_parties_request_latency")
            .help("Request latency in seconds for GET /parties.")
            .register();

    private final TrimIdValueFromHeader trimIdValueFromHeader = new TrimIdValueFromHeader();
    private final RouteExceptionHandlingConfigurer exceptionHandlingConfigurer = new RouteExceptionHandlingConfigurer();
    private final SetPropertiesForGetParties setPropertiesForGetParties = new SetPropertiesForGetParties();
    private final SetPropertiesForClientId setPropertiesForClientId = new SetPropertiesForClientId();

    public void configure() {

        // Add our global exception handling strategy
        exceptionHandlingConfigurer.configureExceptionHandling(this);

        // In this case the GET parties will return the loan account with customer details
        from("direct:getPartiesByIdTypeIdValueSubIdValue").routeId("com.modusbox.getPartiesByIdTypeIdValueSubIdValue").doTry()
                .process(exchange -> {
                    reqCounter.inc(1); // increment Prometheus Counter metric
                    exchange.setProperty(TIMER_NAME, reqLatency.startTimer()); // initiate Prometheus Histogram metric
                })
                /*
                 * BEGIN processing
                 */
                .log("Account lookup API called")
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Request received, GET /parties/${header.idType}/${header.idValue}', " +
                        "null, null, 'fspiop-source: ${header.fspiop-source}')")
                .setBody(simple("{}"))
                .process(trimIdValueFromHeader)
                .setHeader("MFIName", constant("{{dfsp.name}}"))

                //Search ClientID By PhNo
                .to("direct:getAuthHeader")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .toD("{{dfsp.host}}/v1/search?query=${header.idsubValue}&tenantIdentifier=demo&pretty=true&resource=clients")
                .unmarshal().json()
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Response from Musoni Loan API, ${body}', " +
                        "'Tracking the clientInfo response', 'Verify the response', null)")

                .log("Musoni SearchClientIDByPhNo response,${body}")
                .marshal().json()
                .transform(datasonnet("resource:classpath:mappings/getLookUpAccountwithPhnoResponse.ds"))
                .setBody(simple("${body.content}"))
                .marshal().json()

                //Format the response
                .process(setPropertiesForClientId)
                .log("EntityId: ${exchangeProperty.entityId}")

                //Get Accounts
                .to("direct:getAuthHeader")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .toD("{{dfsp.host}}/v1/search?query=${exchangeProperty.entityId}&tenantIdentifier=demo&pretty=true&resource=clients")
                .unmarshal().json()
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Response from Musoni Loan API, ${body}', " +
                        "'Tracking the clientInfo response', 'Verify the response', null)")

                .log("Musoni GetAccountLists response,${body}")
                .log("EntityId: ${exchangeProperty.entityId}")
                .marshal().json()
                .transform(datasonnet("resource:classpath:mappings/getLookUpAccountResponse.ds"))
                .setBody(simple("${body.content}"))
                .marshal().json()
//
//                .log("LastName: ${exchangeProperty.lastName}")
//                .log("BranchName: ${exchangeProperty.branchName}")
//                .log("EntityId: ${exchangeProperty.entityId}")
//
//                .to("direct:getAuthHeader")
//                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
//                .toD("{{dfsp.host}}/v1/loans/${exchangeProperty.entityId}?associations=repaymentSchedule")
//                .unmarshal().json()
//                .marshal().json()
//                .transform(datasonnet("resource:classpath:mappings/getPartiesResponse.ds"))
//                .setBody(simple("${body.content}"))
//                .marshal().json()
//
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Final getPartiesResponse: ${body}', " +
                        "null, null, 'Response of GET parties/${header.idType}/${header.idValue} API')")
                /*
                 * END processing
                 */
                .doCatch(HttpOperationFailedException.class)
                .log("HttpOperationFailedException Caught")
                .to("direct:extractCustomErrors")

                .doFinally().process(exchange -> {
            ((Histogram.Timer) exchange.getProperty(TIMER_NAME)).observeDuration(); // stop Prometheus Histogram metric
        }).end()
        ;
    }
}
