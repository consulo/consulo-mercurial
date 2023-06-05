/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package org.zmlx.hg4idea.action.mq;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.language.editor.CommonDataKeys;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.function.Condition;
import consulo.versionControlSystem.log.Hash;
import consulo.versionControlSystem.log.VcsFullCommitDetails;
import consulo.versionControlSystem.log.VcsLog;
import consulo.versionControlSystem.log.VcsLogDataKeys;
import jakarta.annotation.Nonnull;
import org.zmlx.hg4idea.HgNameWithHashInfo;
import org.zmlx.hg4idea.HgVcsMessages;
import org.zmlx.hg4idea.command.mq.HgQGotoCommand;
import org.zmlx.hg4idea.command.mq.HgQPopCommand;
import org.zmlx.hg4idea.repo.HgRepository;

import java.util.List;
import java.util.function.Consumer;

public class HgQGotoFromLogAction extends HgMqAppliedPatchAction {

  protected void actionPerformed(@Nonnull final HgRepository repository, @Nonnull final VcsFullCommitDetails commit) {
    final Project project = repository.getProject();
    List<Hash> parents = commit.getParents();
    final Hash parentHash = parents.isEmpty() ? null : parents.get(0);

    final HgNameWithHashInfo parentPatchName = ContainerUtil.find(repository.getMQAppliedPatches(), new Condition<HgNameWithHashInfo>() {
      @Override
      public boolean value(HgNameWithHashInfo info) {
        return info.getHash().equals(parentHash);
      }
    });
    new Task.Backgroundable(repository.getProject(), parentPatchName != null
                                                     ? HgVcsMessages.message("hg4idea.mq.progress.goto", parentPatchName)
                                                     : HgVcsMessages.message("hg4idea.mq.progress.pop")) {

      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        if (parentPatchName != null) {
          new HgQGotoCommand(repository).executeInCurrentThread(parentPatchName.getName());
        }
        else {
          new HgQPopCommand(repository).executeInCurrentThread();
        }
      }

      @Override
      public void onSuccess() {
        HgShowUnAppliedPatchesAction.showUnAppliedPatches(project, repository);
      }
    }.queue();
  }

  @Override
  protected void actionPerformed(@Nonnull HgRepository repository, @Nonnull Hash commit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final Project project = e.getRequiredData(CommonDataKeys.PROJECT);
    VcsLog log = e.getRequiredData(VcsLogDataKeys.VCS_LOG);

    log.requestSelectedDetails(new Consumer<List<VcsFullCommitDetails>>() {
      @Override
      public void accept(List<VcsFullCommitDetails> selectedDetails) {
        VcsFullCommitDetails fullCommitDetails = ContainerUtil.getFirstItem(selectedDetails);

        assert fullCommitDetails != null;
        final HgRepository repository = getRepositoryForRoot(project, fullCommitDetails.getRoot());
        assert repository != null;

        actionPerformed(repository, fullCommitDetails);
      }
    }, null);
  }
}
