/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.zmlx.hg4idea.execution;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Nadya Zabrodina
 */
public class HgDeleteModifyPromptHandler implements HgPromptHandler {
  private static final Pattern LOCAL_DELETE_REMOTE_MODIFIED_CONFLICT_MESSAGE_PATTERN = Pattern.compile(
    "remote\\schanged(.+)which\\slocal\\sdeleted\\s.+");
  private static final Pattern REMOTE_DELETE_LOCAL_MODIFIED_CONFLICT_MESSAGE_PATTERN = Pattern.compile(
    "\\slocal\\schanged(.+)which\\sremote\\sdeleted\\s.+");

  private final Project myProject;

  public HgDeleteModifyPromptHandler(@Nonnull Project project) {
    myProject = project;
  }

  public HgPromptChoice promptUser(@Nonnull final String message,
                                   @Nonnull final HgPromptChoice[] choices,
                                   @Nonnull final HgPromptChoice defaultChoice) {

    Matcher localDelMatcher = LOCAL_DELETE_REMOTE_MODIFIED_CONFLICT_MESSAGE_PATTERN.matcher(message);
    Matcher localModifyMatcher = REMOTE_DELETE_LOCAL_MODIFIED_CONFLICT_MESSAGE_PATTERN.matcher(message);
    String filename;
    final String modifiedMessage;
    if (localDelMatcher.matches()) {
      filename = localDelMatcher.group(1);
      modifiedMessage =
        "File " + filename + " is deleted locally, but modified remotely. Do you want to keep the modified version or remove the file?";
    }
    else if (localModifyMatcher.matches()) {
      filename = localModifyMatcher.group(1);
      modifiedMessage =
        "File " + filename + " is deleted remotely, but modified locally. Do you want to keep the modified version or remove the file?";
    }
    else {
      modifiedMessage = message;
    }
    return HgPromptChoice.ask(myProject, LocalizeValue.localizeTODO("Delete-Modify Conflict"), modifiedMessage, choices, defaultChoice);
  }

  public boolean shouldHandle(@Nullable String message) {
    if (message == null) {
      return false;
    }
    Matcher localDelMatcher = LOCAL_DELETE_REMOTE_MODIFIED_CONFLICT_MESSAGE_PATTERN.matcher(message);
    Matcher localModifyMatcher = REMOTE_DELETE_LOCAL_MODIFIED_CONFLICT_MESSAGE_PATTERN.matcher(message);
    if (localDelMatcher.matches() || localModifyMatcher.matches()) {
      return true;
    }
    return false;
  }
}
