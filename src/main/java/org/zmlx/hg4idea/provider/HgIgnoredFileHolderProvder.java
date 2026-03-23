package org.zmlx.hg4idea.provider;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.change.VcsManagedFilesHolder;
import consulo.versionControlSystem.change.VcsManagedIgnoredFilesHolderProvider;
import jakarta.inject.Inject;
import org.zmlx.hg4idea.HgVcs;

/**
 * @author VISTALL
 * @since 2025-08-31
 */
@ExtensionImpl
public class HgIgnoredFileHolderProvder implements VcsManagedIgnoredFilesHolderProvider {
    private final Project myProject;

    @Inject
    public HgIgnoredFileHolderProvder(Project project) {
        myProject = project;
    }

    @Override
    public AbstractVcs getVcs() {
        return HgVcs.getInstance(myProject);
    }

    @Override
    public VcsManagedFilesHolder createHolder() {
        return new HgIgnoredFileHolder(myProject);
    }
}
