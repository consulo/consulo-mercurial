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
package org.zmlx.hg4idea.branch;

import consulo.document.FileDocumentManager;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.versionControlSystem.distributed.action.BranchActionGroup;
import consulo.versionControlSystem.update.UpdatedFiles;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.command.HgMergeCommand;
import org.zmlx.hg4idea.command.HgUpdateCommand;
import org.zmlx.hg4idea.repo.HgRepository;

import java.util.List;

public class HgCommonBranchActions extends BranchActionGroup {

  @Nonnull
  protected final Project myProject;
  @Nonnull
  protected String myBranchName;
  @Nonnull
  List<HgRepository> myRepositories;

  HgCommonBranchActions(@Nonnull Project project, @Nonnull List<HgRepository> repositories, @Nonnull String branchName) {
    myProject = project;
    myBranchName = branchName;
    myRepositories = repositories;
    getTemplatePresentation().setText(myBranchName, false); // no mnemonics
  }

  @Nonnull
  @Override
  public AnAction[] getChildren(@Nullable AnActionEvent e) {
    return new AnAction[]{
      new UpdateAction(myProject, myRepositories, myBranchName),
      new MergeAction(myProject, myRepositories, myBranchName)
    };
  }

  private static class MergeAction extends HgBranchAbstractAction {

    public MergeAction(@Nonnull Project project,
                       @Nonnull List<HgRepository> repositories,
                       @Nonnull String branchName) {
      super(project, "Merge", repositories, branchName);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      FileDocumentManager.getInstance().saveAllDocuments();
      final UpdatedFiles updatedFiles = UpdatedFiles.create();
      for (final HgRepository repository : myRepositories) {
        HgMergeCommand.mergeWith(repository, myBranchName, updatedFiles);
      }
    }
  }

  private static class UpdateAction extends HgBranchAbstractAction {

    public UpdateAction(@Nonnull Project project,
                        @Nonnull List<HgRepository> repositories,
                        @Nonnull String branchName) {
      super(project, "Update", repositories, branchName);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      HgUpdateCommand.updateTo(myBranchName, myRepositories, null);
    }
  }
}