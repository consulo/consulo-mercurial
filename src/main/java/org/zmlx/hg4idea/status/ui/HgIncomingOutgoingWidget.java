// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.zmlx.hg4idea.status.ui;

import com.intellij.dvcs.repo.VcsRepositoryManager;
import com.intellij.dvcs.repo.VcsRepositoryMappingListener;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.intellij.openapi.wm.impl.status.EditorBasedWidget;
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager;
import com.intellij.util.Consumer;
import com.intellij.util.messages.MessageBusConnection;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import org.jetbrains.annotations.Nls;
import org.zmlx.hg4idea.HgProjectSettings;
import org.zmlx.hg4idea.HgVcs;
import org.zmlx.hg4idea.HgVcsMessages;
import org.zmlx.hg4idea.status.HgChangesetStatus;
import org.zmlx.hg4idea.status.HgRemoteStatusUpdater;

import javax.annotation.Nonnull;
import java.awt.event.MouseEvent;
import java.util.Objects;

final class HgIncomingOutgoingWidget extends EditorBasedWidget implements StatusBarWidget.IconPresentation, StatusBarWidget.Multiframe
{
	private static final String INCOMING_WIDGET_ID = "InHgIncomingOutgoingWidget";
	private static final String OUTGOING_WIDGET_ID = "OutHgIncomingOutgoingWidget";

	@Nonnull
	private final HgVcs myVcs;
	private final boolean myIsIncoming;
	@Nonnull
	private final Image myEnabledIcon;
	@Nonnull
	private final Image myDisabledIcon;

	private volatile String myTooltip = "";
	private Image myCurrentIcon;

	public HgIncomingOutgoingWidget(@Nonnull HgVcs vcs, boolean isIncoming)
	{
		super(vcs.getProject());
		myIsIncoming = isIncoming;
		myVcs = vcs;
		myEnabledIcon = myIsIncoming ? AllIcons.Ide.IncomingChangesOn : AllIcons.Ide.OutgoingChangesOn;
		myDisabledIcon = ImageEffects.grayed(myEnabledIcon);
		myCurrentIcon = myDisabledIcon;

		MessageBusConnection busConnection = myProject.getMessageBus().connect();
		busConnection.subscribe(VcsRepositoryManager.VCS_REPOSITORY_MAPPING_UPDATED, () -> updateLater());
		busConnection.subscribe(HgVcs.STATUS_TOPIC, (project, root) -> updateLater());
		busConnection.subscribe(HgVcs.REMOTE_TOPIC, (project, root) -> updateLater());
		busConnection.subscribe(HgVcs.INCOMING_OUTGOING_CHECK_TOPIC, new HgWidgetUpdater()
		{
			@Override
			public void update()
			{
				updateLater();
			}
		});
		updateLater();
	}

	@Override
	public StatusBarWidget copy()
	{
		return new HgIncomingOutgoingWidget(myVcs, myIsIncoming);
	}

	@Nonnull
	@Override
	public String ID()
	{
		return myIsIncoming ? INCOMING_WIDGET_ID : OUTGOING_WIDGET_ID;
	}

	@Override
	public WidgetPresentation getPresentation()
	{
		return this;
	}

	@Override
	public void selectionChanged(@Nonnull FileEditorManagerEvent event)
	{
		updateLater();
	}

	@Override
	public void fileOpened(@Nonnull FileEditorManager source, @Nonnull VirtualFile file)
	{
		updateLater();
	}

	@Override
	public void fileClosed(@Nonnull FileEditorManager source, @Nonnull VirtualFile file)
	{
		updateLater();
	}

	@Override
	public String getTooltipText()
	{
		return myTooltip;
	}

	@Override
	// Updates branch information on click
	public Consumer<MouseEvent> getClickConsumer()
	{
		return mouseEvent -> updateLater();
	}

	public void updateLater()
	{
		ApplicationManager.getApplication().invokeLater(() -> {
			if(Disposer.isDisposed(this))
			{
				return;
			}

			HgRemoteStatusUpdater statusUpdater = myVcs.getRemoteStatusUpdater();
			if(statusUpdater == null)
			{
				return;
			}
			HgChangesetStatus status = statusUpdater.getStatus(myIsIncoming);
			boolean changesAvailable = status.getNumChanges() > 0;
			myCurrentIcon = changesAvailable ? myEnabledIcon : myDisabledIcon;
			myTooltip = changesAvailable ? "\n" + status.getToolTip() : HgVcsMessages.message("no.changes.available");
			if(myStatusBar != null)
			{
				myStatusBar.updateWidget(ID());
			}
		});
	}

	@Nonnull
	@Override
	public Image getIcon()
	{
		return myCurrentIcon;
	}

	//if smb call hide widget then it removed from status bar ans dispose method called.
	// if we do not override dispose IDE call EditorWidget dispose method and set connection to null.
	//next, if we repeat hide/show dispose eth will be callees several times,but connection will be null -> NPE or already disposed message.
	@Override
	public void dispose()
	{
		if(!isDisposed())
		{
			super.dispose();
		}
	}

	public static class Listener implements VcsRepositoryMappingListener, HgWidgetUpdater
	{
		private final Project myProject;

		public Listener(@Nonnull Project project)
		{
			myProject = project;
		}

		@Override
		public void mappingChanged()
		{
			updateVisibility();
		}

		@Override
		public void updateVisibility()
		{
			StatusBarWidgetsManager widgetsManager = StatusBarWidgetsManager.getInstance(myProject);
			widgetsManager.updateWidget(HgIncomingOutgoingWidget.IncomingFactory.class);
			widgetsManager.updateWidget(HgIncomingOutgoingWidget.OutgoingFactory.class);
		}
	}

	public static class IncomingFactory extends MyWidgetFactory
	{
		public IncomingFactory()
		{
			super(true);
		}
	}

	public static class OutgoingFactory extends MyWidgetFactory
	{
		public OutgoingFactory()
		{
			super(false);
		}
	}

	private static abstract class MyWidgetFactory implements StatusBarWidgetFactory
	{
		private final boolean myIsIncoming;

		protected MyWidgetFactory(boolean isIncoming)
		{
			myIsIncoming = isIncoming;
		}

		@Override
		public boolean isAvailable(@Nonnull Project project)
		{
			return HgProjectSettings.getInstance(project).isCheckIncomingOutgoing();
		}

		@Override
		public
		@Nonnull
		StatusBarWidget createWidget(@Nonnull Project project)
		{
			HgVcs hgVcs = Objects.requireNonNull(HgVcs.getInstance(project));
			return new HgIncomingOutgoingWidget(hgVcs, myIsIncoming);
		}

		@Override
		public void disposeWidget(@Nonnull StatusBarWidget widget)
		{
			consulo.disposer.Disposer.dispose(widget);
		}

		@Override
		public boolean canBeEnabledOn(@Nonnull StatusBar statusBar)
		{
			return true;
		}

		@Override
		public
		@Nonnull
		String getId()
		{
			return myIsIncoming ? INCOMING_WIDGET_ID : OUTGOING_WIDGET_ID;
		}

		@Override
		public
		@Nls
		@Nonnull
		String getDisplayName()
		{
			return myIsIncoming ? HgVcsMessages.message("hg4idea.status.bar.incoming.widget.name")
					: HgVcsMessages.message("hg4idea.status.bar.outgoing.widget.name");
		}
	}
}

