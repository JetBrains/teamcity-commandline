[![official JetBrains project](https://jb.gg/badges/official-flat-square.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)


> This plugin is **no longer supported**. Use REST API to [upload patches directly](https://www.jetbrains.com/help/teamcity/personal-build.html#Direct+Patch+Upload) instead.
> 
{type="note"}

TeamCity command line remote run tool allows you to start Personal Builds from the command line.

TeamCity Command Line Remote Run tool allows running Personal Builds from the command line 
(Windows, Linux, Mac are supported, provided a JRE is installed).

The tool has no integration with a version control. It starts a personal build for the supplied list of files, 
and uses a special mapping file to match local files on the developer machine with build agent checkout directory.

The server supports running personal builds for the sources stored under Subversion, CVS, Perforce and Git.

See [plugin page in marketplace](https://plugins.jetbrains.com/plugin/9101-command-line-remote-run-tool)

## Usage

First, make sure  Java JRE 1.8.+ is installed on your machine.

### Basics

After [installing the plugin](https://www.jetbrains.com/help/teamcity/installing-additional-plugins.html), 
navigate to  **My Profile | TeamCity Tools side panel | Command Line Remote Run** 
and click *download* to save the `tcc.jar` file to your file system.
        
The tool is run with the `java -jar <path to tcc.jar> <command> <options>` command.

The list of available commands is available upon `java -jar <path to tcc.jar> help`

To see description for a specific command, run `java -jar <path to tcc.jar> help <command>`
               
To work correctly, the tool requires a mapping from local directory paths to VCS settings in TeamCity. 
This mapping should be stored to `.teamcity-mappings.properties` configuration file. See below on how to create this file.

### Login to TeamCity:

    java -jar <path to tcc.jar> login --host <server URL> --user <username>

You will be prompted to enter the password. The "login" command persists credentials for the target host to avoid 
authorization options for subsequent starts. The last logged in `<server URL>` is treated as the default one and 
will be used if the `--host` key is omitted. You will need to specify the server URL for the `run` command only.

### Run a remote build

To run a remote build, the tool also requires:

- The [external ID](https://www.jetbrains.com/help/teamcity/identifier.html) of the target build configuration on the server. This `buildTypeID` is displayed in the drop-down list 
  of available build configurations on the configuration web UI page of the Command Line Remote Run tool, or it can be taken from the URL 
  on the build configuration page.
- A comment to the personal build.
- The path to the changed files to be included in a personal build. You can provide the path either to the **directory** with modified files or 
  to a **file with the list of changed files** (such file can be prepared with help of the VCS tools).
- `.teamcity-mapping.properties` file (see below)

Collect the required information and run a remote build on the specified list of files for the selected build configuration:

    java -jar <path to tcc.jar> run --host <server URL> -m "<build comment>" -c <buildTypeID> <list of modified files>

By default, the "run" command will wait for the build to finish and returns the code corresponding to the status of the build. 
This exit code can be used to perform a commit. See `java -jar <path to tcc.jar> help run` for more options. 

### Creating `.teamcity-mapping.properties` file

The plugin provides a web UI configuration page which helps to create the `.teamcity-mapping.properties` file.

1. Navigate to  **My Profile | TeamCity Tools side panel | Command Line Remote Run** and click *configure*.
1. Select a build configuration for your personal build from the dropdown with and click **Add**
1. The mappings contain pairs of paths to local directories with changed files and remote VCS Root's prefixes. 
   A local path can be absolute or relative to the location of the mapping file folder. Check the mappings table and edit it if needed.

When done, copy the resulting content to `.teamcity-mappings.properties` file to any of the following directories:

- root directory of the project sources to be sent to remote run (recommended)
- `$HOME/.TeamCity-CommandLine` default directory

