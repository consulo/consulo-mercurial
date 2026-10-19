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
package org.zmlx.hg4idea.push;

import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Length;
import consulo.ui.TextItemPresentation;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.util.TextAttributeUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.distributed.DvcsUtil;
import consulo.versionControlSystem.distributed.push.PushTargetCompletionProvider;
import consulo.versionControlSystem.distributed.push.PushTargetEditorListener;
import consulo.versionControlSystem.distributed.push.PushTargetPanel;
import consulo.versionControlSystem.distributed.push.VcsError;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.repo.HgRepository;
import org.zmlx.hg4idea.util.HgUtil;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class HgPushTargetPanel extends PushTargetPanel<HgTarget> {
    private final static String ENTER_REMOTE = "Enter Remote";
    private final HgRepository myRepository;
    private final String myBranchName;
    private final List<PushTargetEditorListener> myListeners = new CopyOnWriteArrayList<>();

    @Nonnull
    private String myTargetText;
    @Nonnull
    private String myEditedText;
    @Nullable
    private EditorBox myDestTargetPanel;

    public HgPushTargetPanel(@Nonnull HgRepository repository, @Nullable HgTarget defaultTarget) {
        myRepository = repository;
        myBranchName = myRepository.getCurrentBranchName();
        String defaultText = defaultTarget != null ? defaultTarget.getPresentation() : "";
        myTargetText = defaultText;
        myEditedText = defaultText;
    }

    @Nonnull
    @RequiredUIAccess
    @Override
    public Component getComponent() {
        EditorBox destTargetPanel = myDestTargetPanel;
        if (destTargetPanel == null) {
            Project project = myRepository.getProject();
            destTargetPanel = project.getApplication().getInstance(EditorBoxBuilderFactory.class)
                .create(project)
                .text(myEditedText)
                .completion(new PushTargetCompletionProvider(HgUtil.getTargetNames(myRepository)))
                .build();
            destTargetPanel.setWidth(Length.ofFont(30));
            destTargetPanel.addValueListener(event -> {
                String value = event.getValue();
                myEditedText = value == null ? "" : value;
                for (PushTargetEditorListener listener : myListeners) {
                    listener.onTargetInEditModeChanged(myEditedText);
                }
            });
            myDestTargetPanel = destTargetPanel;
        }
        return destTargetPanel;
    }

    @RequiredUIAccess
    @Override
    public void editingStarted() {
        EditorBox destTargetPanel = myDestTargetPanel;
        if (destTargetPanel != null) {
            destTargetPanel.setValue(myEditedText, false);
            destTargetPanel.focus();
        }
    }

    @Override
    public void render(@Nonnull TextItemPresentation presentation, boolean isSelected, boolean isActive, @Nullable String forceRenderedText) {
        if (forceRenderedText != null) {
            myEditedText = forceRenderedText;
            presentation.append(forceRenderedText);
            return;
        }
        if (StringUtil.isEmptyOrSpaces(myTargetText)) {
            presentation.append(ENTER_REMOTE, TextAttributeUtil.toTextAttribute(SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES));
            return;
        }
        SimpleTextAttributes attributes = isActive ? SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES : SimpleTextAttributes.GRAYED_ATTRIBUTES;
        presentation.append(myTargetText, TextAttributeUtil.toTextAttribute(attributes));
    }

    @Override
    public boolean isEditable() {
        return true;
    }

    @Override
    @Nullable
    public HgTarget getValue() {
        return createValidPushTarget();
    }

    @Nonnull
    private HgTarget createValidPushTarget() {
        return new HgTarget(myEditedText, myBranchName);
    }

    @RequiredUIAccess
    @Override
    public void fireOnCancel() {
        myEditedText = myTargetText;
        EditorBox destTargetPanel = myDestTargetPanel;
        if (destTargetPanel != null) {
            destTargetPanel.setValue(myTargetText, false);
        }
    }

    @RequiredUIAccess
    @Override
    public void fireOnChange() {
        myTargetText = myEditedText;
    }

    @Nonnull
    @RequiredUIAccess
    @Override
    public LocalizeValue verify() {
        if (StringUtil.isEmptyOrSpaces(myEditedText)) {
            return VcsError.createEmptyTargetError(DvcsUtil.getShortRepositoryName(myRepository)).getText();
        }
        return LocalizeValue.empty();
    }

    @Override
    public void setFireOnChangeAction(@Nonnull Runnable action) {
        // no extra changing components => ignore
    }

    @Override
    public void addTargetEditorListener(@Nonnull PushTargetEditorListener listener) {
        myListeners.add(listener);
    }

    @RequiredUIAccess
    @Override
    public void forceUpdateEditableUiModel(@Nonnull String forcedText) {
        myEditedText = forcedText;
        EditorBox destTargetPanel = myDestTargetPanel;
        if (destTargetPanel != null) {
            destTargetPanel.setValue(forcedText, false);
        }
    }
}
