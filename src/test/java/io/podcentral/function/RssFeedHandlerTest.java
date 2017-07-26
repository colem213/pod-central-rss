package io.podcentral.function;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.BaseRequest;

import io.podcentral.TestUtil;
import io.podcentral.model.ServerlessInput;
import io.podcentral.model.ServerlessOutput;

public class RssFeedHandlerTest {
  @Mock
  HttpResponse<InputStream> rsp;

  @Test
  public void testFeedRetreival() throws IOException, UnirestException {
    MockitoAnnotations.initMocks(this);
    BaseRequest feedReq = mock(BaseRequest.class);
    InputStream feedStream = TestUtil.loadInputStreamFromClasspath("undisclosed-feed.xml");
    when(rsp.getStatus()).thenReturn(200);
    when(rsp.getBody()).thenReturn(feedStream);
    when(feedReq.asBinary()).thenReturn(rsp);
    ServerlessInput input =
        TestUtil.loadJsonFromClasspath(ServerlessInput.class, "aws-api-gateway-req.json");


    RssFeedHandler handler = new RssFeedHandler();
    handler.mockRsp = rsp;
    ServerlessOutput out = handler.handleRequest(input, null);

    assertEquals(new Integer(200), out.getStatusCode());
  }
}
