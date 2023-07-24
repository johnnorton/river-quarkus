package com.norton;

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.joda.time.DateTime;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

import java.time.DayOfWeek;
import java.util.List;
import java.util.stream.Collectors;

@Path("/river")
@Slf4j
public class RiverResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ObjectNode runRiver() {
        River river = new River();
        return river.lookupRiver(new ObjectMapper().createObjectNode().put("greeting", "hello"));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("ge")
    public String runGe() {
        val g = new GlobalEntry();
        val startDate = DateTime.now().getMinuteOfDay() > 1035 ? DateTime.now().plusDays(1) : DateTime.now();

        val slots = g.getWeekendOr515Slots(7600, startDate, DateTime.parse("2023-12-29"));
        val collect =
                slots.stream().collect(Collectors
                        .groupingBy((jsonNode) ->
                                DateTimeFormatters.DATE_ONLY.print(DateTime.parse(jsonNode.get("timestamp").asText()))));


        StringBuilder sb = new StringBuilder();

        if(!collect.isEmpty()) {
            sb.append("GLOBAL ENTRY\n");
            collect.keySet().stream().sorted().forEach((key) -> {
                val dateTime = DateTime.parse(key);
                sb.append(String.format("%s a %s has %s appt.\n", key, DayOfWeek.of(dateTime.dayOfWeek().get()).toString().substring(0,3), collect.get(key).size()));
                log.info("{}} a {} has {} appts.", key, DayOfWeek.of(dateTime.dayOfWeek().get()).toString().substring(0,3), collect.get(key).size());
            });
            sb.append("https://rb.gy/x9nyo");
            g.tellEveryone(sb.toString());
        }

        sb.append("DONE");

        return sb.toString();
    }


    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("creds")
    public String runRiverTest1() {
        final AwsSessionCredentials awsCredentials = resolveCredentials(null);

        return "aws_session_token = " +
                awsCredentials.sessionToken() +
                "\n" +
                "aws_access_key_id = " +
                awsCredentials.accessKeyId() +
                "\n" +
                "aws_secret_access_key = " +
                awsCredentials.secretAccessKey();

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("test")
    public ObjectNode runRiverTest(@Context APIGatewayV2HTTPEvent apiGatewayV2HTTPEvent) throws JsonProcessingException {
        log.info(new ObjectMapper().writeValueAsString(apiGatewayV2HTTPEvent));
        River river = new River();
        return river.lookupRiver(new ObjectMapper().createObjectNode().put("test", "true"));
    }

    public static AwsSessionCredentials resolveCredentials(final String profile) {
        final AwsCredentialsProviderChain.Builder awsCredentialsProviderChainBuilder =
                AwsCredentialsProviderChain.builder()
                        .addCredentialsProvider(EnvironmentVariableCredentialsProvider.create())
                        .addCredentialsProvider(SystemPropertyCredentialsProvider.create());

        if (profile != null) {
            awsCredentialsProviderChainBuilder.addCredentialsProvider(
                    ProfileCredentialsProvider.builder().profileName(profile).build());
        }

        awsCredentialsProviderChainBuilder.addCredentialsProvider(DefaultCredentialsProvider.create());
        final AwsCredentialsProviderChain awsCredentialsProviderChain =
                awsCredentialsProviderChainBuilder.build();

        final StsClient stsClient = StsClient.builder().region(Region.US_EAST_1)
                .credentialsProvider(awsCredentialsProviderChain)
                .build();

        final GetCallerIdentityResponse callerIdentity = stsClient.getCallerIdentity();

        log.info("Credentials for [{}] successfully resolved", callerIdentity.arn());
        return (AwsSessionCredentials) awsCredentialsProviderChain.resolveCredentials();
    }

}



