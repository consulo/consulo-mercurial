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
package org.zmlx.hg4idea.command;

import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.execution.HgCommandException;
import org.zmlx.hg4idea.execution.HgCommandExecutor;
import org.zmlx.hg4idea.execution.HgCommandResult;

import java.util.Collections;

/**
 * @author Nadya Zabrodina
 */
public class HgBranchCreateCommand {

  private final Project project;
  private final VirtualFile repo;
  private final String branchName;

  public HgBranchCreateCommand(@Nonnull Project project, @Nonnull VirtualFile repo, @Nullable String branchName) {
    this.project = project;
    this.repo = repo;
    this.branchName = branchName;
  }

  public HgCommandResult executeInCurrentThread() throws HgCommandException {
    if (StringUtil.isEmptyOrSpaces(branchName)) {
      throw new HgCommandException("branch name is empty");
    }
    return new HgCommandExecutor(project).executeInCurrentThread(repo, "branch", Collections.singletonList(branchName));
  }
}
