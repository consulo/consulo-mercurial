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

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nullable;

import jakarta.annotation.Nonnull;
import org.zmlx.hg4idea.HgVcsMessages;
import org.zmlx.hg4idea.provider.update.HgConflictResolver;
import org.zmlx.hg4idea.repo.HgRepository;
import org.zmlx.hg4idea.ui.HgRunConflictResolverDialog;
import org.zmlx.hg4idea.util.HgErrorUtil;

import java.util.Collection;

public class HgRunConflictResolverAction extends HgAbstractGlobalSingleRepoAction {

  @Override
  public void execute(@Nonnull final Project project, @Nonnull Collection<HgRepository> repositories, @Nullable HgRepository selectedRepo) {
    final HgRepository repository = repositories.size() > 1 ? letUserSelectRepository(project, repositories, selectedRepo) :
                                    ContainerUtil.getFirstItem(repositories);
    if (repository != null) {
      new Task.Backgroundable(project, HgVcsMessages.message("action.hg4idea.run.conflict.resolver.description")) {

        @Override
        public void run(@Nonnull ProgressIndicator indicator) {
          new HgConflictResolver(project).resolve(repository.getRoot());
          HgErrorUtil.markDirtyAndHandleErrors(project, repository.getRoot());
        }
      }.queue();
    }
  }

  @Nullable
  private static HgRepository letUserSelectRepository(@Nonnull Project project, @Nonnull Collection<HgRepository> repositories,
                                                      @Nullable HgRepository selectedRepo) {
    HgRunConflictResolverDialog dialog = new HgRunConflictResolverDialog(project, repositories, selectedRepo);
    return dialog.showAndGet() ? dialog.getRepository() : null;
  }
}
