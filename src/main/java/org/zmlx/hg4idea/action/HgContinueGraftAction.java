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

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.project.Project;
import consulo.versionControlSystem.distributed.repository.Repository;

import jakarta.annotation.Nullable;

import jakarta.annotation.Nonnull;
import org.zmlx.hg4idea.command.HgGraftCommand;
import org.zmlx.hg4idea.execution.HgCommandResult;
import org.zmlx.hg4idea.repo.HgRepository;
import org.zmlx.hg4idea.util.HgErrorUtil;

import java.util.Collection;

public class HgContinueGraftAction extends HgProcessStateAction {

  public HgContinueGraftAction() {
    super(Repository.State.GRAFTING);
  }

  @Override
  protected void execute(@Nonnull final Project project,
                         @Nonnull Collection<HgRepository> repositories,
                         @Nullable final HgRepository selectedRepo) {

    new Task.Backgroundable(project, "Continue Grafting...") {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        if (selectedRepo != null) {
          HgGraftCommand graftCommand = new HgGraftCommand(project, selectedRepo);
          HgCommandResult result = graftCommand.continueGrafting();
          if (HgErrorUtil.isAbort(result)) {
            new HgCommandResultNotifier(project).notifyError(result, "Hg Error", "Couldn't continue grafting");
          }
          HgErrorUtil.markDirtyAndHandleErrors(project, selectedRepo.getRoot());
        }
      }
    }.queue();
  }
}
