// Copyright 2008-2010 Victor Iacoban
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distributed under
// the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
// either express or implied. See the License for the specific language governing permissions and
// limitations under the License.
package org.zmlx.hg4idea;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.util.SystemInfo;
import consulo.component.persist.*;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@State(
  name = "HgGlobalSettings",
  storages = @Storage(file = StoragePathMacros.APP_CONFIG + "/hg.xml", roamingType = RoamingType.PER_OS)
)
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@Singleton
public class HgGlobalSettings implements PersistentStateComponent<HgGlobalSettings.State> {
  @NonNls
  private static final String[] DEFAULT_WINDOWS_PATHS = {"C:\\Program Files\\Mercurial",
    "C:\\Program Files (x86)\\Mercurial",
    "C:\\cygwin\\bin"};
  @NonNls
  private static final String[] DEFAULT_UNIX_PATHS = {"/usr/local/bin",
    "/usr/bin",
    "/opt/local/bin",
    "/opt/bin",
    "/usr/local/mercurial"};
  @NonNls
  private static final String DEFAULT_WINDOWS_HG = "hg.exe";
  @NonNls
  private static final String DEFAULT_UNIX_HG = "hg";

  private static final int FIVE_MINUTES = 300;

  private State myState = new State();

  private String myDetectedHgExecutable;

  public static class State {
    public String myHgExecutable = null;
    // visited URL -> login for this URL. Passwords are remembered in the PasswordSafe.
    public Map<String, String> myRememberedUserNames = new HashMap<>();
  }

  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(State state) {
    myState = state;
  }

  /**
   * @return the default executable name depending on the platform
   */
  @Nonnull
  public String defaultHgExecutable() {
    String hgExecutable = myState.myHgExecutable;
    if (hgExecutable != null) {
      return hgExecutable;
    }

    if (myDetectedHgExecutable == null) {
      String[] paths;
      String programName;
      if (SystemInfo.isWindows) {
        programName = DEFAULT_WINDOWS_HG;
        paths = DEFAULT_WINDOWS_PATHS;
      }
      else {
        programName = DEFAULT_UNIX_HG;
        paths = DEFAULT_UNIX_PATHS;
      }

      for (String p : paths) {
        File f = new File(p, programName);
        if (f.exists()) {
          myDetectedHgExecutable = f.getAbsolutePath();
          break;
        }
      }
      if (myDetectedHgExecutable == null) {
        // otherwise, take the first variant and hope it's in $PATH
        myDetectedHgExecutable = programName;
      }
    }
    return myDetectedHgExecutable;
  }

  /**
   * Returns the remembered username for the specified URL which were accessed while working in the plugin.
   *
   * @param stringUrl the url for which to retrieve the last used username;
   * @return the (probably empty) login remembered for this URL.
   */
  @Nullable
  public String getRememberedUserName(@Nonnull String stringUrl) {
    return myState.myRememberedUserNames.get(stringUrl);
  }

  /**
   * Adds the information about visited URL.
   *
   * @param stringUrl String representation of the URL. If null or blank String is passed, nothing is saved.
   * @param username  Login used to access the URL. If null is passed, a blank String is used.
   */
  public void addRememberedUrl(@Nullable String stringUrl, @Nullable String username) {
    if (StringUtil.isEmptyOrSpaces(stringUrl)) {
      return;
    }
    if (username == null) {
      username = "";
    }
    myState.myRememberedUserNames.put(stringUrl, username);
  }

  @Nonnull
  public String getHgExecutable() {
    return myState.myHgExecutable == null ? defaultHgExecutable() : myState.myHgExecutable;
  }

  public void setHgExecutable(String hgExecutable) {
    myState.myHgExecutable = hgExecutable;
  }


  public static int getIncomingCheckIntervalSeconds() {
    return FIVE_MINUTES;
  }
}
