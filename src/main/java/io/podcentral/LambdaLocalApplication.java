package io.podcentral;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import io.podcentral.function.RssFeedHandler;
import io.podcentral.model.ServerlessInput;
import io.podcentral.model.ServerlessOutput;

@Controller
@SpringBootApplication
public class LambdaLocalApplication {

  RssFeedHandler handler = new RssFeedHandler();

  public static void main(String[] args) {
    SpringApplication.run(LambdaLocalApplication.class, args);
  }

  @PostMapping("/")
  @ResponseBody
  ResponseEntity<String> lambda(@RequestBody String body) {
    ServerlessInput input = new ServerlessInput();
    input.setBody(body);
    ServerlessOutput output = handler.handleRequest(input, null);

    return new ResponseEntity<String>(output.getBody(), HttpStatus.valueOf(output.getStatusCode()));
  }
}
