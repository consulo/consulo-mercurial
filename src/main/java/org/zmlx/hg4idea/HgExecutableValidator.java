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

import consulo.execution.ExecutableValidator;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import org.zmlx.hg4idea.util.HgUtil;

public class HgExecutableValidator extends ExecutableValidator {

  private final HgVcs myVcs;

  public HgExecutableValidator(@Nonnull Project project, @Nonnull HgVcs vcs) {
    super(project,
          HgVcsMessages.message("hg4idea.executable.notification.title"),
          HgVcsMessages.message("hg4idea.executable.notification.description"));
    myVcs = vcs;
  }

  @Override
  protected String getCurrentExecutable() {
    return myVcs.getGlobalSettings().getHgExecutable();
  }

  @Override
  public boolean isExecutableValid(@Nonnull String executable) {
    return HgUtil.isExecutableValid(executable);
  }

  @Override
  protected void showSettings() {
    ShowSettingsUtil.getInstance().showAndSelect(myProject, HgProjectConfigurable.class);
  }
}
