package com.modusbox.client.router;

import com.modusbox.client.exception.RouteExceptionHandlingConfigurer;
import com.modusbox.client.processor.SetPropertiesForPostQuote;
import com.modusbox.client.processor.TrimIdValueFromToQuoteRequest;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

public class QuotesRouter extends RouteBuilder {

	private static final String TIMER_NAME = "histogram_post_quoterequests_timer";

	public static final Counter reqCounter = Counter.build()
			.name("counter_post_quoterequests_requests_total")
			.help("Total requests for POST /quoterequests.")
			.register();

	private static final Histogram reqLatency = Histogram.build()
			.name("histogram_post_quoterequests_request_latency")
			.help("Request latency in seconds for POST /quoterequests.")
			.register();


	private final RouteExceptionHandlingConfigurer exceptionHandlingConfigurer = new RouteExceptionHandlingConfigurer();
	private final TrimIdValueFromToQuoteRequest trimMFICode = new TrimIdValueFromToQuoteRequest();
	private final SetPropertiesForPostQuote setPropertiesPostQuote = new SetPropertiesForPostQuote();

	public void configure() {
		// Add our global exception handling strategy
		exceptionHandlingConfigurer.configureExceptionHandling(this);

		from("direct:postQuoteRequests").routeId("com.modusbox.postQuoteRequests").doTry()
				.process(exchange -> {
					reqCounter.inc(1); // increment Prometheus Counter metric
					exchange.setProperty(TIMER_NAME, reqLatency.startTimer()); // initiate Prometheus Histogram metric
				})
				/*
				 * BEGIN processing
				 */
				.process(exchange -> System.out.println("Strating Post Quote*****"))
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
				.setProperty("origPayload", simple("${body}"))
				.transform(datasonnet("resource:classpath:mappings/postQuoterequestsResponse.ds"))
				.setBody(simple("${body.content}"))
				.marshal().json()
				.to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
						"'Final Response: ${body}', " +
						"null, null, 'Response of POST /quoterequests API')")
				/*
				 * END processing
				 */
				.doFinally().process(exchange -> {
					((Histogram.Timer) exchange.getProperty(TIMER_NAME)).observeDuration(); // stop Prometheus Histogram metric
				}).end()
		;
    }
}