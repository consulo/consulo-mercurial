package org.zmlx.hg4idea.repo;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.versionControlSystem.distributed.branch.DvcsSyncSettings;
import consulo.versionControlSystem.distributed.repository.AbstractRepositoryManager;
import consulo.versionControlSystem.distributed.repository.RepositoryManager;
import consulo.versionControlSystem.distributed.repository.VcsRepositoryManager;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.zmlx.hg4idea.HgProjectSettings;
import org.zmlx.hg4idea.HgVcs;
import org.zmlx.hg4idea.branch.HgMultiRootBranchConfig;

import java.util.List;

@ExtensionImpl
public class HgRepositoryManager extends AbstractRepositoryManager<HgRepository> {
    public static HgRepositoryManager getInstance(@Nonnull Project project) {
        return (HgRepositoryManager) RepositoryManager.<HgRepository>getInstance(project, HgVcs.getKey());
    }

    @Inject
    public HgRepositoryManager(@Nonnull Project project,
                               @Nonnull VcsRepositoryManager vcsRepositoryManager) {
        super(project, vcsRepositoryManager, HgVcs.getKey());
    }

    @Override
    public boolean isSyncEnabled() {
        HgProjectSettings projectSettings = ((HgVcs) getVcs()).getProjectSettings();
        return projectSettings.getSyncSetting() == DvcsSyncSettings.Value.SYNC && !new HgMultiRootBranchConfig(getRepositories()).diverged();
    }

    @Nonnull
    @Override
    public List<HgRepository> getRepositories() {
        return getRepositories(HgRepository.class);
    }
}
