package io.podcentral.function;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import io.podcentral.model.FeedForm;
import io.podcentral.model.ServerlessInput;
import io.podcentral.model.ServerlessOutput;
import io.podcentral.rss.RssFeed;
import lombok.extern.log4j.Log4j2;

/**
 * Lambda function that triggered by the API Gateway event "POST /". It reads all the query
 * parameters as the metadata for this article and stores them to a DynamoDB table. It reads the
 * payload as the content of the article and stores it to a S3 bucket.
 */
@Log4j2
public class RssFeedHandler implements RequestHandler<ServerlessInput, ServerlessOutput> {
  @Override
  public ServerlessOutput handleRequest(ServerlessInput input, Context context) {
    ServerlessOutput output = new ServerlessOutput();
    ObjectMapper mapper = new ObjectMapper();

    try {
      FeedForm form = mapper.readValue(input.getBody(), FeedForm.class);
      log.info("Url={}", form.getFeedUrl());

      JAXBContext jc = JAXBContext.newInstance(RssFeed.class);
      Unmarshaller des = jc.createUnmarshaller();
      HttpResponse<InputStream> rsp = Unirest.get(form.getFeedUrl()).asBinary();
      if (log.isDebugEnabled()) {
        String headers = String.join(", ", rsp.getHeaders().entrySet().stream().map(
            entry -> String.format("[%s=%s]", entry.getKey(), String.join(", ", entry.getValue())))
            .collect(Collectors.toList()));
        log.debug("Status Code={}, Headers={{}}", rsp.getStatus(), headers);
      }
      if (rsp.getStatus() < 200 || rsp.getStatus() >= 400) {
        throw new Exception("Failed request: " + form.getFeedUrl());
      }
      RssFeed feed = (RssFeed) des.unmarshal(rsp.getBody());
      log.trace(feed);

      output.setStatusCode(200);
    } catch (Exception e) {
      output.setStatusCode(500);
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      output.setBody(sw.toString());
    }
    return output;
  }
}
