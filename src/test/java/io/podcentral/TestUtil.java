package io.podcentral;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.podcentral.model.ServerlessInput;

public class TestUtil {
	private TestUtil() {
	}

	private final static ObjectMapper mapper = new ObjectMapper();

	public static ServerlessInput loadInputFromClasspath(String filename)
			throws JsonParseException, JsonMappingException, IOException {
		InputStream reqStream = TestUtil.class.getClass().getResourceAsStream("/" + filename);
		ServerlessInput input = mapper.readValue(reqStream, ServerlessInput.class);
		return input;
	}
}
