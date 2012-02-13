/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
