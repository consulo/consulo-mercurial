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

import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import javax.annotation.Nonnull;
import org.zmlx.hg4idea.repo.HgRepository;

import java.util.List;

public abstract class HgBranchAbstractAction extends DumbAwareAction {
  @Nonnull
  protected final Project myProject;
  @Nonnull
  protected final List<HgRepository> myRepositories;
  @Nonnull
  protected final String myBranchName;

  public HgBranchAbstractAction(@Nonnull Project project, @Nonnull String title,
								@Nonnull List<HgRepository> repositories,
								@Nonnull String branchName) {
    super(title);
    myProject = project;
    myRepositories = repositories;
    myBranchName = branchName;
  }
}