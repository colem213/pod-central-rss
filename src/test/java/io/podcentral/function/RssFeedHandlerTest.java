package io.podcentral.function;

import java.io.IOException;

import org.junit.Test;

import io.podcentral.TestUtil;
import io.podcentral.model.ServerlessInput;

public class RssFeedHandlerTest {
	@Test
	public void testFeedRetreival() throws IOException {
		ServerlessInput input = TestUtil.loadInputFromClasspath("aws-api-gateway-req.json");
		RssFeedHandler handler = new RssFeedHandler();
		handler.handleRequest(input, null);
	}
}
