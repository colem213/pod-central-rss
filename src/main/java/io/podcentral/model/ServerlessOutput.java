package io.podcentral.model;

import java.util.HashMap;
import java.util.Map;

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

  public static class ServerlessOutputBuilder {
    private Integer statusCode;
    private Map<String, String> headers = new HashMap<String, String>();
    private String body;

    public ServerlessOutputBuilder() {
      headers.put("Access-Control-Allow-Origin", "*");
    }
  }
}
