
package com.jetbrains.teamcity;

public class ECommunicationException extends Exception {

  public ECommunicationException(final String message) {
    super(message);
  }

  public ECommunicationException(final Throwable e) {
    super(e);
  }

  public ECommunicationException(final String message, final Throwable e) {
    super(message, e);
  }

  private static final long serialVersionUID = 1L;

}