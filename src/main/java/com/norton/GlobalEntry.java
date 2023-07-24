package com.norton;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.SneakyThrows;
import lombok.val;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class GlobalEntry {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(GlobalEntry.class);

    final OkHttpClient client = new OkHttpClient.Builder()
            .dispatcher(new Dispatcher(new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                    60L, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(),
                    runnable -> {
                        Thread thread = new Thread(runnable);
                        thread.setDaemon(true); // Set the thread as daemon
                        return thread;
                    })))
            .build();
    final ObjectMapper objectMapper = new ObjectMapper();


    @SneakyThrows
    private JsonNode makeHttpRequest(final String url) {
        val request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException(response.message());
            }
            return objectMapper.readValue(Objects.requireNonNull(response.body()).string(), JsonNode.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private JsonNode getLocations() {
        final String url = "https://ttp.cbp.dhs.gov/schedulerapi/locations/?temporary=false&inviteOnly=false&operational=true&serviceName=Global%20Entry";

        return makeHttpRequest(url);
    }

    private JsonNode getLocation(final int id) {
        val stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(getLocations().elements(), Spliterator.ORDERED), false);

        val location = stream.filter((e) -> e.get("id").asInt() == id).findFirst();
        if (location.isPresent()) {
            return location.get();
        }
        throw new RuntimeException("Global Entry Location Not Found");
    }

    private JsonNode getSlots(final int locationId, final DateTime startDate, final DateTime endDate) {


        val url = String.format("https://ttp.cbp.dhs.gov/schedulerapi/locations/%s/slots?startTimestamp=%s&endTimestamp=%s", locationId, DateTimeFormatters.TSA.print(startDate), DateTimeFormatters.MIDNIGHT.print(endDate));
        return makeHttpRequest(url);
    }

    private List<JsonNode> getOpenSlots(final int locationId, final DateTime startDate, final DateTime endDate) {
        val slots = getSlots(locationId, startDate, endDate);
        val stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(slots.elements(), Spliterator.ORDERED), false);
        return stream.filter((s) -> s.get("active").asInt() > 0).collect(Collectors.toList());
    }

    public List<JsonNode> getWeekendOr515Slots(final int locationId, final DateTime startDate, final DateTime endDate) {
        return getOpenSlots(locationId, startDate, endDate).stream().filter((s) -> {
            val timestamp = DateTime.parse(s.get("timestamp").asText());
            if (timestamp.dayOfWeek().get() == DateTimeConstants.SATURDAY || timestamp.dayOfWeek().get() == DateTimeConstants.SUNDAY) {
                return true;
            }

            //1035 == 17:15
            return timestamp.getMinuteOfDay() == 1035;

        }).collect(Collectors.toList());
    }

    @SneakyThrows
    public void tellEveryone(final String messageText) {
        final String accountSid = "AC29e6df19f1fa0ee47002ccad1fac2bcf";
        final String token = "2ca0a4187e38f7f8f7c752436c0af399";
        Twilio.init(accountSid, token);
        final List<String> numbers;
        numbers = Lists.newArrayList("+18018750891", "+13853151784");

        numbers.forEach((n) ->
        {
            log.debug("sending message to " + n);
            log.debug("message: " + messageText);
            Message.creator(new PhoneNumber(n),
                    new PhoneNumber(n),
                    messageText).create();
        });

    }
}

    //JFK
//
// https://ttp.cbp.dhs.gov/schedulerapi/locations/5140/slots?startTimestamp=2023-08-12T00%3A00%3A00&endTimestamp=2023-08-19T00%3A00%3A00
// https://ttp.cbp.dhs.gov/schedulerapi/locations/5001/slots?startTimestamp=2023-06-24T00:00:00endTimestamp=2024-06-24T00:00:00

//Locations:
//https://ttp.cbp.dhs.gov/schedulerapi/locations/?temporary=false&inviteOnly=false&operational=true&serviceName=Global%20Entry

//Slot Availability
//https://ttp.cbp.dhs.gov/schedulerapi/slot-availability?locationId=7600


