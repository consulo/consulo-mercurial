/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import jakarta.annotation.Nonnull;

import consulo.document.FileDocumentManager;
import consulo.project.Project;
import consulo.versionControlSystem.log.Hash;
import consulo.virtualFileSystem.VirtualFile;
import org.zmlx.hg4idea.command.HgUpdateCommand;
import org.zmlx.hg4idea.repo.HgRepository;

public class HgUpdateToFromLogAction extends HgLogSingleCommitAction {
  @Override
  protected void actionPerformed(@Nonnull final HgRepository repository, @Nonnull Hash commit) {
    final Project project = repository.getProject();
    final VirtualFile root = repository.getRoot();
    FileDocumentManager.getInstance().saveAllDocuments();
    HgUpdateCommand.updateRepoTo(project, root, commit.asString(), null);
  }
}
