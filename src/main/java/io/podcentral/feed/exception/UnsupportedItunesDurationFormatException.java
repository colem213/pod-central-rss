package io.podcentral.feed.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class UnsupportedItunesDurationFormatException extends Exception {
  private static final long serialVersionUID = 1L;

  public UnsupportedItunesDurationFormatException(String message, Throwable cause,
      boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public UnsupportedItunesDurationFormatException(String message, Throwable cause) {
    super(message, cause);
  }

  public UnsupportedItunesDurationFormatException(String message) {
    super(message);
  }

  public UnsupportedItunesDurationFormatException(Throwable cause) {
    super(cause);
  }
}
