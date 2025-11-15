// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.zmlx.hg4idea.status.ui;

import consulo.application.Application;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposer;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.event.FileEditorManagerEvent;
import consulo.fileEditor.statusBar.EditorBasedWidget;
import consulo.localize.LocalizeValue;
import consulo.mercurial.localize.HgLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.versionControlSystem.distributed.repository.VcsRepositoryMappingListener;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import org.zmlx.hg4idea.HgRemoteUpdater;
import org.zmlx.hg4idea.HgStatusUpdater;
import org.zmlx.hg4idea.HgVcs;
import org.zmlx.hg4idea.HgVcsMessages;
import org.zmlx.hg4idea.status.HgChangesetStatus;
import org.zmlx.hg4idea.status.HgRemoteStatusUpdater;

import java.awt.event.MouseEvent;
import java.util.function.Consumer;

final class HgIncomingOutgoingWidget extends EditorBasedWidget implements StatusBarWidget.IconPresentation, StatusBarWidget.Multiframe {
    @Nonnull
    private final HgVcs myVcs;
    private final boolean myIsIncoming;
    @Nonnull
    private final Image myEnabledIcon;
    @Nonnull
    private final Image myDisabledIcon;

    private volatile LocalizeValue myTooltip = LocalizeValue.empty();
    private Image myCurrentIcon;

    public HgIncomingOutgoingWidget(@Nonnull HgVcs vcs, @Nonnull StatusBarWidgetFactory factory, boolean isIncoming) {
        super(vcs.getProject(), factory);
        myIsIncoming = isIncoming;
        myVcs = vcs;
        myEnabledIcon = myIsIncoming ? PlatformIconGroup.ideIncomingchangeson() : PlatformIconGroup.ideOutgoingchangeson();
        myDisabledIcon = ImageEffects.grayed(myEnabledIcon);
        myCurrentIcon = myDisabledIcon;

        MessageBusConnection busConnection = myProject.getMessageBus().connect();
        busConnection.subscribe(VcsRepositoryMappingListener.class, this::updateLater);
        busConnection.subscribe(HgStatusUpdater.class, (project, root) -> updateLater());
        busConnection.subscribe(HgRemoteUpdater.class, (project, root) -> updateLater());
        busConnection.subscribe(HgWidgetUpdater.class, new HgWidgetUpdater() {
            @Override
            public void update() {
                updateLater();
            }
        });
        updateLater();
    }

    @Override
    public StatusBarWidget copy() {
        return new HgIncomingOutgoingWidget(myVcs, myFactory, myIsIncoming);
    }

    @Override
    public WidgetPresentation getPresentation() {
        return this;
    }

    @Override
    public void selectionChanged(@Nonnull FileEditorManagerEvent event) {
        updateLater();
    }

    @Override
    public void fileOpened(@Nonnull FileEditorManager source, @Nonnull VirtualFile file) {
        updateLater();
    }

    @Override
    public void fileClosed(@Nonnull FileEditorManager source, @Nonnull VirtualFile file) {
        updateLater();
    }

    @Nonnull
    @Override
    public LocalizeValue getTooltipText() {
        return myTooltip;
    }

    @Override
    // Updates branch information on click
    public Consumer<MouseEvent> getClickConsumer() {
        return mouseEvent -> updateLater();
    }

    public void updateLater() {
        Application.get().invokeLater(() -> {
            if (Disposer.isDisposed(this)) {
                return;
            }

            HgRemoteStatusUpdater statusUpdater = myVcs.getRemoteStatusUpdater();
            if (statusUpdater == null) {
                return;
            }
            HgChangesetStatus status = statusUpdater.getStatus(myIsIncoming);
            boolean changesAvailable = status.getNumChanges() > 0;
            myCurrentIcon = changesAvailable ? myEnabledIcon : myDisabledIcon;
            myTooltip = changesAvailable ? LocalizeValue.of("\n" + status.getToolTip()) : HgLocalize.noChangesAvailable();
            if (myStatusBar != null) {
                myStatusBar.updateWidget(ID());
            }
        });
    }

    @Nonnull
    @Override
    public Image getIcon() {
        return myCurrentIcon;
    }

    //if smb call hide widget then it removed from status bar ans dispose method called.
    // if we do not override dispose IDE call EditorWidget dispose method and set connection to null.
    //next, if we repeat hide/show dispose eth will be callees several times,but connection will be null -> NPE or already disposed message.
    @Override
    public void dispose() {
        if (!isDisposed()) {
            super.dispose();
        }
    }
}

