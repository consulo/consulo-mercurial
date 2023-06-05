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
package org.zmlx.hg4idea.action;

import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.util.lang.StringUtil;
import consulo.util.lang.xml.XmlStringUtil;
import consulo.versionControlSystem.VcsNotifier;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.execution.HgCommandResult;

import java.util.List;

public final class HgCommandResultNotifier {

  private final Project myProject;
  private static final Logger LOG = Logger.getInstance(HgCommandResultNotifier.class);

  public HgCommandResultNotifier(Project project) {
    myProject = project;
  }

  public void notifyError(@Nullable HgCommandResult result, @Nonnull String failureTitle, @Nonnull String failureDescription) {
    notifyError(result, failureTitle, failureDescription, null);
  }

  public void notifyError(@Nullable HgCommandResult result,
                          @Nonnull String failureTitle,
                          @Nonnull String failureDescription,
                          @Nullable NotificationListener listener) {
    List<String> err;
    String errorMessage;
    if (StringUtil.isEmptyOrSpaces(failureDescription)) {
      failureDescription = failureTitle;
    }
    if (result == null) {
      errorMessage = failureDescription;
    } else {
      err = result.getErrorLines();
      if (err.isEmpty()) {
        LOG.assertTrue(!StringUtil.isEmptyOrSpaces(failureDescription),
                       "Failure title, failure description and errors log can not be empty at the same time");
        errorMessage = failureDescription;
      } else if (failureDescription.isEmpty()) {
        errorMessage = XmlStringUtil.wrapInHtml(StringUtil.join(err, "<br>"));
      } else {
        errorMessage = XmlStringUtil.wrapInHtml(failureDescription + "<br>" + StringUtil.join(err, "<br>"));
      }
    }
    VcsNotifier.getInstance(myProject).notifyError(failureTitle, errorMessage, listener);
  }
}
