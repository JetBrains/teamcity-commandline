
package com.jetbrains.teamcity;

public class EAuthorizationException extends Exception {

  private static final long serialVersionUID = 1L;

  public EAuthorizationException(final Throwable e) {
    super(e);
  }

  public EAuthorizationException(final String message) {
    super(message);
  }

  public EAuthorizationException(final String message, final Throwable e) {
    super(message, e);
  }

}