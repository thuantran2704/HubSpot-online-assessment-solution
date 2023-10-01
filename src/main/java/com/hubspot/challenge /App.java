package com.hubspot.challenge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

public class App {

    private static final String GET_DATASET = "https://candidate.hubteam.com/candidateTest/v3/problem/dataset?userKey=?";
    private static final String POST_RESULT = "https://candidate.hubteam.com/candidateTest/v3/problem/result?userKey=?";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        HttpTransport transport = new NetHttpTransport();
        HttpRequestFactory requestFactory = transport.createRequestFactory();

        try {
            // GET Dataset
            JsonNode dataset = getDataset(requestFactory);

            // Process the data set and return result
            List<Map<String, Object>> results = processData(dataset);

            // Print result
            String jsonPayload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(results);

            // POST results to API
            postResults(requestFactory, results);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static JsonNode getDataset(HttpRequestFactory requestFactory) throws IOException {
        HttpRequest getRequest = requestFactory.buildGetRequest(new GenericUrl(GET_DATASET));
        HttpResponse getResponse = getRequest.execute();

        if (getResponse.getStatusCode() != 200) {
            throw new IOException("Error: Received HTTP code " + getResponse.getStatusCode());
        }

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(getResponse.getContent());
    }

    private static List<Map<String, Object>> processData(JsonNode dataset) {
        JsonNode partners = dataset.get("partners");
        Map<String, Map<String, Set<String>>> countryDateAttendees = new HashMap<>();

        for (JsonNode partner : partners) {
            String country = partner.get("country").asText();
            JsonNode availableDates = partner.get("availableDates");
            countryDateAttendees.putIfAbsent(country, new HashMap<>());

            for (int j = 0; j < availableDates.size() - 1; j++) {
                String date = availableDates.get(j).asText();
                String nextDate = availableDates.get(j + 1).asText();

                if (areConsecutiveDates(date, nextDate)) {
                    countryDateAttendees.get(country).putIfAbsent(date, new HashSet<>());
                    countryDateAttendees.get(country).get(date).add(partner.get("email").asText());
                }
            }
        }

        List<Map<String, Object>> results = new ArrayList<>();
        for (String country : countryDateAttendees.keySet()) {
            Map<String, Set<String>> dateAttendees = countryDateAttendees.get(country);
            String bestStartDate = null;
            int maxCount = 0;

            for (String date : dateAttendees.keySet()) {
                int count = dateAttendees.get(date).size();

                if (count > maxCount || (count == maxCount && date.compareTo(bestStartDate) < 0)) {
                    bestStartDate = date;
                    maxCount = count;
                }
            }

            Map<String, Object> countryResult = new HashMap<>();
            countryResult.put("attendeeCount", maxCount);
            countryResult.put("name", country);
            countryResult.put("startDate", bestStartDate);
            countryResult.put("attendees", new ArrayList<>(dateAttendees.getOrDefault(bestStartDate, new HashSet<>())));
            results.add(countryResult);
        }
        return results;
    }

    private static void postResults(HttpRequestFactory requestFactory, List<Map<String, Object>> results) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> finalResult = new HashMap<>();
        finalResult.put("countries", results);

        String jsonPayload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(finalResult);
        HttpRequest postRequest = requestFactory.buildPostRequest(new GenericUrl(POST_RESULT), ByteArrayContent.fromString("application/json", jsonPayload));
        HttpResponse postResponse = postRequest.execute();

        if (postResponse.getStatusCode() != 200) {
            throw new IOException("Error: Received HTTP code " + postResponse.getStatusCode());
        }

        System.out.println("POST Response Status Code: " + postResponse.getStatusCode());
        System.out.println("POST Response Body: " + postResponse.parseAsString());
    }

    private static boolean areConsecutiveDates(String date1, String date2) {
        LocalDate d1 = LocalDate.parse(date1);
        LocalDate d2 = LocalDate.parse(date2);
        return d2.equals(d1.plusDays(1));
    }
}
