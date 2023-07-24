package com.norton;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@AllArgsConstructor
@Slf4j
public class River {

    final DataService dataService = DataService.builder().dynamoDbClient(
                    DynamoDbClient.builder().credentialsProvider(DefaultCredentialsProvider.create())
                            .region(Region.US_EAST_1).build())
            .build();

    @SneakyThrows

    public ObjectNode lookupRiver(final ObjectNode inputJsonNode) {
        val objectMapper = new ObjectMapper();
        log.debug(inputJsonNode.toPrettyString());
        final boolean test = inputJsonNode.has("test");
        JsonNode mainJson = getJson("main", test);
        JsonNode middleJson = getJson("middle", test);

        mainJson.findPath("date_availability").fieldNames().forEachRemaining((date) ->
        {
            if (mainJson.findPath("date_availability").get(date).get("remaining").asInt() > 0) {
                val mainMessage = "The MAIN has a spot open on " + date.substring(0, 10)
                        + " https://www.recreation.gov/permits/234622";

                val secondsSinceLastMessageSent = dataService.secondsSinceLastMessageSent("MAIN", date.substring(0, 10));

                if (secondsSinceLastMessageSent == -1
                        || secondsSinceLastMessageSent > 150) {
                    dataService.insertMessage("MAIN", date.substring(0, 10));
                    tellEveryone(mainMessage, test);
                }

            } else {
                log.info("nothing on the Main for " + date);
            }
        });

        middleJson.findPath("date_availability").fieldNames().forEachRemaining((date) ->
        {
            if (middleJson.findPath("date_availability").get(date).get("remaining").asInt() > 0) {
                final String middleMessage = "The MIDDLE FORK has a spot open on " + date.substring(0, 10)
                        + "  https://www.recreation.gov/permits/234623";
                final int secondsSinceLastMessageSent = dataService.secondsSinceLastMessageSent("MIDDLE", date.substring(0, 10));

                if (secondsSinceLastMessageSent == -1
                        || secondsSinceLastMessageSent > 150) {
                    dataService.insertMessage("MIDDLE", date.substring(0, 10));
                    tellEveryone(middleMessage, test);
                }
            } else {
                log.info("nothing on the Middle for " + date);
            }
        });

        return objectMapper.createObjectNode().put("status", "done");
    }

    @SneakyThrows
    private void tellEveryone(final String messageText, final boolean johnOnly) {
        final String accountSid = "AC29e6df19f1fa0ee47002ccad1fac2bcf";
        final String token = "2ca0a4187e38f7f8f7c752436c0af399";
        Twilio.init(accountSid, token);
        final List<String> numbers;

        if (johnOnly) {
            numbers = Lists.newArrayList("+18018750891");
        } else {
            numbers = Lists.newArrayList("+18018750891", "+12087316911",
                    "+12087499490", "+12089610342", "+12083165894", "+18017504904");
        }

        //rob, Annika, Liv, Drew, Ash, John, Jack,

        numbers.forEach((n) ->
        {
            log.debug("sending message to " + n);
            log.debug("message: " + messageText);
            Message.creator(new PhoneNumber(n),
                    new PhoneNumber("+18018723731"),
                    messageText).create();
        });

    }

    @SneakyThrows
    private JsonNode getJson(final String riverChoice, final boolean test) {
        // 234622 - 376 = main
        // 234623 - 377 = middlefork

        if (test) {
            val objectmapper = new ObjectMapper();
            final String testJson =
                    "{\"payload\":{\"permit_id\":\"234622\",\"next_available_date\":\"2023-07-31T00:00:00Z\",\"date_availability\":{\"2023-07-31T00:00:00Z\":{\"total\":8,\"remaining\":1,\"show_walkup\":false,\"is_secret_quota\":false}},\"quota_type_maps\":{\"ConstantQuotaUsageByStartDate\":{\"2023-07-31T00:00:00Z\":{\"total\":8,\"remaining\":0,\"show_walkup\":false,\"is_secret_quota\":false}}}}}";
            return objectmapper.readValue(testJson, JsonNode.class);
        }

        final String url;
        if (riverChoice.equalsIgnoreCase("main")) {
            url =
                    "https://www.recreation.gov/api/permits/234622/divisions/376/availability?start_date=2023-07-2T00:00:00.000Z&end_date=2023-08-10T00:00:00.000Z&commercial_acct=false&is_lottery=false";
        } else {
            url =
                    "https://www.recreation.gov/api/permits/234623/divisions/377/availability?start_date=2023-07-27T00:00:00.000Z&end_date=2023-08-10T00:00:00.000Z&commercial_acct=false&is_lottery=false";
        }

        final OkHttpClient client = new OkHttpClient();
        final Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException(response.message());
            }
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(Objects.requireNonNull(response.body()).string(),
                    JsonNode.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {

        String secretName = "/stack1";
        Region region = Region.of("us-east-1");

        // Create a Secrets Manager client
        SecretsManagerClient client = SecretsManagerClient.builder()
                .region(region)
                .build();

        GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();

        GetSecretValueResponse getSecretValueResponse;

        getSecretValueResponse = client.getSecretValue(getSecretValueRequest);

        String secret = getSecretValueResponse.secretString();

        // Your code goes here.
    }
}




