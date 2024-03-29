/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.zmlx.hg4idea.action;

import consulo.application.progress.Task;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.application.progress.ProgressIndicator;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.command.HgRebaseCommand;
import org.zmlx.hg4idea.execution.HgCommandResult;
import org.zmlx.hg4idea.repo.HgRepository;
import org.zmlx.hg4idea.util.HgErrorUtil;

import java.util.Collection;

public class HgContinueRebaseAction extends HgProcessStateAction {

  public HgContinueRebaseAction() {
    super(Repository.State.REBASING);
  }

  @Override
  protected void execute(@Nonnull final Project project,
                         @Nonnull Collection<HgRepository> repositories,
                         @Nullable final HgRepository selectedRepo) {

    new Task.Backgroundable(project, "Continue Rebasing...") {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        if (selectedRepo != null) {
          HgRebaseCommand rebaseCommand = new HgRebaseCommand(project, selectedRepo);
          HgCommandResult result = rebaseCommand.continueRebase();
          if (HgErrorUtil.isAbort(result)) {
            new HgCommandResultNotifier(project).notifyError(result, "Hg Error", "Couldn't continue rebasing");
          }
          HgErrorUtil.markDirtyAndHandleErrors(project, selectedRepo.getRoot());
        }
      }
    }.queue();
  }
}
