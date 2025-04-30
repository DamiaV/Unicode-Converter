package net.darmo_creations.unicode_converter.ui;

public class CodepointException extends Exception {
  public CodepointException(String message) {
    super(message);
  }

  public CodepointException(String message, Throwable cause) {
    super(message, cause);
  }
}
