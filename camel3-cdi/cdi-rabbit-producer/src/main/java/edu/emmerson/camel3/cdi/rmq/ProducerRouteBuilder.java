package edu.emmerson.camel3.cdi.rmq;


import java.time.Instant;
import java.util.UUID;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.metrics.routepolicy.MetricsRoutePolicy;


public class ProducerRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
    	
    	long lpts = ConfigReader.getProcessTimeSimulationMs();

        // configure we want to use undertow as the component for the rest DSL
        restConfiguration().component("undertow")
            .contextPath("/").host("0.0.0.0").port(8080)
            .apiContextPath("/api-doc")
            	.apiContextRouteId("api-doc-endpoint")
                .apiProperty("api.title", "Producer API").apiProperty("api.version", "1.0.0")
                // and enable CORS
                .apiProperty("cors", "true");

        // this user REST service is json only
        rest("/rmq").id("rmq-endpoint").description("RabbitMQ rest service")
            .consumes("application/json").produces("application/json")

            .post("/publish").id("rmq-publish-resource").description("Publish a message in RabbitMQ")
                .responseMessage().code(204).message("Message storaged").endResponseMessage()
                .to("direct:publishMessage");

        String producer_rabbitmq_route_id = RouteIdGenerator.newRouteId("mymscode", "myqueueName", RouteIdGenerator.RouteType.AMQP_PRODUCER);	
		MetricsRoutePolicy mrp = MetricsFactory.createMetricsRoutePolicy(producer_rabbitmq_route_id);

        from("direct:publishMessage")
        .routeId(ProducerRouteBuilder.class.getName())
        .routePolicy(mrp)
        .process().message(m -> {
        	m.setHeader("custom.messageId", UUID.randomUUID().toString());
			m.setHeader("custom.currentTimeMillis", System.currentTimeMillis());
			m.setHeader("custom.Date", Instant.now().toString());
			m.setHeader("custom.XcorrelationID", m.getHeader("X-Correlation-ID"));
			try {
				Thread.sleep(lpts);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        })
        .log("Message to be sent: ${body}")
        .to(ConfigReader.getQueueEndpoint());
    }

}
