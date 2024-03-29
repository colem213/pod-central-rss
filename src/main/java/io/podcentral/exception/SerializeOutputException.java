package io.podcentral.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class SerializeOutputException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public SerializeOutputException(String arg0) {
    super(arg0);
  }

  public SerializeOutputException(Throwable arg0) {
    super(arg0);
  }

  public SerializeOutputException(String arg0, Throwable arg1) {
    super(arg0, arg1);
  }

  public SerializeOutputException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
    super(arg0, arg1, arg2, arg3);
  }
}
