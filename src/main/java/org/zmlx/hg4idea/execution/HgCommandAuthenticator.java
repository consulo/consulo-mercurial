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
package org.zmlx.hg4idea.execution;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.credentialStorage.AuthenticationData;
import consulo.credentialStorage.PasswordSafe;
import consulo.credentialStorage.PasswordSafeException;
import consulo.credentialStorage.ui.AuthDialog;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ModalityState;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.HgGlobalSettings;
import org.zmlx.hg4idea.HgVcs;
import org.zmlx.hg4idea.HgVcsMessages;

/**
 * Base class for any command interacting with a remote repository and which needs authentication.
 */
class HgCommandAuthenticator {
  private static final Logger LOG = Logger.getInstance(HgCommandAuthenticator.class);

  private GetPasswordRunnable myGetPassword;
  private Project myProject;
  private final boolean myForceAuthorization;
  //todo replace silent mode and/or force authorization
  private final boolean mySilentMode;

  public HgCommandAuthenticator(Project project, boolean forceAuthorization, boolean silent) {
    myProject = project;
    myForceAuthorization = forceAuthorization;
    mySilentMode = silent;
  }

  public void saveCredentials() {
    if (myGetPassword == null) {
      return;    // prompt was not suggested;
    }

    // if checkbox is selected, save on disk. Otherwise in memory. Don't read password safe settings.

    final PasswordSafe passwordSafe = PasswordSafe.getInstance();
    final String url = VirtualFileManager.extractPath(myGetPassword.getURL());
    final String key = keyForUrlAndLogin(url, myGetPassword.getUserName());
    try {
      PasswordSafe.getInstance()
                  .storePassword(myProject,
                                 HgCommandAuthenticator.class,
                                 key,
                                 myGetPassword.getPassword(),
                                 myGetPassword.isRememberPassword());

      final HgVcs vcs = HgVcs.getInstance(myProject);
      if (vcs != null) {
        vcs.getGlobalSettings().addRememberedUrl(url, myGetPassword.getUserName());
      }
    }
    catch (PasswordSafeException e) {
      LOG.info("Couldn't store the password for key [" + key + "]", e);
    }
  }

  public boolean promptForAuthentication(Project project,
                                         @Nonnull String proposedLogin,
                                         @Nonnull String uri,
                                         @Nonnull String path,
                                         @Nullable ModalityState state) {
    GetPasswordRunnable runnable = new GetPasswordRunnable(project, proposedLogin, uri, path, myForceAuthorization, mySilentMode);
    ApplicationManager.getApplication().invokeAndWait(runnable, state == null ? Application.get().getDefaultModalityState() : state);
    myGetPassword = runnable;
    return runnable.isOk();
  }

  public String getUserName() {
    return myGetPassword.getUserName();
  }

  public String getPassword() {
    return myGetPassword.getPassword();
  }

  private static class GetPasswordRunnable implements Runnable {
    private static final Logger LOG = Logger.getInstance(GetPasswordRunnable.class);

    private String myUserName;
    private String myPassword;
    private Project myProject;
    private final String myProposedLogin;
    private boolean ok = false;
    @Nullable
    private String myURL;
    private boolean myRememberPassword;
    private boolean myForceAuthorization;
    private final boolean mySilent;

    public GetPasswordRunnable(Project project,
                               @Nonnull String proposedLogin,
                               @Nonnull String uri,
                               @Nonnull String path,
                               boolean forceAuthorization,
                               boolean silent) {
      this.myProject = project;
      this.myProposedLogin = proposedLogin;
      this.myURL = uri + path;
      this.myForceAuthorization = forceAuthorization;
      mySilent = silent;
    }

    @Override
    public void run() {

      // find if we've already been here
      final HgVcs vcs = HgVcs.getInstance(myProject);
      if (vcs == null) {
        return;
      }

      @Nonnull final HgGlobalSettings hgGlobalSettings = vcs.getGlobalSettings();
      @Nullable String rememberedLoginsForUrl = null;
      String url = VirtualFileManager.extractPath(myURL);
      if (!StringUtil.isEmptyOrSpaces(myURL)) {
        rememberedLoginsForUrl = hgGlobalSettings.getRememberedUserName(url);
      }

      String login = myProposedLogin;
      if (StringUtil.isEmptyOrSpaces(login)) {
        // find the last used login
        login = rememberedLoginsForUrl;
      }

      String password = null;
      if (!StringUtil.isEmptyOrSpaces(login) && myURL != null) {
        // if we've logged in with this login, search for password
        final String key = keyForUrlAndLogin(myURL, login);
        try {
          final PasswordSafe passwordSafe = PasswordSafe.getInstance();
          password = passwordSafe.getPassword(myProject, HgCommandAuthenticator.class, key);
        }
        catch (PasswordSafeException e) {
          LOG.info("Couldn't get password for key [" + key + "]", e);
        }
      }

      // don't show dialog if we don't have to (both fields are known) except force authorization required
      if (!myForceAuthorization && !StringUtil.isEmptyOrSpaces(password) && !StringUtil.isEmptyOrSpaces(login)) {
        myUserName = login;
        myPassword = password;
        ok = true;
        return;
      }

      if (mySilent) {
        ok = false;
        return;
      }

      AuthDialog authDialog = myProject.getInstance(AuthDialog.class);

      final AuthenticationData authenticationData =
        authDialog.show(HgVcsMessages.message("hg4idea.dialog.login.password.required"),
                        HgVcsMessages.message("hg4idea.dialog.login.description", myURL),
                        login,
                        password,
                        true);
      if (authenticationData != null) {
        myUserName = authenticationData.getLogin();
        myPassword = new String(authenticationData.getPassword());
        myRememberPassword = authenticationData.isRememberPassword();
        ok = true;
      }
    }

    public String getUserName() {
      return myUserName;
    }

    public String getPassword() {
      return myPassword;
    }

    public boolean isOk() {
      return ok;
    }

    @Nonnull
    public String getURL() {
      return myURL;
    }

    public boolean isRememberPassword() {
      return myRememberPassword;
    }
  }

  private static String keyForUrlAndLogin(String stringUrl, String login) {
    return login + ":" + stringUrl;
  }
}
