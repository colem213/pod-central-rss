package io.podcentral.feed.exception;

public class RssParseException extends FeedParseException {
  private static final long serialVersionUID = 1L;

  public RssParseException() {
    super();
  }

  public RssParseException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public RssParseException(String message, Throwable cause) {
    super(message, cause);
  }

  public RssParseException(String message) {
    super(message);
  }

  public RssParseException(Throwable cause) {
    super(cause);
  }
}
