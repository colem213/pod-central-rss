package io.podcentral.exception;

import lombok.Getter;

@Getter
public class BadResponseException extends Exception {
  private static final long serialVersionUID = 1L;
  private String url;

  public BadResponseException(String url) {
    super();
    this.url = url;
  }

  public BadResponseException(String url, String message) {
    super(message);
    this.url = url;
  }
}
