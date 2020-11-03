package org.zmlx.hg4idea.repo;

import com.intellij.dvcs.branch.DvcsSyncSettings;
import com.intellij.dvcs.repo.AbstractRepositoryManager;
import com.intellij.dvcs.repo.VcsRepositoryManager;
import com.intellij.openapi.project.Project;
import consulo.util.lang.ObjectUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.zmlx.hg4idea.HgProjectSettings;
import org.zmlx.hg4idea.HgVcs;
import org.zmlx.hg4idea.branch.HgMultiRootBranchConfig;
import org.zmlx.hg4idea.util.HgUtil;

import javax.annotation.Nonnull;
import java.util.List;

@Singleton
public class HgRepositoryManager extends AbstractRepositoryManager<HgRepository>
{
	public static HgRepositoryManager getInstance(@Nonnull Project project)
	{
		return project.getInstance(HgRepositoryManager.class);
	}

	private final HgProjectSettings mySettings;

	@Inject
	public HgRepositoryManager(@Nonnull Project project,
							   @Nonnull VcsRepositoryManager vcsRepositoryManager)
	{
		super(vcsRepositoryManager, HgVcs.getInstance(project), HgUtil.DOT_HG);
		mySettings = ObjectUtil.assertNotNull(HgVcs.getInstance(project)).getProjectSettings();
	}

	@Override
	public boolean isSyncEnabled()
	{
		return mySettings.getSyncSetting() == DvcsSyncSettings.Value.SYNC && !new HgMultiRootBranchConfig(getRepositories()).diverged();
	}

	@Nonnull
	@Override
	public List<HgRepository> getRepositories()
	{
		return getRepositories(HgRepository.class);
	}
}
