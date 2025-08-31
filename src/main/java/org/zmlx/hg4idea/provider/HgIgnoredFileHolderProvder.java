package org.zmlx.hg4idea.provider;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.versionControlSystem.VcsKey;
import consulo.versionControlSystem.change.VcsIgnoredFilesHolder;
import consulo.versionControlSystem.change.VcsIgnoredFilesHolderProvider;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.zmlx.hg4idea.HgVcs;

/**
 * @author VISTALL
 * @since 2025-08-31
 */
@ExtensionImpl
public class HgIgnoredFileHolderProvder implements VcsIgnoredFilesHolderProvider {
    private final Project myProject;

    @Inject
    public HgIgnoredFileHolderProvder(Project project) {
        myProject = project;
    }

    @Nonnull
    @Override
    public VcsKey getVcsKey() {
        return HgVcs.getKey();
    }

    @Nonnull
    @Override
    public VcsIgnoredFilesHolder create() {
        return new HgIgnoredFileHolder(myProject);
    }
}
