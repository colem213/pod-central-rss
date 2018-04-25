package io.podcentral.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.podcentral.exception.SerializeOutputException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ServerlessOutput {
  private Integer statusCode;
  private Map<String, String> headers;
  private String body;

  public static ServerlessOutput badRequest(String type, String message) {
    return builder().statusCode(HttpStatus.SC_BAD_REQUEST).body(new AppError(type, message))
        .build();
  }

  public static ServerlessOutput unauthorized() {
    return builder().statusCode(HttpStatus.SC_UNAUTHORIZED).build();
  }

  public static ServerlessOutput notFound() {
    return builder().statusCode(HttpStatus.SC_NOT_FOUND).build();
  }

  public static ServerlessOutput invalidMethod() {
    return builder().statusCode(HttpStatus.SC_METHOD_NOT_ALLOWED).build();
  }

  public static class ServerlessOutputBuilder {
    private Integer statusCode;
    private Map<String, String> headers = new HashMap<String, String>();
    private String body;
    private ObjectMapper mapper;

    public ServerlessOutputBuilder() {
      headers.put("Access-Control-Allow-Origin", "*");
    }

    public ServerlessOutputBuilder(ObjectMapper mapper) {
      this();
      this.mapper = mapper;
    }

    public ServerlessOutputBuilder body(Object o) {
      try {
        body = mapper.writeValueAsString(o);
      } catch (JsonProcessingException e) {
        throw new SerializeOutputException("Failed to serialize response body");
      }
      return this;
    }
  }
}
