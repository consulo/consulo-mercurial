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
package org.zmlx.hg4idea.command.mq;

import consulo.project.Project;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.action.HgCommandResultNotifier;
import org.zmlx.hg4idea.execution.HgCommandExecutor;
import org.zmlx.hg4idea.execution.HgCommandResult;
import org.zmlx.hg4idea.execution.HgCommandResultHandler;
import org.zmlx.hg4idea.repo.HgRepository;
import org.zmlx.hg4idea.util.HgErrorUtil;

import java.util.Collections;

public class HgQFinishCommand {
  @Nonnull
  private final HgRepository myRepository;

  public HgQFinishCommand(@Nonnull HgRepository repository) {
    myRepository = repository;
  }

  public void execute(@Nonnull final String revision) {
    final Project project = myRepository.getProject();
    new HgCommandExecutor(project)
      .execute(myRepository.getRoot(), "qfinish", Collections.singletonList("qbase:" + revision), new HgCommandResultHandler() {
        @Override
        public void process(@Nullable HgCommandResult result) {
          if (HgErrorUtil.hasErrorsInCommandExecution(result)) {
            new HgCommandResultNotifier(project)
              .notifyError(result, "QFinish command failed", "Could not apply patches into repository history.");
          }
          myRepository.update();
        }
      });
  }
}
