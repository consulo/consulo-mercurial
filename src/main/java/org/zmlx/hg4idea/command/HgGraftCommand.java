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

import consulo.application.AccessToken;
import consulo.project.Project;
import consulo.versionControlSystem.distributed.DvcsUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.execution.HgCommandExecutor;
import org.zmlx.hg4idea.execution.HgCommandResult;
import org.zmlx.hg4idea.repo.HgRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HgGraftCommand {

  @Nonnull
  private final Project myProject;
  @Nonnull
  private final HgRepository myRepository;

  public HgGraftCommand(@Nonnull Project project, @Nonnull HgRepository repo) {
    myProject = project;
    myRepository = repo;
  }

  @Nullable
  public HgCommandResult startGrafting(List<String> hashes) {
    return graft(hashes);
  }

  @Nullable
  public HgCommandResult continueGrafting() {
    return graft(Collections.singletonList("--continue"));
  }

  @Nullable
  private HgCommandResult graft(@Nonnull List<String> params) {
    List<String> args = new ArrayList<>();
    args.add("--log");
    args.addAll(params);
    AccessToken token = DvcsUtil.workingTreeChangeStarted(myProject);
    try {
      HgCommandResult result =
        new HgCommandExecutor(myProject)
          .executeInCurrentThread(myRepository.getRoot(), "graft", args);
      myRepository.update();
      return result;
    }
    finally {
      DvcsUtil.workingTreeChangeFinished(myProject, token);
    }
  }
}
