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
package org.zmlx.hg4idea.provider;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.document.FileDocumentManager;
import consulo.localize.LocalizeValue;
import consulo.mercurial.localize.HgLocalize;
import consulo.project.Project;
import consulo.versionControlSystem.checkout.CheckoutProvider;
import consulo.versionControlSystem.distributed.DvcsUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.HgVcs;
import org.zmlx.hg4idea.HgVcsMessages;
import org.zmlx.hg4idea.action.HgCommandResultNotifier;
import org.zmlx.hg4idea.command.HgCloneCommand;
import org.zmlx.hg4idea.execution.HgCommandResult;
import org.zmlx.hg4idea.ui.HgCloneDialog;
import org.zmlx.hg4idea.util.HgErrorUtil;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

@ExtensionImpl
public class HgCheckoutProvider implements CheckoutProvider {

    @Override
    public void doCheckout(@Nonnull final Project project, @Nullable final Listener listener) {
        FileDocumentManager.getInstance().saveAllDocuments();

        final HgCloneDialog dialog = new HgCloneDialog(project);
        if (!dialog.showAndGet()) {
            return;
        }
        dialog.rememberSettings();
        VirtualFile destinationParent = LocalFileSystem.getInstance().findFileByIoFile(new File(dialog.getParentDirectory()));
        if (destinationParent == null) {
            return;
        }
        final String targetDir = destinationParent.getPath() + File.separator + dialog.getDirectoryName();
        final String sourceRepositoryURL = dialog.getSourceRepositoryURL();
        final AtomicReference<HgCommandResult> cloneResult = new AtomicReference<>();
        new Task.Backgroundable(project, HgVcsMessages.message("hg4idea.clone.progress", sourceRepositoryURL), true) {
            @Override
            public void run(@Nonnull ProgressIndicator indicator) {
                HgCloneCommand clone = new HgCloneCommand(project);
                clone.setRepositoryURL(sourceRepositoryURL);
                clone.setDirectory(targetDir);
                cloneResult.set(clone.executeInCurrentThread());
            }

            @Override
            public void onSuccess() {
                if (cloneResult.get() == null || HgErrorUtil.hasErrorsInCommandExecution(cloneResult.get())) {
                    new HgCommandResultNotifier(project).notifyError(cloneResult.get(), "Clone failed",
                        "Clone from " + sourceRepositoryURL + " failed.");
                }
                else {
                    DvcsUtil.addMappingIfSubRoot(project, targetDir, HgVcs.VCS_ID);
                    if (listener != null) {
                        listener.directoryCheckedOut(new File(dialog.getParentDirectory(), dialog.getDirectoryName()), HgVcs.getKey());
                        listener.checkoutCompleted();
                    }
                }
            }
        }.queue();
    }

    @Nonnull
    @Override
    public LocalizeValue getName() {
        return HgLocalize.hg4ideaMercurial();
    }
}
