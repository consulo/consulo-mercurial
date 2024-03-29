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

import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.util.lang.function.Condition;
import consulo.util.collection.ContainerUtil;
import consulo.versionControlSystem.log.Hash;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.HgNameWithHashInfo;
import org.zmlx.hg4idea.action.HgCommandResultNotifier;
import org.zmlx.hg4idea.execution.HgCommandExecutor;
import org.zmlx.hg4idea.execution.HgCommandResult;
import org.zmlx.hg4idea.execution.HgCommandResultHandler;
import org.zmlx.hg4idea.repo.HgRepository;
import org.zmlx.hg4idea.util.HgErrorUtil;
import org.zmlx.hg4idea.util.HgPatchReferenceValidator;

import java.util.Arrays;

public class HgQRenameCommand {

  private static final Logger LOG = Logger.getInstance(HgQRenameCommand.class);
  @Nonnull
  private final HgRepository myRepository;

  public HgQRenameCommand(@Nonnull HgRepository repository) {
    myRepository = repository;
  }

  public void execute(@Nonnull final Hash patchHash) {
    final Project project = myRepository.getProject();
    HgNameWithHashInfo patchInfo = ContainerUtil.find(myRepository.getMQAppliedPatches(), new Condition<HgNameWithHashInfo>() {
      @Override
      public boolean value(HgNameWithHashInfo info) {
        return info.getHash().equals(patchHash);
      }
    });
    if (patchInfo == null) {
      LOG.error("Could not find patch " + patchHash.toString());
      return;
    }
    final String oldName = patchInfo.getName();
    final String newName = Messages.showInputDialog(project, String.format("Enter a new name for %s patch:", oldName),
                                                    "Rename Patch", Messages.getQuestionIcon(), "", new HgPatchReferenceValidator(
        myRepository));
    if (newName != null) {
      performPatchRename(myRepository, oldName, newName);
    }
  }

  public static void performPatchRename(@Nonnull final HgRepository repository,
                                        @Nonnull final String oldName,
                                        @Nonnull final String newName) {
    if (oldName.equals(newName)) return;
    final Project project = repository.getProject();
    new HgCommandExecutor(project)
      .execute(repository.getRoot(), "qrename", Arrays.asList(oldName, newName), new HgCommandResultHandler() {
        @Override
        public void process(@Nullable HgCommandResult result) {
          if (HgErrorUtil.hasErrorsInCommandExecution(result)) {
            new HgCommandResultNotifier(project)
              .notifyError(result, "Qrename command failed", "Could not rename patch " + oldName + " to " + newName);
          }
          repository.update();
        }
      });
  }
}
