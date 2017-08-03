package io.podcentral.model;

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
}
