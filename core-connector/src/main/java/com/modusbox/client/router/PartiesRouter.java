package com.modusbox.client.router;

import com.modusbox.client.customexception.CCCustomException;
import com.modusbox.client.exception.RouteExceptionHandlingConfigurer;
import com.modusbox.client.processor.SetPropertiesForClientInfo;
import com.modusbox.client.processor.SetPropertiesForLoanInfo;
import com.modusbox.client.processor.ValidatePhoneNumber;
import com.modusbox.client.processor.TrimIdValueFromHeader;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.json.JSONException;

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
    private final SetPropertiesForLoanInfo setPropertiesForLoanInfo = new SetPropertiesForLoanInfo();
    private final SetPropertiesForClientInfo setPropertiesForClientInfo = new SetPropertiesForClientInfo();
    private final ValidatePhoneNumber validatePhoneNumber = new ValidatePhoneNumber();

    public void configure() {

        // Add our global exception handling strategy
        exceptionHandlingConfigurer.configureExceptionHandling(this);

        // In this case the GET parties will return the loan account with customer details
        from("direct:getPartiesByIdTypeIdValueIdSubValue").routeId("com.modusbox.getPartiesByIdTypeIdValueIdSubValue").doTry()
                .process(exchange -> {
                    reqCounter.inc(1); // increment Prometheus Counter metric
                    exchange.setProperty(TIMER_NAME, reqLatency.startTimer()); // initiate Prometheus Histogram metric
                })
                /*
                 * BEGIN processing
                 */
                .log("Account lookup API called")
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Request received, GET /parties/${header.idType}/${header.idValue}/${header.idSubValue}', " +
                        "null, null, 'fspiop-source: ${header.fspiop-source}')")
                .setBody(simple("{}"))
                .process(trimIdValueFromHeader)
                .setHeader("MFIName", constant("{{dfsp.name}}"))
                // .process(setPropertyForPhoneNo)
                // .log("PhNo after stripping: ${exchangeProperty.strippedPhNo}")

                //Search ClientID By PhNo
                .to("direct:searchClientIdByPhNo")
                //Format the response.Check Client found or not.
                .process(setPropertiesForClientInfo)

                //Get LoanList with clientId
                .to("direct:getAccountListByClientId")

                //Format the response.Check Loan Accounts found or not.And match with Phno or not.
                .process(setPropertiesForLoanInfo)

                .log("EntityId after lookup with with PhNo: ${exchangeProperty.entityId}")
                .log("Idvalue without Zeros Leading: ${exchangeProperty.idValueWithoutZeros}")
                .log("LastName: ${exchangeProperty.lastName}")
                .log("BranchName: ${exchangeProperty.branchName}")
                .log("rejectCodes: ${exchangeProperty.rejectCodes}")
                .log("entityStatusID: ${exchangeProperty.entityStatusID}")
                .log("entityStatusValue: ${exchangeProperty.entityStatusValue}")

                //Get Loan By IdValue
                .to("direct:getLoanByIdValue")
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Final getPartiesResponse: ${body}', " +
                        "null, null, 'Response of GET parties/${header.idType}/${header.idValue}/${header.idSubValue} API')")
                /*
                 * END processing
                 */

                .doCatch(CCCustomException.class, JSONException.class)
                .log("HttpOperationFailedException Caught")
                .to("direct:extractCustomErrors")

                .doFinally().process(exchange -> {
            ((Histogram.Timer) exchange.getProperty(TIMER_NAME)).observeDuration(); // stop Prometheus Histogram metric
        }).end()
        ;

        from("direct:searchClientIdByPhNo")
                .to("direct:getAuthHeader")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .toD("{{dfsp.host}}/v1/search?query=${header.idSubValue}&tenantIdentifier=demo&pretty=true&resource=clients")
                .unmarshal().json()
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Response from Musoni Loan API with idsubValue , ${body}', " +
                        "'Tracking the clientInfo response', 'Verify the response', null)")

                .log("Musoni SearchClientIDByPhNo response,${body}")
                .marshal().json()
                .transform(datasonnet("resource:classpath:mappings/getLoanInfoWithPhNoResponse.ds"))
                .setBody(simple("${body.content}"))
                .marshal().json()
        ;

        from("direct:getAccountListByClientId")
                .to("direct:getAuthHeader")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .toD("{{dfsp.host}}/v1/clients/${exchangeProperty.clientId}/accounts")
                .unmarshal().json()
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Response from Musoni Loan API with AccountId, ${body}', " +
                        "'Tracking the clientInfo response', 'Verify the response', null)")

                .log("Musoni GetAccountLists response,${body}")
                .marshal().json()
                .transform(datasonnet("resource:classpath:mappings/checkLoanAccountList.ds"))
                .setBody(simple("${body.content}"))
                .marshal().json()
        ;

        from("direct:getLoanByIdValue")
                .to("direct:getAuthHeader")
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .toD("{{dfsp.host}}/v1/loans/${header.idValueTrimmed}?associations=repaymentSchedule")
                .unmarshal().json()
                .marshal().json()
                .transform(datasonnet("resource:classpath:mappings/getPartiesResponse.ds"))
                .setBody(simple("${body.content}"))
                .marshal().json()
        ;
    }
}
