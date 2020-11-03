// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.zmlx.hg4idea.status.ui;

import com.intellij.dvcs.DvcsUtil;
import com.intellij.dvcs.repo.VcsRepositoryMappingListener;
import com.intellij.dvcs.ui.DvcsStatusWidget;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager;
import consulo.disposer.Disposer;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nls;
import org.zmlx.hg4idea.HgProjectSettings;
import org.zmlx.hg4idea.HgVcs;
import org.zmlx.hg4idea.HgVcsMessages;
import org.zmlx.hg4idea.branch.HgBranchPopup;
import org.zmlx.hg4idea.repo.HgRepository;
import org.zmlx.hg4idea.repo.HgRepositoryManager;
import org.zmlx.hg4idea.util.HgUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Widget to display basic hg status in the status bar.
 */
public class HgStatusWidget extends DvcsStatusWidget<HgRepository>
{
	private static final String ID = "hg";

	@Nonnull
	private final HgVcs myVcs;
	@Nonnull
	private final HgProjectSettings myProjectSettings;

	public HgStatusWidget(@Nonnull HgVcs vcs, @Nonnull Project project, @Nonnull HgProjectSettings projectSettings)
	{
		super(project, vcs.getShortName());
		myVcs = vcs;
		myProjectSettings = projectSettings;

		project.getMessageBus().connect(this).subscribe(HgVcs.STATUS_TOPIC, (p, root) -> updateLater());
	}

	@Override
	public
	@Nonnull
	String ID()
	{
		return ID;
	}

	@Override
	public StatusBarWidget copy()
	{
		return new HgStatusWidget(myVcs, myProject, myProjectSettings);
	}

	@Nullable
	@Override
	protected HgRepository guessCurrentRepository(@Nonnull Project project)
	{
		return DvcsUtil.guessCurrentRepositoryQuick(project, HgUtil.getRepositoryManager(project),
				HgProjectSettings.getInstance(project).getRecentRootPath());
	}

	@Nonnull
	@Override
	protected String getFullBranchName(@Nonnull HgRepository repository)
	{
		return HgUtil.getDisplayableBranchOrBookmarkText(repository);
	}

	@Override
	protected boolean isMultiRoot(@Nonnull Project project)
	{
		return HgUtil.getRepositoryManager(project).moreThanOneRoot();
	}

	@Nonnull
	@Override
	protected ListPopup getPopup(@Nonnull Project project, @Nonnull HgRepository repository)
	{
		return HgBranchPopup.getInstance(project, repository).asListPopup();
	}

	@Override
	protected void rememberRecentRoot(@Nonnull String path)
	{
		myProjectSettings.setRecentRootPath(path);
	}

	public static class Listener implements VcsRepositoryMappingListener
	{
		private final Project myProject;

		@Inject
		public Listener(@Nonnull Project project)
		{
			myProject = project;
		}

		@Override
		public void mappingChanged()
		{
			StatusBarWidgetsManager.getInstance(myProject).updateWidget(Factory.class);
		}
	}

	public static class Factory implements StatusBarWidgetFactory
	{
		@Override
		@Nonnull
		public String getId()
		{
			return ID;
		}

		@Override
		@Nls
		@Nonnull
		public String getDisplayName()
		{
			return HgVcsMessages.message("hg4idea.status.bar.widget.name");
		}

		@Override
		public boolean isAvailable(@Nonnull Project project)
		{
			return !HgRepositoryManager.getInstance(project).getRepositories().isEmpty();
		}

		@Override
		@Nonnull
		public StatusBarWidget createWidget(@Nonnull Project project)
		{
			return new HgStatusWidget(Objects.requireNonNull(HgVcs.getInstance(project)), project, HgProjectSettings.getInstance(project));
		}

		@Override
		public void disposeWidget(@Nonnull StatusBarWidget widget)
		{
			Disposer.dispose(widget);
		}

		@Override
		public boolean canBeEnabledOn(@Nonnull StatusBar statusBar)
		{
			return true;
		}
	}
}
