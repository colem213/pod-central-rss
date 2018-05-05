package io.podcentral.feed.exception;

public class InvalidFeedUrlException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public InvalidFeedUrlException() {}

  public InvalidFeedUrlException(String arg0) {
    super(arg0);
  }

  public InvalidFeedUrlException(Throwable arg0) {
    super(arg0);
  }

  public InvalidFeedUrlException(String arg0, Throwable arg1) {
    super(arg0, arg1);
  }

  public InvalidFeedUrlException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
    super(arg0, arg1, arg2, arg3);
  }

}
