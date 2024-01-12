
package jetbrains.buildServer.core.runtime;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;

public class Platform {

  private static final String PROC_SPARCV9 = "sparcv9";

  private static final String PROC_SPARC = "sparc";

  private static final String PROC_IA64 = "ia64";

  private static final String PROC_X64 = "x64";

  private static final String PROC_X86 = "x86";

  private static final String OS_LINUX = "linux";

  private static final String OS_MAC = "mac";

  private static final String OS_SOLARIS = "solaris";

  private static final String OS_WIN = "win";

  //TODO: parse remote platform in connectors
  public static Platform WIN = getPlatform(OS_WIN);

  public static Platform SOLARIS = getPlatform(OS_SOLARIS);

  public static Platform MAC = getPlatform(OS_MAC);

  public static Platform LINUX = getPlatform(OS_LINUX);

  public static Platform UNKNOWN = getPlatform("unknown");

  private final String myOs;

  private final String myArch;

  private Platform(final String platform, final String arch) {
    myOs = platform;
    myArch = arch;
  }

  public static Platform getPlatform(String os) {
    return getPlatform(os, null);
  }

  public static Platform getPlatform(final String osname, final String osarch) {
    final String platform;
    if (osname.contains(OS_WIN)) {
      platform = OS_WIN;
    } else if (osname.contains("sun") || osname.contains(OS_SOLARIS)) {
      platform = OS_SOLARIS;
    } else if (osname.contains(OS_MAC)) {
      platform = OS_MAC;
    } else if (osname.contains(OS_LINUX)) {
      platform = OS_LINUX;
    } else {
      platform = osname;
    }
    String arch = null;
    if(osarch == null){
      //no op
    } else if (osarch.contains(PROC_X86)) {
      arch = PROC_X86;
    } else if (osarch.contains(PROC_X64)) {
      arch = PROC_X64;
    } else if (osarch.contains(PROC_IA64)) {
      arch = PROC_IA64;
    } else if (osarch.contains(PROC_SPARC)) {
      arch = PROC_SPARC;
    } else {
      arch = PROC_SPARCV9;
    }
    return new Platform(platform, arch);
  }

  public static Platform getCurrentPlatform() {
    final String osname = String.valueOf(System.getProperties().get("os.name")).toLowerCase();
    final String osarch = String.valueOf(System.getProperties().get("os.arch")).toLowerCase();
    return getPlatform(osname, osarch);
  }

  public String getOs() {
    return myOs;
  }

  public String getArch() {
    return myArch;
  }

  public File[] makeExecutable(final File[] files, IProgressMonitor monitor) throws IOException {
    final LinkedList<File> executables = new LinkedList<File>();
    final String currentPlatform = getOs();
    if (OS_WIN.equals(currentPlatform)) {
      for (File file : files) {
        if (!file.getName().endsWith(".exe")) {
          final File executable = new File(file.getParentFile(), String.format("%s.exe", file.getName()));
          file.renameTo(executable);
          executables.add(executable);
          monitor.status(new ProgressStatus(ProgressStatus.OK, String.format("make execurable: '%s' renamed to '%s'", file, executable)));
        } else {
          //left as it
          executables.add(file);
        }
      }
    } else if (OS_SOLARIS.equals(currentPlatform) || OS_LINUX.equals(currentPlatform)) {
      for (File file : files) {
        final String chmodCommand = String.format("chmod +x %s", file.getAbsolutePath());
        RuntimeUtil.execAndWait(chmodCommand, new File("."), monitor);//just for logging          
        executables.add(file);
      }
    }
    return executables.toArray(new File[executables.size()]);
  }

  public void runExecutable(final File executable, final String arguments, IProgressMonitor monitor) throws IOException {
    RuntimeUtil.execAndWait(String.format("%s %s", executable, arguments), new File("."), monitor);
  }

  public void runScripts(final String scriptContent, final String[] arguments, IProgressMonitor monitor) throws IOException {
    final File script = File.createTempFile("tci-script", getPlatformScriptExtension());
    final StringBuilder args = new StringBuilder();
    final FileOutputStream out = new FileOutputStream(script);
    RuntimeUtil.copy(new ByteArrayInputStream(scriptContent.getBytes()), out);
    out.flush();
    out.close();
    monitor.status(new ProgressStatus(ProgressStatus.OK, String.format("Making executable '%s'", script)));
    final File executableScript = makeExecutable(new File[] { script }, monitor)[0];
    if (OS_WIN.equals(getOs())) {

    } else if (OS_SOLARIS.equals(getOs())) {
      RuntimeUtil.execAndWait("sh -version", new File("."), monitor);//check 'sh' exists        
      args.append(String.format("%s ", executableScript.getAbsolutePath()));
      for (String arg : arguments) {
        args.append(String.format("%s ", arg));
      }
      final String command = String.format("sh %s", args.toString().trim());
      monitor.status(new ProgressStatus(ProgressStatus.OK, String.format("Running script '%s'", script)));
      RuntimeUtil.execAndWait(command, new File("."), monitor);

    } else if (OS_LINUX.equals(getOs())) {
      RuntimeUtil.execAndWait("bash -version", new File("."), monitor);//check 'sh' exists        
      args.append(String.format("%s ", executableScript.getAbsolutePath()));
      for (String arg : arguments) {
        args.append(String.format("%s ", arg));
      }
      final String command = String.format("bash %s", args.toString().trim());
      monitor.status(new ProgressStatus(ProgressStatus.OK, String.format("Running script '%s'", script)));
      RuntimeUtil.execAndWait(command, new File("."), monitor);

    } else {
      //....
    }
    RuntimeUtil.delete(script);
  }

  private String getPlatformScriptExtension() {
    if (OS_WIN.equals(getOs())) {
      return ".cmd";
    }
    return ".sh";
  }

  @Override
  public String toString() {
    return String.format("%s: os='%s' arch='%s'", getClass().getSimpleName(), getOs(), getArch());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Platform)) return false;

    final Platform platform = (Platform)obj;

    if (myOs != null ? !myOs.equals(platform.myOs) : platform.myOs != null) return false;
    if (myArch == null || platform.myArch == null) return true;
    if (myArch != null ? !myArch.equals(platform.myArch) : platform.myArch != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myOs != null ? myOs.hashCode() : 0;
    result = 31 * result + (myArch != null ? myArch.hashCode() : 0);
    return result;
  }
}