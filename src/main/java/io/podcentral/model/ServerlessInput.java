package io.podcentral.model;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ServerlessInput {

  private String resource;
  private String path;
  private String httpMethod;
  private Map<String, String> headers;
  private Map<String, String> queryStringParameters;
  @Singular
  private Map<String, String> pathParameters;
  @Singular
  private Map<String, String> stageVariables = new HashMap<>();
  private String body;
  private RequestContext requestContext;
  private Boolean isBase64Encoded;

  @Builder
  @Data
  public static class RequestContext {
    private String accountId;
    private String resourceId;
    private String stage;
    private String requestId;
    private Map<String, String> identity;
    private String resourcePath;
    private String httpMethod;
    private String apiId;
  }
}
