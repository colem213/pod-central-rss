package io.podcentral.function;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.BaseRequest;

import io.podcentral.TestUtil;

public class RssFeedHandlerTest {
  @Mock
  HttpResponse<InputStream> rsp;

  @Mock
  Context ctx;

  @Mock
  CognitoIdentity identity;

  String id = new UUID(0L, 0L).toString();

  @Test
  public void testFeedRetreival() throws IOException, UnirestException {
    MockitoAnnotations.initMocks(this);
    BaseRequest feedReq = mock(BaseRequest.class);
    InputStream feedStream = TestUtil.loadInputStreamFromClasspath("undisclosed-feed.xml");
    when(ctx.getIdentity()).thenReturn(identity);
    when(identity.getIdentityId()).thenReturn(id);
    when(rsp.getStatus()).thenReturn(200);
    when(rsp.getBody()).thenReturn(feedStream);
    when(feedReq.asBinary()).thenReturn(rsp);
    // ServerlessInput input =
    // TestUtil.loadJsonFromClasspath(ServerlessInput.class, "aws-api-gateway-req.json");


    // RssFeedHandler handler = new RssFeedHandler();
    // handler.mockRsp = rsp;
    new UUID(0L, 0L);
    // ServerlessOutput out = handler.handleRequest(input, ctx);

    // assertEquals(HttpStatus.CREATED.value(), out.getStatusCode().intValue());
  }
}
