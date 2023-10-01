package com.hubspot.challenge;

import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import org.junit.Test;
import org.mockito.Mockito;
import org.junit.Assert;

public class AppTest {

    @Test
    public void testGetDataset() throws Exception {
        // Given
        HttpRequestFactory requestFactory = Mockito.mock(HttpRequestFactory.class);
        HttpResponse httpResponse = Mockito.mock(HttpResponse.class);
        Mockito.when(httpResponse.getStatusCode()).thenReturn(200);
        Mockito.when(requestFactory.buildGetRequest(Mockito.any())).thenReturn(Mockito.mock(HttpRequest.class));
        Mockito.when(requestFactory.buildGetRequest(Mockito.any()).execute()).thenReturn(httpResponse);

        // When
        JsonNode result = App.getDataset(requestFactory);

        // Then
        assertEquals(200, httpResponse.getStatusCode());
    }
}
