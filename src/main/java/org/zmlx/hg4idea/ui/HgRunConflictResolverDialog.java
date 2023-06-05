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
package org.zmlx.hg4idea.ui;

import consulo.application.ApplicationManager;
import consulo.project.Project;
import consulo.ui.ModalityState;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.HgFile;
import org.zmlx.hg4idea.command.HgResolveCommand;
import org.zmlx.hg4idea.command.HgResolveStatusEnum;
import org.zmlx.hg4idea.repo.HgRepository;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

public class HgRunConflictResolverDialog extends DialogWrapper {

  private JPanel mainPanel;
  private HgRepositorySelectorComponent repositorySelector;
  private JList conflictsList;

  private final Project project;

  public HgRunConflictResolverDialog(@Nonnull Project project,
                                     @Nonnull Collection<HgRepository> repositories,
                                     @Nullable HgRepository selectedRepo) {
    super(project, false);
    this.project = project;
    repositorySelector.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        onChangeRepository();
      }
    });
    setTitle("Resolve Conflicts");
    init();
    setRoots(repositories, selectedRepo);
  }

  @Nonnull
  public HgRepository getRepository() {
    return repositorySelector.getRepository();
  }

  private void setRoots(@Nonnull Collection<HgRepository> repositories, @Nullable HgRepository selectedRepo) {
    repositorySelector.setRoots(repositories);
    repositorySelector.setSelectedRoot(selectedRepo);
    onChangeRepository();
  }

  protected JComponent createCenterPanel() {
    return mainPanel;
  }

  private void onChangeRepository() {
    VirtualFile repo = repositorySelector.getRepository().getRoot();
    HgResolveCommand command = new HgResolveCommand(project);
    final ModalityState modalityState = ApplicationManager.getApplication().getModalityStateForComponent(getRootPane());
    command.getListAsynchronously(repo, new Consumer<Map<HgFile, HgResolveStatusEnum>>() {
      @Override
      public void accept(Map<HgFile, HgResolveStatusEnum> status) {
        final DefaultListModel model = new DefaultListModel();
        for (Map.Entry<HgFile, HgResolveStatusEnum> entry : status.entrySet()) {
          if (entry.getValue() == HgResolveStatusEnum.UNRESOLVED) {
            model.addElement(entry.getKey().getRelativePath());
          }
        }
        ApplicationManager.getApplication().invokeLater(new Runnable() {

          @Override
          public void run() {
            setOKActionEnabled(!model.isEmpty());
            if (model.isEmpty()) {
              model.addElement("No conflicts to resolve");
            }
            conflictsList.setModel(model);
          }
        }, modalityState);
      }
    });
  }
}
