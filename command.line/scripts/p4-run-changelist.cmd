@echo off
REM
REM The script prepares changes from the pointed ChangeList for RemoteRun invocation by tcc.jar
REM Usage: <your_script.cmd> "change#"
REM


 if not "%1"=="" goto input_ok else goto usage 

:usage 
 @echo Usage: %0 "change#"
 goto end

:input_ok
 
 if not "%P4PORT%"=="" goto client
  @echo "The P4PORT environment variable is not defined"
  goto end

:client
 if not "%P4CLIENT%"=="" goto user
  @echo "The P4CLIENT environment variable is not defined"
  goto end

:user
 if not "%P4USER%"=="" goto password
  @echo "The P4USER environment variable is not defined"
  goto end

:password
 if not "%P4PASSWD%"=="" goto run
  @echo "The P4PASSWD environment variable is not defined"
  goto end

:run

 rem get comments
 p4 changes -s "pending" > p4-changes.tmp
 findstr /R "Change %1" p4-changes.tmp > p4-raw-changes.tmp
 for /F "eol=; tokens=7* delims= " %%i in (p4-raw-changes.tmp) do set CHANGEDESCR=%%j

 rem collect changelist's members
 p4 fstat -T "path" -P -Oc -e %1 -W //%P4CLIENT%/... > p4-fstat.tmp

 rem grep+awk
 findstr /R "path" ./p4-fstat.tmp > p4-raw-files.tmp

 if exist ./p4-change#%1-files.lst del p4-change#%1-files.lst

 for /F "eol=; tokens=3* delims= " %%i in (p4-raw-files.tmp) do @echo %%i >> p4-change#%1-files.lst

 if exist p4-changes.tmp del p4-changes.tmp
 if exist p4-raw-changes.tmp del p4-raw-changes.tmp
 if exist p4-fstat.tmp del p4-fstat.tmp
 if exist p4-raw-files.tmp del p4-raw-files.tmp

REM
REM put here your tcc.jar invocation for RR with the %CHANGEDESCR% as message and @p4-change#%1-files.lst file. See HOWTO for @file notation
REM

:end

 echo on