package io.podcentral;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestUtil {
  private TestUtil() {}

  private final static ObjectMapper mapper = new ObjectMapper();

  public static <T> T loadJsonFromClasspath(Class<T> clazz, String filename)
      throws JsonParseException, JsonMappingException, IOException {
    InputStream reqStream = loadInputStreamFromClasspath(filename);
    T input = mapper.readValue(reqStream, clazz);
    return input;
  }

  public static InputStream loadInputStreamFromClasspath(String filename) {
    InputStream reqStream = TestUtil.class.getClass().getResourceAsStream("/" + filename);
    return reqStream;
  }
}
