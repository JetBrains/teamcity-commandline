/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
