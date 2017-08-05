package io.podcentral;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

import io.podcentral.function.RssFeedHandler;
import io.podcentral.model.ServerlessInput;
import io.podcentral.model.ServerlessOutput;

@Controller
@SpringBootApplication
public class LambdaLocalApplication {
  @Value("${userId}")
  private String userId;

  RssFeedHandler handler = new RssFeedHandler();

  public static void main(String[] args) {
    SpringApplication.run(LambdaLocalApplication.class, args);
  }

  @CrossOrigin(origins = "*")
  @PostMapping("/subscribe")
  @ResponseBody
  ResponseEntity<String> subscribe(
      @RequestHeader(name = "Authorization", required = false) String auth,
      @RequestBody String body) {
    if (auth != null) {
      DecodedJWT jwt = JWT.decode(auth.split(" ")[1]);
      userId = jwt.getSubject();
    }
    ServerlessInput input = new ServerlessInput();
    input.setBody(body);
    ServerlessOutput output = handler.handleRequest(input, new DefaultContext());

    return new ResponseEntity<String>(output.getBody(), HttpStatus.valueOf(output.getStatusCode()));
  }

  class DefaultContext implements Context {
    private final CognitoIdentity identity = new LambdaLocalApplication.DefaultIdentity();

    @Override
    public String getAwsRequestId() {
      return null;
    }

    @Override
    public String getLogGroupName() {
      return null;
    }

    @Override
    public String getLogStreamName() {
      return null;
    }

    @Override
    public String getFunctionName() {
      return null;
    }

    @Override
    public String getFunctionVersion() {
      return null;
    }

    @Override
    public String getInvokedFunctionArn() {
      return null;
    }

    @Override
    public CognitoIdentity getIdentity() {
      return identity;
    }

    @Override
    public ClientContext getClientContext() {
      return null;
    }

    @Override
    public int getRemainingTimeInMillis() {
      return 0;
    }

    @Override
    public int getMemoryLimitInMB() {
      return 0;
    }

    @Override
    public LambdaLogger getLogger() {
      return null;
    }
  }

  class DefaultIdentity implements CognitoIdentity {
    @Override
    public String getIdentityId() {
      return userId;
    }

    @Override
    public String getIdentityPoolId() {
      return null;
    }
  }
}
