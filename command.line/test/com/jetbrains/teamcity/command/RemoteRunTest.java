package com.jetbrains.teamcity.command;

import com.jetbrains.teamcity.TestServer;
import com.jetbrains.teamcity.TestingUtil;
import com.jetbrains.teamcity.resources.ITCResource;
import com.jetbrains.teamcity.resources.ITCResourceMatcher;
import com.jetbrains.teamcity.resources.TCWorkspace;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.*;
import jetbrains.buildServer.core.runtime.RuntimeUtil;
import jetbrains.buildServer.util.FileUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class RemoteRunTest {

  private static RemoteRun ourCommand;
  private static File ourRootFolder;
  private static String ourCurrentDirectory;
  private static TestServer ourTestServer;

  @BeforeClass
  public static void setup() throws Exception {
    // dirs
    ourRootFolder = TestingUtil.createFS();

    // keep current directory
    ourCurrentDirectory = System.getProperty("user.dir");
    // change current directory
    System.setProperty("user.dir", ourRootFolder.getCanonicalFile().getAbsolutePath());

    ourCommand = new RemoteRun();

    ourTestServer = new TestServer();
  }

  @AfterClass
  public static void clean() throws Exception {
    System.setProperty("user.dir", ourCurrentDirectory);
    TestingUtil.releaseFS(ourRootFolder);
  }

  @Test
  public void getApplicableConfigurations() {
    // TODO: implement it
  }

  @Test
  public void createPatch() {
    // TODO: implement it
  }

  @Test
  public void createChangeList() {
    // TODO: implement it
  }

  @Test
  public void TW_9694() throws Exception {
    final File configFile = new File(ourRootFolder + File.separator + "java" + File.separator + "resources", TCWorkspace.TCC_ADMIN_FILE);
    FileUtil.writeFileAndReportErrors(configFile, ".=//depo/test/resources\n");
    final File patchFile = new File("./test.patch");
    patchFile.createNewFile();

    try {
      final File controlledFile = new File("java" + File.separator + "resources", "java.resources");
      {// file under TC
        final Collection<ITCResource> resources = ourCommand.getTCResources(new TCWorkspace(), Collections.singletonList(controlledFile), RuntimeUtil.NULL_MONITOR);
        System.out.println(String.format("TW_9694(): Collected resources: %s", resources));
        File patch = ourCommand.createPatch(resources, RuntimeUtil.NULL_MONITOR);
        System.out.println(String.format("TW_9694(): Patch size = %s", patch.length()));
        assertTrue(patch.length() > 10);
      }

      {// file is not under TC: disable default config
        File uncontrolledFile = new File("java", "1.java");
        final Collection<ITCResource> resources = ourCommand.getTCResources(new TCWorkspace() {
          @Override
          protected File getGlobalAdminFile() {
            return null;
          }
        }, Arrays.asList(controlledFile, uncontrolledFile), RuntimeUtil.NULL_MONITOR);
        System.out.println(String.format("TW_9694(): Collected resources: %s", resources));
        File patch = ourCommand.createPatch(resources, RuntimeUtil.NULL_MONITOR);
        System.out.println(String.format("TW_9694(): Patch size = %s", patch.length()));
        assertTrue(patch.length() > 10);
      }
    } finally {
      FileUtil.delete(patchFile);
      FileUtil.delete(configFile);
    }
  }

  @Test
  public void fireRemoteRun() {
    // TODO: implement it
  }

  @Test
  public void waitForSuccessResult() {
    // TODO: implement it
  }

  @Test
  public void getFiles_file_list_passed() {
    // TODO: implement it
  }

  @Test
  public void getFiles_file_target_passed() {
    // TODO: implement it
  }

  @Test
  public void getFiles_nothing_passed() {
    final Collection<File> files = ourCommand.getFiles(new Args(new String[] { RemoteRun.ID, RemoteRun.CONFIGURATION_PARAM_LONG, "bt2" }), RuntimeUtil.NULL_MONITOR);
    assertNotNull("null file's collection got", files);
    assertEquals("wrong files count collected", 5, files.size());
  }

  @Test
  public void getFiles_nowait_passed() {
    final Collection<File> files = ourCommand.getFiles(new Args(new String[] { RemoteRun.ID, RemoteRun.NO_WAIT_SWITCH_LONG, RemoteRun.CONFIGURATION_PARAM_LONG, "bt2" }), RuntimeUtil.NULL_MONITOR);
    assertNotNull("null file's collection got", files);
    assertEquals("wrong files count collected", 5, files.size());
  }

  @Test
  public void getFiles_check_changes_passed() {
    final Collection<File> files = ourCommand.getFiles(new Args(new String[] { RemoteRun.ID, RemoteRun.CHECK_FOR_CHANGES_EARLY_SWITCH, RemoteRun.CONFIGURATION_PARAM_LONG, "bt2" }), RuntimeUtil.NULL_MONITOR);
    assertNotNull("null file's collection got", files);
    assertEquals("wrong files count collected", 5, files.size());
  }

  @Test
  public void getFiles_nowait_and_check_changes_passed() {
    final Collection<File> files = ourCommand.getFiles(new Args(new String[] { RemoteRun.ID, RemoteRun.NO_WAIT_SWITCH_LONG, RemoteRun.CHECK_FOR_CHANGES_EARLY_SWITCH, RemoteRun.CONFIGURATION_PARAM_LONG, "bt2" }), RuntimeUtil.NULL_MONITOR);
    assertNotNull("null file's collection got", files);
    assertEquals("wrong files count collected", 5, files.size());
  }

  @Test
  public void readFromStream() {
    final String stdin = "a.java\n\rb.cpp";
    final String out = ourCommand.readFromStream(new ByteArrayInputStream(stdin.getBytes()));
    assertEquals("wrong stream content read", stdin, out);
  }

  @Test
  public void validate_error() {
    try {
      ourCommand.validate(null);
      assertTrue("Exception expected", true);
    } catch (IllegalArgumentException e) {
      // ok
    }
    try {
      ourCommand.validate(new Args(new String[] {}));
      assertTrue("Exception expected", true);
    } catch (IllegalArgumentException e) {
      // ok
    }

  }

  @Test
  public void validate_ok() {
    ourCommand.validate(new Args(new String[] { RemoteRun.ID, RemoteRun.MESSAGE_PARAM, "short" }));
    ourCommand.validate(new Args(new String[] { RemoteRun.ID, RemoteRun.MESSAGE_PARAM_LONG, "long" }));
    ourCommand.validate(new Args(new String[] { RemoteRun.ID, RemoteRun.MESSAGE_PARAM, "short", RemoteRun.MESSAGE_PARAM_LONG, "long" }));
  }

  @Test
  public void getGlobalConfigFile() throws Exception {

    // null on null
    ITCResourceMatcher config = ourCommand.getOverridingMatcher(new Args(new String[] { RemoteRun.ID }));
    assertNull(config);

    // error on virtual file
    try {
      config = ourCommand.getOverridingMatcher(new Args(new String[] { RemoteRun.ID, RemoteRun.OVERRIDING_MAPPING_FILE_PARAM, "global_config" }));
      assertTrue(false); // should not be here
    } catch (IllegalArgumentException e) {
      // OK: file is not exist
    }

    // wrong format
    File configFile = new File(ourCurrentDirectory, "global_config");
    configFile.createNewFile();
    try {
      config = ourCommand.getOverridingMatcher(new Args(new String[] { RemoteRun.ID, RemoteRun.OVERRIDING_MAPPING_FILE_PARAM, configFile.getAbsolutePath() }));
      assertTrue(false); // should not be here
    } catch (IllegalArgumentException e) {
      // OK: file format is wrong
    } finally {
      FileUtil.delete(configFile);
    }

    // all ok
    configFile = new File(ourCurrentDirectory, "global_config");
    try {
      FileUtil.writeFileAndReportErrors(configFile, ".=//depo/test/\n");
      config = ourCommand.getOverridingMatcher(new Args(new String[] { RemoteRun.ID, RemoteRun.OVERRIDING_MAPPING_FILE_PARAM, configFile.getAbsolutePath() }));
      assertNotNull(config);
    } finally {
      FileUtil.delete(configFile);
    }

  }

  @Test
  public void parseConfigurations() throws Exception {
    final Collection<String> nullConfig = RemoteRun.parseConfigurations(null);
    assertTrue(nullConfig.isEmpty());
    final Collection<String> emptyConfig = RemoteRun.parseConfigurations("  ");
    assertTrue(emptyConfig.isEmpty());
    final Collection<String> emptyConfigDevided = RemoteRun.parseConfigurations(" , , , ");
    assertTrue(emptyConfigDevided.isEmpty());
    final Collection<String> singleConfig = RemoteRun.parseConfigurations(" aaa ");
    assertEquals(1, singleConfig.size());
    assertEquals("aaa", singleConfig.iterator().next());
    {
      final Collection<String> doubleConfig = new TreeSet<String>(RemoteRun.parseConfigurations(" aaa , bbb "));
      assertEquals(2, doubleConfig.size());
      Iterator<String> iterator = doubleConfig.iterator();
      assertEquals("aaa", iterator.next());
      assertEquals("bbb", iterator.next());
    }
    {
      final Collection<String> doubleConfigBadFormatted = new TreeSet<String>(RemoteRun.parseConfigurations(" ,     ,aaa, \t,  , bbb,   , \t,    "));
      assertEquals(2, doubleConfigBadFormatted.size());
      Iterator<String> iterator = doubleConfigBadFormatted.iterator();
      assertEquals("aaa", iterator.next());
      assertEquals("bbb", iterator.next());
    }

  }

  @Test
  public void should_parse_build_params_bad_patterns() throws Exception {
    Map<String, String> map = RemoteRun.convertToMapAndUnescape(Arrays.<String>asList());
    assertTrue(map.isEmpty());

    final String[] bad_patterns = {
      "ddd", "-ddd", "=ddd", " =ddd", "=", " = "
    };

    for (String bad_pattern : bad_patterns) {
      map = RemoteRun.convertToMapAndUnescape(Arrays.asList(bad_pattern));
      assertTrue("Should be bad: '" + bad_pattern + "'", map.isEmpty());
    }

  }

  @Test
  public void should_parse_build_params_good_patterns() throws Exception {

    final String[] good_patterns = {
      "a=b", " a=b", " a =b"
    };

    for (String pattern : good_patterns) {
      final Map<String, String> map = RemoteRun.convertToMapAndUnescape(Arrays.asList(pattern));
      assertEquals(1, map.size());
      assertEquals("b", map.get("a"));
    }

    final Map<String, String> map2 = RemoteRun.convertToMapAndUnescape(Arrays.asList("a=b", " c = d= "));
    assertEquals(2, map2.size());
    assertEquals("b", map2.get("a"));
    assertEquals(" d= ", map2.get("c"));

  }

  @Test
  public void should_parse_build_params_escape_newline() throws Exception {

    // We allow to add newlines as |n and real | should be escaped as ||
    final Map<String, String> map = RemoteRun.convertToMapAndUnescape(Arrays.asList("a=b|nc", "c=||n"));
    assertEquals(2, map.size());
    assertEquals("b\nc", map.get("a"));
    assertEquals("|n", map.get("c"));

  }

}
