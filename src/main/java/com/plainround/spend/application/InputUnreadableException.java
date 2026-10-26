package com.plainround.spend.application;

/** The input file as a whole cannot be read (missing, not UTF-8, I/O failure), unlike a bad row. */
public final class InputUnreadableException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates the exception.
   *
   * @param message what is wrong with the input, written for the person running the tool
   * @param cause the underlying failure
   */
  public InputUnreadableException(String message, Throwable cause) {
    super(message, cause);
  }
}
