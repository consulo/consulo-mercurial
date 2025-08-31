package org.zmlx.hg4idea.repo;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import consulo.versionControlSystem.distributed.branch.DvcsSyncSettings;
import consulo.versionControlSystem.distributed.repository.AbstractRepositoryManager;
import consulo.versionControlSystem.distributed.repository.VcsRepositoryManager;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.zmlx.hg4idea.HgProjectSettings;
import org.zmlx.hg4idea.HgVcs;
import org.zmlx.hg4idea.branch.HgMultiRootBranchConfig;

import java.util.List;

@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class HgRepositoryManager extends AbstractRepositoryManager<HgRepository> {
    public static HgRepositoryManager getInstance(@Nonnull Project project) {
        return project.getInstance(HgRepositoryManager.class);
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
