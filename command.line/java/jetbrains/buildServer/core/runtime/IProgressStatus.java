
package jetbrains.buildServer.core.runtime;

public interface IProgressStatus {
  
  int OK = 0; /* Debug? */

  int INFO = 0x01;

  int WARNING = 0x02;

  int ERROR = 0x04;

  int CANCEL = 0x08;
  
  int getSeverity();
  
  boolean isOK();  
  
  String getMessage();
  
  Throwable getException();
  
  IProgressStatus[] getChildren();
  
  void add(IProgressStatus status);
  
  boolean matches(int severityMask);  
}