package com.modusbox.client.router;

import com.modusbox.client.exception.RouteExceptionHandlingConfigurer;
import com.modusbox.client.processor.*;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

public class QuotesRouter extends RouteBuilder {

	private RouteExceptionHandlingConfigurer exceptionHandlingConfigurer = new RouteExceptionHandlingConfigurer();
	private final TrimIdValueFromToQuoteRequest trimMFICode = new TrimIdValueFromToQuoteRequest();
	private final SetPropertiesForPostQuote setPropertiesPostQuote = new SetPropertiesForPostQuote();

	public void configure() {
		// Add our global exception handling strategy
		exceptionHandlingConfigurer.configureExceptionHandling(this);

		from("direct:postQuoterequests")
				.routeId("com.modusbox.postQuoterequests")
				.log("POST Quotes API called")
				.to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
						"'Request received, POST /quoterequests', " +
						"null, null, 'Input Payload: ${body}')")
				.process(trimMFICode)
				.process(setPropertiesPostQuote)

				.to("direct:getAuthHeader")
				.setHeader(Exchange.HTTP_METHOD, constant("GET"))
				.log("Before calling: ${header.idValueTrimmed}")
				.toD("{{dfsp.host}}/v1/loans/${header.idValueTrimmed}?associations=repaymentSchedule")
				.to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
						"'Response from Musoni LoanLookup API: ${body}', " +
						"'Tracking the response', 'Verify the response', null)")

				.log("Musoni response,${body}")
				.bean("postQuoterequestsResponse")
				.to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
						"'Final Response: ${body}', " +
						"null, null, 'Response of POST /quoterequests API')")
		;
    }
}