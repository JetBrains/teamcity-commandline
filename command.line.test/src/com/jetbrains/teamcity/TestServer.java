package com.jetbrains.teamcity;

import jetbrains.buildServer.BuildTypeData;
import jetbrains.buildServer.BuildTypeDescriptor.CheckoutType;
import jetbrains.buildServer.BuildTypeStatusDescriptorData;
import jetbrains.buildServer.ProjectData;
import jetbrains.buildServer.StatusDescriptor;
import jetbrains.buildServer.messages.Status;
import org.junit.Test;

import java.util.*;

public class TestServer extends Server {

  public static final String TC_CONFIG_MULTIPLE_ROOT = "tc-config-multiple-root";
  public static final String TC_CONFIG_SINGLE_ROOT = "tc-config-single-root";
  public static final String TC_CONFIG_NO_ROOTS = "tc-config-no-one-vcs-roots";

  public static final String TC_PRJ_0 = "tc-prj-0";

  private HashMap<String, ProjectData> myProjects = new HashMap<String, ProjectData>();
  private HashMap<String, BuildTypeData> myConfigurations = new HashMap<String, BuildTypeData>();

  public TestServer() {
    super(null);
    // fill projects
    final TestProjectData prj0 = new TestProjectData(TC_PRJ_0);
    myProjects.put(prj0.getProjectId(), prj0);
    // fill configurations
    final TestBuildTypeData cfg0 = new TestBuildTypeData(TC_CONFIG_NO_ROOTS, prj0);
    myConfigurations.put(cfg0.getId(), cfg0);
    final TestBuildTypeData cfg1 = new TestBuildTypeData(TC_CONFIG_SINGLE_ROOT, prj0);
    myConfigurations.put(cfg1.getId(), cfg1);
    final TestBuildTypeData cfg2 = new TestBuildTypeData(TC_CONFIG_MULTIPLE_ROOT, prj0);
    myConfigurations.put(cfg2.getId(), cfg2);
  }

  public static class TestProjectData extends ProjectData {
    public TestProjectData(String id) {
      super(id, id, "<unknown>", Status.UNKNOWN, new ArrayList<BuildTypeData>());
    }

  }

  public static class TestBuildTypeData extends BuildTypeData {

    public TestBuildTypeData(String id, ProjectData project) {
      super(false, id, null, project.getName(), project.getProjectId(), Collections.singletonList("<unknown>"), id, "<unknown>", CheckoutType.MANUAL, createStatusDescriptor(), null, false);
      project.getBuildTypes().add(this);
    }

    private static BuildTypeStatusDescriptorData createStatusDescriptor() {
      return new BuildTypeStatusDescriptorData(new StatusDescriptor(Status.UNKNOWN, ""), null);
    }
  }

  @Override
  public void connect() throws ECommunicationException {
    // do nothing
  }

  @Override
  public synchronized Collection<BuildTypeData> getConfigurations() throws ECommunicationException {
    return myConfigurations.values();
  }

  @Override
  public int getCurrentUser() {
    return -1;
  }

  @Override
  public synchronized Collection<ProjectData> getProjects() throws ECommunicationException {
    return myProjects.values();
  }

  @Override
  public String getURL() {
    return "http://localhost";
  }

  @Override
  public void logon(String username, String password) throws ECommunicationException, EAuthorizationException {
    // do nothing
  }

  @Test
  public void test_nothing() {
    // JUnit4 hack
  }

}
