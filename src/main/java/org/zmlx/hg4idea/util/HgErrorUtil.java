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
package org.zmlx.hg4idea.util;

import consulo.ide.setting.ShowSettingsUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.function.Condition;
import consulo.versionControlSystem.VcsBundle;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import org.zmlx.hg4idea.action.HgCommandResultNotifier;
import org.zmlx.hg4idea.execution.HgCommandResult;

import jakarta.annotation.Nullable;
import javax.swing.event.HyperlinkEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HgErrorUtil {

  private static final Logger LOG = Logger.getInstance(HgErrorUtil.class.getName());

  private static final String SETTINGS_LINK = "settings";
  public static final String MAPPING_ERROR_MESSAGE =
    "Please, ensure that your project base dir is hg root directory or specify full repository path in  <a href='" +
    SETTINGS_LINK + "'>directory mappings panel</a>.";
  private static final String MERGE_WITH_ANCESTOR_ERROR = "merging with a working directory ancestor has no effect";
  private static final String NOTHING_TO_REBASE_WARNING = "nothing to rebase";

  private HgErrorUtil() {
  }

  public static boolean isAbort(@Nullable HgCommandResult result) {
    return result == null || getAbortLine(result) != null;
  }

  @Nullable
  private static String getAbortLine(@Nonnull HgCommandResult result) {
    final List<String> errorLines = result.getErrorLines();
    return ContainerUtil.find(errorLines, new Condition<String>() {
      @Override
      public boolean value(String s) {
        return isAbortLine(s);
      }
    });
  }

  public static boolean isAncestorMergeError(@Nullable HgCommandResult result) {
    if (result == null) return false;
    String errorLine = getAbortLine(result);
    return errorLine != null && StringUtil.contains(errorLine, MERGE_WITH_ANCESTOR_ERROR);
  }

  public static boolean isNothingToRebase(@Nullable HgCommandResult result) {
    if (result == null) return false;
    return ContainerUtil.exists(result.getOutputLines(), new Condition<String>() {
      @Override
      public boolean value(String s) {
        return StringUtil.contains(s, NOTHING_TO_REBASE_WARNING);
      }
    });
  }

  public static boolean isAuthorizationError(@Nullable HgCommandResult result) {
    if (result == null) {
      return false;
    }
    String line = getLastErrorLine(result);
    return isAuthorizationError(line);
  }

  @Nullable
  private static String getLastErrorLine(@Nullable HgCommandResult result) {
    if (result == null) {
      return null;
    }
    final List<String> errorLines = result.getErrorLines();
    if (errorLines.isEmpty()) {
      return null;
    }
    return errorLines.get(errorLines.size() - 1);
  }

  //unresolved conflict errors included
  public static boolean hasErrorsInCommandExecution(@Nullable HgCommandResult result) {
    return isAbort(result) || result.getExitValue() != 0;
  }

  //todo should be modified and/or merged with HgErrorHandler
  public static boolean isCommandExecutionFailed(@Nullable HgCommandResult result) {
    return isAbort(result) || result.getExitValue() > 1;
  }

  public static boolean hasAuthorizationInDestinationPath(@Nullable String destinationPath) {
    if (StringUtil.isEmptyOrSpaces(destinationPath)) {
      return false;
    }
    return HgUtil.URL_WITH_PASSWORD.matcher(destinationPath).matches();
  }

  @Nonnull
  public static NotificationListener getMappingErrorNotificationListener(@Nonnull final Project project) {
    return new NotificationListener.Adapter() {
      @Override
      protected void hyperlinkActivated(@Nonnull Notification notification,
                                        @Nonnull HyperlinkEvent e) {
        if (SETTINGS_LINK.equals(e.getDescription())) {
          ShowSettingsUtil.getInstance()
            .showSettingsDialog(project, VcsBundle.message("version.control.main.configurable.name"));
        }
      }
    };
  }

  public static boolean isUnknownEncodingError(@Nullable HgCommandResult result) {
    if (result == null) {
      return false;
    }
    List<String> errorLines = result.getErrorLines();
    if (errorLines.isEmpty()) {
      return false;
    }
    String line = errorLines.get(0);
    return !StringUtil.isEmptyOrSpaces(line) && (line.contains("abort") && line.contains("unknown encoding"));
  }

  //during update  or revert action with  uncommitted merges/changes
  public static boolean hasUncommittedChangesConflict(@Nullable HgCommandResult result) {
    if (result == null) {
      return false;
    }
    // error messages from mercurial after update command failed: "abort: outstanding uncommitted merges", "abort: uncommitted changes";
    //after revert command failed: abort: "uncommitted merge"
    final Pattern UNCOMMITTED_PATTERN = Pattern.compile(".*abort.*uncommitted\\s*(change|merge).*", Pattern.DOTALL);
    Matcher matcher = UNCOMMITTED_PATTERN.matcher(result.getRawError());
    return matcher.matches();
  }

  public static boolean isAuthorizationError(String line) {
    return !StringUtil.isEmptyOrSpaces(line) && (line.contains("authorization required") || line.contains("authorization failed"));
  }

  public static boolean isAbortLine(String line) {
    return !StringUtil.isEmptyOrSpaces(line) && line.trim().startsWith("abort:");
  }

  public static void handleException(@Nullable Project project, @Nonnull Exception e) {
    handleException(project, "Error", e);
  }

  public static void handleException(@Nullable Project project, @Nonnull String title, @Nonnull Exception e) {
    LOG.info(e);
    new HgCommandResultNotifier(project).notifyError(null, title, e.getMessage());
  }

  public static void markDirtyAndHandleErrors(Project project, VirtualFile repository) {
    try {
      HgUtil.markDirectoryDirty(project, repository);
    }
    catch (InvocationTargetException | InterruptedException e) {
      handleException(project, e);
    }
  }
}
