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
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.util.VcsFileUtil;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.HgFile;
import org.zmlx.hg4idea.execution.HgCommandExecutor;
import org.zmlx.hg4idea.execution.HgCommandResult;
import org.zmlx.hg4idea.execution.HgCommandResultHandler;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

public class HgResolveCommand {

  private static final int ITEM_COUNT = 3;

  private final Project myProject;

  public HgResolveCommand(Project project) {
    myProject = project;
  }

  public Map<HgFile, HgResolveStatusEnum> getListSynchronously(VirtualFile repo) {
    if (repo == null) {
      return Collections.emptyMap();
    }
    final HgCommandExecutor executor = new HgCommandExecutor(myProject);
    executor.setSilent(true);
    final HgCommandResult result = executor.executeInCurrentThread(repo, "resolve", Collections.singletonList("--list"));
    if (result == null) {
      return Collections.emptyMap();
    }
    return handleResult(repo, result);
  }

  public void getListAsynchronously(final VirtualFile repo, final Consumer<Map<HgFile, HgResolveStatusEnum>> resultHandler) {
    if (repo == null) {
      resultHandler.accept(Collections.emptyMap());
    }
    final HgCommandExecutor executor = new HgCommandExecutor(myProject);
    executor.setSilent(true);
    executor.execute(repo, "resolve", Collections.singletonList("--list"), new HgCommandResultHandler() {
      @Override
      public void process(@Nullable HgCommandResult result) {
        if (result == null) {
          resultHandler.accept(Collections.emptyMap());
        }

        final Map<HgFile, HgResolveStatusEnum> resolveStatus = handleResult(repo, result);
        resultHandler.accept(resolveStatus);
      }
    });
  }

  private static Map<HgFile, HgResolveStatusEnum> handleResult(VirtualFile repo, HgCommandResult result) {
    final Map<HgFile, HgResolveStatusEnum> resolveStatus = new HashMap<>();
    for (String line : result.getOutputLines()) {
      if (StringUtil.isEmptyOrSpaces(line) || line.length() < ITEM_COUNT) {
        continue;
      }
      final HgResolveStatusEnum status = HgResolveStatusEnum.valueOf(line.charAt(0));
      if (status != null) {
        File ioFile = new File(repo.getPath(), line.substring(2));
        resolveStatus.put(new HgFile(repo, ioFile), status);
      }
    }
    return resolveStatus;
  }

  public void markResolved(@Nonnull VirtualFile repo, @Nonnull VirtualFile path) {
    markResolved(repo, Collections.singleton(VcsUtil.getFilePath(path)));
  }

  public void markResolved(@Nonnull VirtualFile repo, @Nonnull Collection<FilePath> paths) {
    for (List<String> chunk : VcsFileUtil.chunkPaths(repo, paths)) {
      final List<String> args = new ArrayList<>();
      args.add("--mark");
      args.addAll(chunk);
      new HgCommandExecutor(myProject).execute(repo, "resolve", args, null);
    }
  }

}