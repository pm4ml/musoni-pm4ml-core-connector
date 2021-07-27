package com.modusbox.client.router;

import com.modusbox.client.exception.RouteExceptionHandlingConfigurer;
import com.modusbox.client.processor.*;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;


public class PartiesRouter extends RouteBuilder {

	private RouteExceptionHandlingConfigurer exceptionHandlingConfigurer = new RouteExceptionHandlingConfigurer();
	private final TrimIdValueFromHeader trimIdValueFromHeader = new TrimIdValueFromHeader();
    public void configure() {
		// Add our global exception handling strategy
		exceptionHandlingConfigurer.configureExceptionHandling(this);

		// In this case the GET parties will return the loan account with customer details
		from("direct:getParties")
				.routeId("com.modusbox.getParties")
				.log("Account lookup API called")
				.to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
																	"'Request received, GET /parties/${header.idType}/${header.idValue}', " +
																	"null, null, 'fspiop-source: ${header.fspiop-source}')")
				.setBody(simple("{}"))
				.process(trimIdValueFromHeader)
				.to("direct:getAuthHeader")
				.setHeader(Exchange.HTTP_METHOD, constant("GET"))
				.toD("{{dfsp.host}}/v1/search?query=${header.idValueTrimmed}&resource=loans&exactMatch=true")
				.to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
																	"'Response from Musoni Loan API, ${body}', " +
																	"'Tracking the clientInfo response', 'Verify the response', null)")

				.log("Musoni getLoanInfo response,${body}")
				// Format the response
				.bean("getPartiesResponse")
				.to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
																	"'Final getPartiesResponse: ${body}', " +
																	"null, null, 'Response of GET parties/${header.idType}/${header.idValue} API')")
		;

	}
}
