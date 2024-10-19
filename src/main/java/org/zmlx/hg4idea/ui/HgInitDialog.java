/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.configurable.ConfigurationException;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.IdeaFileChooser;
import consulo.mercurial.localize.HgLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.TextFieldWithBrowseButton;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.action.HgInit;
import org.zmlx.hg4idea.util.HgUtil;

import javax.swing.*;

/**
 * The HgInitDialog appears when user wants to create new Mercurial repository, in response to the
 * {@link HgInit} action.
 * It provides two options - create repository for the whole project or select a directory for the repository.
 * Also if the project directory already is a mercurial root, then no options are provided.
 * Instead a file chooser appears to select directory for the repository.
 *
 * @author Kirill Likhodedov
 * @see HgInit
 */
public class HgInitDialog extends DialogWrapper {
    private JPanel contentPane;
    private JRadioButton myCreateRepositoryForTheRadioButton;
    private JRadioButton mySelectWhereToCreateRadioButton;
    private TextFieldWithBrowseButton myTextFieldBrowser;

    @Nullable
    private final Project myProject;
    private final boolean myShowDialog; // basing on this field, show options or invoke file chooser at once
    private final FileChooserDescriptor myFileDescriptor;
    private VirtualFile mySelectedDir;

    public HgInitDialog(@Nullable Project project) {
        super(project);
        myProject = project;
        // a file chooser instead of dialog will be shown immediately if there is no current project or if current project is already an hg root
        myShowDialog = (myProject != null && (!myProject.isDefault()) && !HgUtil.isHgRoot(myProject.getBaseDir()));

        myFileDescriptor = new FileChooserDescriptor(false, true, false, false, false, false) {
            @Override
            public void validateSelectedFiles(VirtualFile[] files) throws Exception {
                if (HgUtil.isHgRoot(files[0])) {
                    throw new ConfigurationException(HgLocalize.hg4ideaInitThisIsHgRoot(files[0].getPresentableUrl()).get());
                }
                updateEverything();
            }
        };
        myFileDescriptor.setHideIgnored(false);

        init();
    }

    @Override
    protected void init() {
        super.init();
        setTitle(HgLocalize.hg4ideaInitDialogTitle());
        if (myProject != null && (!myProject.isDefault())) {
            mySelectedDir = myProject.getBaseDir();
        }

        mySelectWhereToCreateRadioButton.addActionListener(e -> {
            myTextFieldBrowser.setEnabled(true);
            updateEverything();
        });
        myCreateRepositoryForTheRadioButton.addActionListener(e -> {
            myTextFieldBrowser.setEnabled(false);
            updateEverything();
        });
        myTextFieldBrowser.getTextField().addCaretListener(e -> updateEverything());

        myTextFieldBrowser.addBrowseFolderListener(
            HgLocalize.hg4ideaInitDestinationDirectoryTitle().get(),
            HgLocalize.hg4ideaInitDestinationDirectoryDescription().get(),
            myProject,
            myFileDescriptor
        );
    }

    /**
     * Show the dialog OR show a FileChooser to select target directory.
     */
    @Override
    @RequiredUIAccess
    public void show() {
        if (myShowDialog) {
            super.show();
        }
        else {
            mySelectedDir = IdeaFileChooser.chooseFile(myFileDescriptor, myProject, null);
        }
    }

    @Override
    public boolean isOK() {
        return myShowDialog ? super.isOK() : mySelectedDir != null;
    }

    @Override
    protected String getHelpId() {
        return "reference.mercurial.create.mercurial.repository";
    }

    @Nullable
    public VirtualFile getSelectedFolder() {
        return mySelectedDir;
    }

    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    /**
     * Based on the selected option and entered path to the target directory,
     * enable/disable the 'OK' button, show error text and update mySelectedDir.
     */
    private void updateEverything() {
        if (myShowDialog && myCreateRepositoryForTheRadioButton.isSelected()) {
            enableOKAction();
            mySelectedDir = myProject.getBaseDir();
        }
        else {
            final VirtualFile vf = VcsUtil.getVirtualFile(myTextFieldBrowser.getText());
            if (vf == null) {
                disableOKAction();
                mySelectedDir = null;
                return;
            }
            vf.refresh(false, false);
            if (vf.exists() && vf.isValid() && vf.isDirectory()) {
                enableOKAction();
                mySelectedDir = vf;
            }
            else {
                disableOKAction();
                mySelectedDir = null;
            }
        }
    }

    private void enableOKAction() {
        clearErrorText();
        setOKActionEnabled(true);
    }

    private void disableOKAction() {
        setErrorText(HgLocalize.hg4ideaInitDialogIncorrectPath());
        setOKActionEnabled(false);
    }
}
