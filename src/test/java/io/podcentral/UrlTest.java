package io.podcentral;

import java.io.IOException;
import java.net.InetAddress;

import org.junit.Test;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentity;
import com.amazonaws.services.cognitoidentity.AmazonCognitoIdentityClientBuilder;
import com.amazonaws.services.cognitoidentity.model.GetIdRequest;
import com.amazonaws.services.cognitoidentity.model.GetIdResult;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class UrlTest {
  @Test
  public void testUrl() throws IOException {
    AmazonCloudFormation cf = AmazonCloudFormationClientBuilder.defaultClient();
    DescribeStacksResult res = cf.describeStacks(
        new DescribeStacksRequest().withStackName(InetAddress.getLocalHost().getHostName()));
    Stack stack = res.getStacks().get(0);
    Output identityPool = stack.getOutputs().stream()
        .filter(out -> "identityPoolId".equalsIgnoreCase(out.getOutputKey())).findFirst().get();
    Output userPool = stack.getOutputs().stream()
        .filter(out -> "userPoolId".equalsIgnoreCase(out.getOutputKey())).findFirst().get();
    AmazonCognitoIdentity id = AmazonCognitoIdentityClientBuilder.defaultClient();
    GetIdResult idRes = id
        .getId(new GetIdRequest().withIdentityPoolId(identityPool.getOutputValue()).addLoginsEntry(
            String.format("cognito-idp.%s.amazonaws.com/%s", "us-east-2",
                userPool.getOutputValue()),
            "AgoGb3JpZ2luECAaCXVzLWVhc3QtMiKAAkIO0LzAmh5Zhrx2W8PcvoyRmZUc/2u5YynnEVZuoz9qZpw/52S9l83sST2Xkq/GXIoycevKZDuMuLg6gDLVSyqX9kcJuu9l4k7H3KvS230wVZ/AQCp0AOpHZ7L1JpQ1wmgJ6wdZ/+QZCnlgQTsrbNyGBToO9pliPkyi5XIONGTee18Tpti7TG52KMrkDuuFYCyldrSyZcWZiYslXAB0LA2dTufa14HeKabXsc5lD4lGv3a635jWPHadeCnFOJ8vX7cI3ApVmKd/zRAYV2MqReBotMiniFrd7AMElXHBcjlMr7h6NvDXlqFrh+RyMudQybsF82EKyq8ma5NK0RdndjcqpgUIGhAAGgw0MjM2MDE0MDI2MjUiDI7Vbyp5OE+HU1WMlCqDBddj/wORmva0ZNN+oTBPAi5fliLit0eVE5Sy+2gatU0sr+JUcTWVKyci02kW6G5waOCrnxD8NKbgrgxq0IrDcYxaJj039uR4qHbSNZFF0OVHAiQkdfLGrzFy/VXBg4QAfe0ZELIecgu2+R9qgUnSKk8alQWw2RB5bIUoBRsEfkohxSTHlDIl+xHG0avUnsE2HZVJjCkBtmwzHE4ku2N+VoP+8O/HC2YI/02DNOvyqVUrP83Tjt2vQipv/Jed1w4fkpzt7Lp477ovEYWMSp7TUAyzX2Y+h/HyfUApw0Q1U7ztjQzRfMOAswC+yYuK0hlWzeYaG6ByuJMqJ8mScEfgHIo/7FF66birZdcEp86gfZO6x74drG9NLLyHK408WHVjmHOWrHViI3HZfEMay4gS04vWKWfIIoPS9i9osA26sCTPWC969tW3KRlA4hxxsSSpi7dSfG5oS+uKwkRFp3+ai4cRMy5CbC3vqwrREfxuoWhqxUWa9mSw0o2XqY6JVsyAVTQdB5NYfIOQ05UYrt/9bg7NOvqmJtDupf4pUNgZ/tH8t/pnbCC32iJQkxY4OYlvlTE0gotxfOZIzMvbv8/GGNP521lfG8iTZwKydoTZ9tw16iyElCV33+8etYA27KbBeG5SECVZbGyA67fARKFdjq+vu0FRrAQJhmY9Y2/ZQuKQNKx6TBbMVfn7HPSSNBfp/mPmQs+aJGYmXsmus03pOO86Z4sU8xeNhW5Jw5ZjA28hGdB3yWMpGa2aApPS0N93epzWY+A3PJzB/juiVO5ck2CPhD2gNG5+wNL1NHqnV0vhnE73LfQ6ZVhqr5Fq7xD1MHskE7OXhKq07ZkTGH6auD/wMhIwx+2wzQU="));
    System.out.println(idRes.getIdentityId());
  }
}
