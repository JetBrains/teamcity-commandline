
package jetbrains.buildServer.core.runtime;

import java.util.LinkedList;

public class ProgressStatus implements IProgressStatus {

  public static final IProgressStatus OK_STATUS = new ProgressStatus(OK, "ok"); //$NON-NLS-1$

  public static final IProgressStatus CANCEL_STATUS = new ProgressStatus(CANCEL, "", null); //$NON-NLS-1$

  private int severity = OK;

  private String message;

  private Throwable exception = null;

  private final LinkedList<IProgressStatus> myChildren = new LinkedList<IProgressStatus>();

  public ProgressStatus(int severity, String message, Throwable exception) {
    setSeverity(severity);
    setMessage(message);
    setException(exception);
  }

  public ProgressStatus(int severity, String message) {
    setSeverity(severity);
    setMessage(message);
  }

  public IProgressStatus[] getChildren() {
    return myChildren.toArray(new IProgressStatus[myChildren.size()]);
  }

  public void add(final IProgressStatus status) {
    severity = Math.max(getSeverity(), status.getSeverity());
    myChildren.add(status);
  }

  public Throwable getException() {
    return exception;
  }

  public String getMessage() {
    if (myChildren.isEmpty()) {
      return message;
    } else {
      final StringBuilder summary = new StringBuilder(message);
      for (IProgressStatus child : myChildren) {
        summary.append("\n").append(child.getMessage());
      }
      return summary.toString();
    }
  }

  public int getSeverity() {
    return severity;
  }

  public boolean isMultiStatus() {
    return myChildren.size() > 0;
  }

  public boolean isOK() {
    return severity < ERROR;
  }

  public boolean matches(int severityMask) {
    return (severity & severityMask) != 0;
  }

  protected void setException(Throwable exception) {
    this.exception = exception;
  }

  protected void setMessage(String message) {
    if (message == null)
      this.message = ""; //$NON-NLS-1$
    else
      this.message = message;
  }

  protected void setSeverity(int severity) {
    this.severity = severity;
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("Status "); //$NON-NLS-1$
    if (severity == OK) {
      buf.append("OK"); //$NON-NLS-1$
    } else if (severity == ERROR) {
      buf.append("ERROR"); //$NON-NLS-1$
    } else if (severity == WARNING) {
      buf.append("WARNING"); //$NON-NLS-1$
    } else if (severity == INFO) {
      buf.append("INFO"); //$NON-NLS-1$
    } else if (severity == CANCEL) {
      buf.append("CANCEL"); //$NON-NLS-1$
    } else {
      buf.append("severity="); //$NON-NLS-1$
      buf.append(severity);
    }
    buf.append(": "); //$NON-NLS-1$
    buf.append(message);
    buf.append(' ');
    buf.append(exception);
    return buf.toString();
  }

}