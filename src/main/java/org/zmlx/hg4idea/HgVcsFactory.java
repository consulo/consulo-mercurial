package org.zmlx.hg4idea;

import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsFactory;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * @author VISTALL
 * @since 05/06/2023
 */
@ExtensionImpl
public class HgVcsFactory implements VcsFactory {
  private static final LocalizeValue Mercurial = LocalizeValue.localizeTODO("Mercurial");

  private final Project myProject;
  private final Provider<HgGlobalSettings> myGlobalSettingsProvider;
  private final Provider<HgProjectSettings> myProjectSettingsProvider;
  private final Provider<ProjectLevelVcsManager> myProjectLevelVcsManagerProvider;

  @Inject
  public HgVcsFactory(Project project,
                      Provider<HgGlobalSettings> globalSettingsProvider,
                      Provider<HgProjectSettings> projectSettingsProvider,
                      Provider<ProjectLevelVcsManager> projectLevelVcsManagerProvider) {
    myProject = project;
    myGlobalSettingsProvider = globalSettingsProvider;
    myProjectSettingsProvider = projectSettingsProvider;
    myProjectLevelVcsManagerProvider = projectLevelVcsManagerProvider;
  }

  @Nonnull
  @Override
  public String getId() {
    return "Mercurial";
  }

  @Nonnull
  @Override
  public LocalizeValue getDisplayName() {
    return Mercurial;
  }

  @Nonnull
  @Override
  public String getAdministrativeAreaName() {
    return ".hg";
  }

  @Nonnull
  @Override
  public AbstractVcs<?> createVcs() {
    return new HgVcs(myProject, myGlobalSettingsProvider.get(), myProjectSettingsProvider.get(), myProjectLevelVcsManagerProvider.get());
  }
}
