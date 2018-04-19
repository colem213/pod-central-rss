package io.podcentral.feed.exception;

public class FeedParseException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public FeedParseException() {
    super();
  }

  public FeedParseException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public FeedParseException(String message, Throwable cause) {
    super(message, cause);
  }

  public FeedParseException(String message) {
    super(message);
  }

  public FeedParseException(Throwable cause) {
    super(cause);
  }
}
