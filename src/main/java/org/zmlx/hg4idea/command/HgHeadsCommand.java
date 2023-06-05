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
package org.zmlx.hg4idea.command;

import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import org.zmlx.hg4idea.HgRevisionNumber;

import java.util.List;

public class HgHeadsCommand {

  private final Project project;
  private final VirtualFile repo;

  public HgHeadsCommand(Project project, @Nonnull VirtualFile repo) {
    this.project = project;
    this.repo = repo;
  }

  public List<HgRevisionNumber> executeInCurrentThread() {
    return executeInCurrentThread(".");
  }

  public List<HgRevisionNumber> executeInCurrentThread(String branch) {
    return new HeadsCommand(project, branch).executeInCurrentThread(repo);
  }

  private class HeadsCommand extends HgChangesetsCommand {
    private final String branch;
    public HeadsCommand(Project project, String branch) {
      super(project, "heads");
      this.branch = branch;
    }

    @Override
    protected void addArguments(List<String> args) {
      if (!StringUtil.isEmpty(branch)) {
        args.add(branch);
      }
    }
  }
}
