package io.podcentral.function;

import java.util.UUID;

import org.mockito.Mock;

import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;

public class ApiProxyControllerUnitTest {
  @Mock
  Context ctx;

  @Mock
  CognitoIdentity identity;

  String id = new UUID(0L, 0L).toString();

  // @Test
  // public void testFeedRetreival() throws IOException, UnirestException {
  // MockitoAnnotations.initMocks(this);
  // BaseRequest feedReq = mock(BaseRequest.class);
  // InputStream feedStream = TestUtil.loadInputStreamFromClasspath("undisclosed-feed.xml");
  // when(ctx.getIdentity()).thenReturn(identity);
  // when(identity.getIdentityId()).thenReturn(id);
  // when(rsp.getStatus()).thenReturn(200);
  // when(rsp.getBody()).thenReturn(feedStream);
  // when(feedReq.asBinary()).thenReturn(rsp);
  // ServerlessInput input =
  // TestUtil.loadJsonFromClasspath(ServerlessInput.class, "aws-api-gateway-req.json");
  // input.setStageVariables(new HashMap<>());
  //
  //
  // ApiProxyController handler = new ApiProxyController();
  // handler.mockRsp = rsp;
  // ServerlessOutput out = handler.handleRequest(input, ctx);
  //
  // assertEquals(HttpStatus.CREATED.value(), out.getStatusCode().intValue());
  // }
}
