package com.test.protobuf.client;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit.MockServerConfig;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.consumer.model.MockServerImplementation;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Interaction;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.google.protobuf.InvalidProtocolBufferException;
import com.test.protobuf.grpc.HelloRequest;
import com.test.protobuf.grpc.HelloResponse;
import com.test.protobuf.grpc.HelloServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static au.com.dius.pact.consumer.dsl.PactBuilder.filePath;
import static com.test.protobuf.grpc.HelloServiceGrpc.newBlockingStub;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Main test class for the AreaCalculator calculate service method call.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "protobuf-producer", providerType = ProviderType.SYNCH_MESSAGE, pactVersion = PactSpecVersion.V4)
public class PactConsumerTest {

    /**
     * Configures the Pact interaction for the test. This will load the Protobuf plugin, which will provide all the
     * Protobuf and gRPC support to the Pact framework.
     */
    @Pact(consumer = "protobuf-consumer")
    V4Pact calculateRectangleArea(PactBuilder builder) {
        return builder
                // Tell Pact we need the Protobuf plugin
                .usingPlugin("protobuf")
                // We will use a V4 synchronous message interaction for the test
                .expectsToReceive("greeting", "core/interaction/synchronous-message")
                // We need to pass all the details for the interaction over to the plugin
                .with(Map.of(
                        // Configure the proto file, the content type and the service we expect to invoke
                        "pact:proto", filePath("../proto/HelloService.proto"),
                        "pact:content-type", "application/grpc",
                        "pact:proto-service", "HelloService/hello",

                        // Details on the request message (ShapeMessage) we will send
                        "request", HelloRequest.newBuilder()
                                .setFirstName("Karthik")
                                .setLastName("Patlolla")
                                .build(),

                        // Details on the response message we expect to get back (AreaResponse)
                        "response", HelloResponse.newBuilder().setGreeting("Hello, Karthik Patlolla").build()
                ))
                .toPact();
    }

    /**
     * Main test method. This method will receive a gRPC mock server and example request message, which we will use the
     * generated stub classes to send to the mock server. The mock server will return the AreaResponse message configured
     * from the values in the setup method above.
     */
    @Test
    @PactTestFor(pactMethod = "calculateRectangleArea")
    @MockServerConfig(implementation = MockServerImplementation.Plugin, registryEntry = "protobuf/transport/grpc")
    void calculateRectangleArea(MockServer mockServer, V4Interaction.SynchronousMessages interaction) throws InvalidProtocolBufferException {
        ManagedChannel channel = ManagedChannelBuilder.forTarget("127.0.0.1:" + mockServer.getPort())
                .usePlaintext()
                .build();
        try {
            HelloServiceGrpc.HelloServiceBlockingStub stub = newBlockingStub(channel);

            // Correct request
            HelloResponse helloResponse = stub.hello(HelloRequest.newBuilder()
                    .setFirstName("Karthik")
                    .setLastName("Patlolla")
                    .build());

            // Incorrect request, missing the length field. Uncommenting this will cause the test to fail.
            //AreaCalculator.ShapeMessage.Builder builder = AreaCalculator.ShapeMessage.newBuilder();
            //AreaCalculator.Rectangle rectangle = builder.getRectangleBuilder().setWidth(22).build();
            //shapeMessage = builder.setRectangle(rectangle).build();
            //stub.calculateOne(shapeMessage);
        } finally {
            channel.shutdownNow();
        }
    }
}
