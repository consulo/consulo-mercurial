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

import javax.annotation.Nonnull;

import com.intellij.dvcs.repo.AbstractRepositoryManager;
import com.intellij.dvcs.ui.VcsLogSingleCommitAction;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import javax.annotation.Nullable;
import org.zmlx.hg4idea.repo.HgRepository;
import org.zmlx.hg4idea.repo.HgRepositoryManager;

public abstract class HgLogSingleCommitAction extends VcsLogSingleCommitAction<HgRepository> {

  @Nonnull
  @Override
  protected AbstractRepositoryManager<HgRepository> getRepositoryManager(@Nonnull Project project) {
    return ServiceManager.getService(project, HgRepositoryManager.class);
  }

  @Nullable
  @Override
  protected HgRepository getRepositoryForRoot(@Nonnull Project project, @Nonnull VirtualFile root) {
    return getRepositoryManager(project).getRepositoryForRoot(root);
  }

}
