/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zmlx.hg4idea;

import consulo.annotation.component.ExtensionImpl;
import consulo.ide.ServiceManager;
import consulo.ide.impl.idea.ide.ui.OptionsTopHitProvider;
import consulo.ide.impl.idea.ide.ui.PublicMethodBasedOptionDescription;
import consulo.ide.impl.idea.ide.ui.search.BooleanOptionDescription;
import consulo.project.Project;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsDescriptor;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Sergey.Malenkov
 */
@ExtensionImpl
public final class HgOptionsTopHitProvider extends OptionsTopHitProvider {
  @Override
  public String getId() {
    return "vcs";
  }

  @Nonnull
  @Override
  public Collection<BooleanOptionDescription> getOptions(@Nullable Project project) {
    if (project != null) {
      for (VcsDescriptor descriptor : ProjectLevelVcsManager.getInstance(project).getAllVcss()) {
        if ("Mercurial".equals(descriptor.getDisplayName())) {
          return Collections.unmodifiableCollection(Arrays.asList(
            option(project, "Mercurial: Check for incoming and outgoing changesets", "isCheckIncomingOutgoing", "setCheckIncomingOutgoing"),
            option(project,
                   "Mercurial: Ignore whitespace differences in annotations",
                   "isWhitespacesIgnoredInAnnotations",
                   "setIgnoreWhitespacesInAnnotations")));
        }
      }
    }
    return Collections.emptyList();
  }

  private static BooleanOptionDescription option(final Project project, String option, String getter, String setter) {
    return new PublicMethodBasedOptionDescription(option, "vcs.Mercurial", getter, setter) {
      @Override
      public Object getInstance() {
        return ServiceManager.getService(project, HgProjectSettings.class);
      }
    };
  }
}
