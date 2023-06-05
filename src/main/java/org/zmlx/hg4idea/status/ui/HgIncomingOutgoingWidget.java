// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.zmlx.hg4idea.status.ui;

import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.component.messagebus.MessageBusConnection;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.event.FileEditorManagerEvent;
import consulo.fileEditor.statusBar.EditorBasedWidget;
import consulo.ide.impl.idea.openapi.util.Disposer;
import consulo.project.ui.wm.StatusBarWidget;
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
  static final String INCOMING_WIDGET_ID = "InHgIncomingOutgoingWidget";
  static final String OUTGOING_WIDGET_ID = "OutHgIncomingOutgoingWidget";

  @Nonnull
  private final HgVcs myVcs;
  private final boolean myIsIncoming;
  @Nonnull
  private final Image myEnabledIcon;
  @Nonnull
  private final Image myDisabledIcon;

  private volatile String myTooltip = "";
  private Image myCurrentIcon;

  public HgIncomingOutgoingWidget(@Nonnull HgVcs vcs, boolean isIncoming) {
    super(vcs.getProject());
    myIsIncoming = isIncoming;
    myVcs = vcs;
    myEnabledIcon = myIsIncoming ? AllIcons.Ide.IncomingChangesOn : AllIcons.Ide.OutgoingChangesOn;
    myDisabledIcon = ImageEffects.grayed(myEnabledIcon);
    myCurrentIcon = myDisabledIcon;

    MessageBusConnection busConnection = myProject.getMessageBus().connect();
    busConnection.subscribe(VcsRepositoryMappingListener.class, () -> updateLater());
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
    return new HgIncomingOutgoingWidget(myVcs, myIsIncoming);
  }

  @Nonnull
  @Override
  public String ID() {
    return myIsIncoming ? INCOMING_WIDGET_ID : OUTGOING_WIDGET_ID;
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

  @Override
  public String getTooltipText() {
    return myTooltip;
  }

  @Override
  // Updates branch information on click
  public Consumer<MouseEvent> getClickConsumer() {
    return mouseEvent -> updateLater();
  }

  public void updateLater() {
    ApplicationManager.getApplication().invokeLater(() -> {
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
      myTooltip = changesAvailable ? "\n" + status.getToolTip() : HgVcsMessages.message("no.changes.available");
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

