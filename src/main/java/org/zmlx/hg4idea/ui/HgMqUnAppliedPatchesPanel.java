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
package org.zmlx.hg4idea.ui;

import com.google.common.primitives.Ints;
import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.dataContext.DataProvider;
import consulo.ide.impl.idea.openapi.vcs.CalledInAny;
import consulo.ide.impl.idea.openapi.vcs.CalledInAwt;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.language.editor.CommonDataKeys;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.PopupHandler;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.speedSearch.TableSpeedSearch;
import consulo.ui.ex.awt.table.JBTable;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;
import org.zmlx.hg4idea.HgStatusUpdater;
import org.zmlx.hg4idea.command.mq.HgQDeleteCommand;
import org.zmlx.hg4idea.command.mq.HgQRenameCommand;
import org.zmlx.hg4idea.mq.HgMqAdditionalPatchReader;
import org.zmlx.hg4idea.mq.MqPatchDetails;
import org.zmlx.hg4idea.repo.HgRepository;
import org.zmlx.hg4idea.util.HgUtil;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class HgMqUnAppliedPatchesPanel extends JPanel implements DataProvider, HgStatusUpdater {

  public static final Key<HgMqUnAppliedPatchesPanel> MQ_PATCHES = Key.create("Mq.Patches");
  private static final String POPUP_ACTION_GROUP = "Mq.Patches.ContextMenu";
  private static final String TOOLBAR_ACTION_GROUP = "Mq.Patches.Toolbar";
  private static final Logger LOG = Logger.getInstance(HgMqUnAppliedPatchesPanel.class);
  private static final String START_EDITING = "startEditing";

  @Nonnull
  private final Project myProject;
  @Nonnull
  private final HgRepository myRepository;
  @Nonnull
  private final MyPatchTable myPatchTable;
  @Nullable
  private final VirtualFile myMqPatchDir;
  private volatile boolean myNeedToUpdateFileContent;
  @Nullable
  private final File mySeriesFile;

  public HgMqUnAppliedPatchesPanel(@Nonnull HgRepository repository) {
    super(new BorderLayout());
    myRepository = repository;
    myProject = myRepository.getProject();
    myMqPatchDir = myRepository.getHgDir().findChild("patches");
    mySeriesFile = myMqPatchDir != null ? new File(myMqPatchDir.getPath(), "series") : null;

    myPatchTable = new MyPatchTable(new MyPatchModel(myRepository.getUnappliedPatchNames()));
    myPatchTable.addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        updatePatchSeriesInBackground(null);
        super.focusLost(e);
      }
    });
    myPatchTable.setShowColumns(true);
    myPatchTable.setFillsViewportHeight(true);
    myPatchTable.getEmptyText().setText("Nothing to show");
    myPatchTable.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), START_EDITING);
    myPatchTable.setDragEnabled(true);
    new TableSpeedSearch(myPatchTable);
    myPatchTable.setDropMode(DropMode.INSERT_ROWS);
    myPatchTable.setTransferHandler(new TableRowsTransferHandler(myPatchTable));

    add(createToolbar(), BorderLayout.WEST);

    JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myPatchTable);
    add(scrollPane, BorderLayout.CENTER);
    myProject.getMessageBus().connect(myProject).subscribe(HgStatusUpdater.class, this);
  }

  private JComponent createToolbar() {
    MqRefreshAction mqRefreshAction = new MqRefreshAction();
    EmptyAction.setupAction(mqRefreshAction, "hg4idea.QRefresh", this);

    MqDeleteAction mqDeleteAction = new MqDeleteAction();
    EmptyAction.setupAction(mqDeleteAction, "hg4idea.QDelete", this);

    PopupHandler.installPopupHandler(myPatchTable, POPUP_ACTION_GROUP, ActionPlaces.PROJECT_VIEW_POPUP);

    ActionManager actionManager = ActionManager.getInstance();

    DefaultActionGroup toolbarGroup = new DefaultActionGroup();
    toolbarGroup.add(mqRefreshAction);
    toolbarGroup.add(actionManager.getAction("Hg.MQ.Unapplied"));
    toolbarGroup.add(mqDeleteAction);

    ActionToolbar toolbar = actionManager.createActionToolbar(TOOLBAR_ACTION_GROUP, toolbarGroup, false);
    toolbar.setTargetComponent(this);
    return toolbar.getComponent();
  }

  @CalledInAwt
  public void updatePatchSeriesInBackground(@Nullable final Runnable runAfterUpdate) {
    final String newContent = myNeedToUpdateFileContent ? getContentFromModel() : null;
    myNeedToUpdateFileContent = false;
    new Task.Backgroundable(myProject, "Updating patch series for " + myRepository.getPresentableUrl()) {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        if (newContent != null) {
          writeSeriesFile(newContent);
        }
        if (runAfterUpdate != null) {
          runAfterUpdate.run();
        }
      }
    }.queue();
  }

  private void writeSeriesFile(@Nonnull String newContent) {
    if (mySeriesFile == null || !mySeriesFile.exists()) return;
    try {
      FileUtil.writeToFile(mySeriesFile, newContent);
    }
    catch (IOException e1) {
      LOG.error("Could not modify mq series file", e1);
    }
    myRepository.update();
  }

  @Nonnull
  @CalledInAwt
  private String getContentFromModel() {
    StringBuilder content = new StringBuilder();
    String separator = "\n";
    StringUtil.join(HgUtil.getNamesWithoutHashes(myRepository.getMQAppliedPatches()), Function.identity(), separator, content);
    content.append(separator);
    //append unapplied patches
    for (int i = 0; i < myPatchTable.getRowCount(); i++) {
      content.append(getPatchName(i)).append(separator);
    }
    return content.toString();
  }

  @CalledInAwt
  private String getPatchName(int i) {
    return myPatchTable.getModel().getPatchName(i);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof HgMqUnAppliedPatchesPanel)) return false;

    HgMqUnAppliedPatchesPanel panel = (HgMqUnAppliedPatchesPanel)o;

    if (!myRepository.equals(panel.myRepository)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myRepository.hashCode();
  }

  @Nullable
  private VirtualFile getSelectedPatchFile() {
    if (myMqPatchDir == null || myPatchTable.getSelectedRowCount() != 1) return null;
    String patchName = getPatchName(myPatchTable.getSelectedRow());
    return VfsUtil.findFileByIoFile(new File(myMqPatchDir.getPath(), patchName), true);
  }

  @Nonnull
  @CalledInAwt
  public List<String> getSelectedPatchNames() {
    return getPatchNames(myPatchTable.getSelectedRows());
  }

  @Nonnull
  @CalledInAny
  private List<String> getPatchNames(int[] rows) {
    return ContainerUtil.map(Ints.asList(rows), integer -> getPatchName(integer));
  }

  @Nonnull
  public HgRepository getRepository() {
    return myRepository;
  }

  public int getSelectedRowsCount() {
    return myPatchTable.getSelectedRowCount();
  }

  @Nullable
  @Override
  public Object getData(@NonNls Key<?> dataId) {
    if (MQ_PATCHES == dataId) {
      return this;
    }
    else if (CommonDataKeys.VIRTUAL_FILE == dataId) {
      VirtualFile patchVFile = getSelectedPatchFile();
      if (patchVFile != null) return patchVFile;
    }
    return null;
  }

  @Override
  public void update(final Project project, @Nullable VirtualFile root) {
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        if (project != null && !project.isDisposed()) {
          refreshAll();
        }
      }
    });
  }

  private class MqDeleteAction extends DumbAwareAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
      final List<String> names = getSelectedPatchNames();
      if (names.isEmpty()) return;

      if (Messages.showOkCancelDialog(myRepository.getProject(), String
                                        .format("You are going to delete selected %s. Would you like to continue?",
                                                StringUtil.pluralize("patch", names.size())),
                                      "Delete Confirmation", Messages.getWarningIcon()) == Messages.OK) {
        Runnable deleteTask = new Runnable() {
          @Override
          public void run() {
            ProgressManager.getInstance().getProgressIndicator().setText("Deleting patches...");
            new HgQDeleteCommand(myRepository).executeInCurrentThread(names);
          }
        };
        updatePatchSeriesInBackground(deleteTask);
      }
    }

    @Override
    public void update(AnActionEvent e) {
      e.getPresentation().setEnabled(getSelectedRowsCount() != 0 && !myPatchTable.isEditing());
    }
  }

  private class MqRefreshAction extends DumbAwareAction {
    public void actionPerformed(AnActionEvent e) {
      refreshAll();
    }
  }

  private void refreshAll() {
    updateModel();
  }

  private void updateModel() {
    MyPatchModel model = myPatchTable.getModel();
    model.updatePatches(myRepository.getUnappliedPatchNames());
  }

  private class MyPatchModel extends AbstractTableModel implements MultiReorderedModel {

    @Nonnull
    private final MqPatchDetails.MqPatchEnum[] myColumnNames = MqPatchDetails.MqPatchEnum.values();
    @Nonnull
    private final Map<String, MqPatchDetails> myPatchesWithDetails = new HashMap<>();
    @Nonnull
    private final List<String> myPatches;

    public MyPatchModel(@Nonnull List<String> names) {
      myPatches = ContainerUtil.newArrayList(names);
      readMqPatchesDetails();
    }

    private void readMqPatchesDetails() {
      for (String name : myPatches) {
        File patchFile = myMqPatchDir != null ? new File(myMqPatchDir.getPath(), name) : null;
        myPatchesWithDetails.put(name, HgMqAdditionalPatchReader.readMqPatchInfo(myRepository.getRoot(), patchFile));
      }
    }

    @Override
    public int getColumnCount() {
      return myColumnNames.length;
    }

    @Override
    public String getColumnName(int col) {
      return myColumnNames[col].toString();
    }

    @Override
    public Class getColumnClass(int c) {
      return getValueAt(0, c).getClass();
    }

    @Override
    public int getRowCount() {
      return myPatches.size();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return columnIndex == 0;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      String name = getPatchName(rowIndex);
      if (columnIndex == 0) return name;
      MqPatchDetails patchDetails = myPatchesWithDetails.get(name);
      String mapDetail = patchDetails != null ? patchDetails.getPresentationDataFor(myColumnNames[columnIndex]) : "";
      return mapDetail != null ? mapDetail : "";
    }

    @Nonnull
    private String getPatchName(int rowIndex) {
      return myPatches.get(rowIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      String newPatchName = (String)aValue;
      myPatchesWithDetails.put(newPatchName, myPatchesWithDetails.remove(myPatches.get(rowIndex)));
      myPatches.set(rowIndex, newPatchName);
    }

    public void updatePatches(List<String> newNames) {
      myPatches.clear();
      myPatches.addAll(newNames);
      myPatchesWithDetails.clear();
      readMqPatchesDetails();
      fireTableDataChanged();
    }

    @Override
    public boolean canMoveRows() {
      return true;
    }

    @Override
    public int[] moveRows(int[] rowsIndexes, int destination) {
      List<String> names = getPatchNames(rowsIndexes);
      myPatches.removeAll(names);
      int[] selection = new int[rowsIndexes.length];
      for (int i = 0; i < rowsIndexes.length; i++) {
        selection[i] = destination;
        myPatches.add(destination++, names.get(i));
      }
      myNeedToUpdateFileContent = true;
      fireTableDataChanged();
      return selection;
    }
  }

  private class MyPatchTable extends JBTable {
    public MyPatchTable(MyPatchModel model) {
      super(model);
    }

    public MyPatchModel getModel() {
      return (MyPatchModel)dataModel;
    }

    @Override
    public void editingStopped(ChangeEvent e) {
      final int editingRow = getEditingRow();
      final String oldName = getModel().getPatchName(editingRow);
      super.editingStopped(e);
      updatePatchSeriesInBackground(new Runnable() {
        @Override
        public void run() {
          HgQRenameCommand.performPatchRename(myRepository, oldName, getModel().getPatchName(editingRow));
        }
      });
    }
  }
}
