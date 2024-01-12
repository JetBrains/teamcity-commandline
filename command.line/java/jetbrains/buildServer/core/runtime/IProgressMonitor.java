
package jetbrains.buildServer.core.runtime;


public interface IProgressMonitor {
  /**
   * begins new Task 
   */
  void beginTask(final String taskName);
  /**
   * report new Status for the Monitor 
   */
  void status(final IProgressStatus status);
  /**
   * completing current Task 
   */
  void done();
  /**
   * mark the Monitor as canceled 
   */
  void cancel();
  /**
   * @return true if the Monitor was canceled ant false otherwise
   */
  boolean isCancelled();
  
  
//  
//  @Deprecated
//  void out(final String message);
//  @Deprecated  
//  void err(final String message);
//  
  
}