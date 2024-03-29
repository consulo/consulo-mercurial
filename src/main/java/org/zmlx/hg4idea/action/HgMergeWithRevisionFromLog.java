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
package org.zmlx.hg4idea.action;

import consulo.document.FileDocumentManager;
import consulo.versionControlSystem.update.UpdatedFiles;
import consulo.versionControlSystem.log.Hash;
import jakarta.annotation.Nonnull;

import org.zmlx.hg4idea.command.HgMergeCommand;
import org.zmlx.hg4idea.repo.HgRepository;

public class HgMergeWithRevisionFromLog extends HgLogSingleCommitAction {

  @Override
  protected void actionPerformed(@Nonnull HgRepository repository, @Nonnull Hash commit) {
    FileDocumentManager.getInstance().saveAllDocuments();
    HgMergeCommand.mergeWith(repository, commit.asString(), UpdatedFiles.create());
  }
}
