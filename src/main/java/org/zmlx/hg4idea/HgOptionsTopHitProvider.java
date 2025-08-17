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
import consulo.component.ComponentManager;
import consulo.project.Project;
import consulo.ui.ex.action.BooleanOptionDescription;
import consulo.ui.ex.action.OptionsTopHitProvider;
import consulo.ui.ex.action.PublicMethodBasedOptionDescription;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsDescriptor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

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
    public Collection<BooleanOptionDescription> getOptions(@Nullable ComponentManager project) {
        if (project != null) {
            for (VcsDescriptor descriptor : ProjectLevelVcsManager.getInstance((Project) project).getAllVcss()) {
                if (HgVcs.VCS_ID.equals(descriptor.getId())) {
                    return Collections.unmodifiableCollection(Arrays.asList(
                        option(project,
                            "Mercurial: Check for incoming and outgoing changesets",
                            HgProjectSettings::isCheckIncomingOutgoing,
                            HgProjectSettings::setCheckIncomingOutgoing
                        ),

                        option(project,
                            "Mercurial: Ignore whitespace differences in annotations",
                            HgProjectSettings::isWhitespacesIgnoredInAnnotations,
                            HgProjectSettings::setIgnoreWhitespacesInAnnotations)
                    ));
                }
            }
        }
        return List.of();
    }

    private static BooleanOptionDescription option(ComponentManager project,
                                                   String option,
                                                   Function<HgProjectSettings, Boolean> getter,
                                                   BiConsumer<HgProjectSettings, Boolean> setter) {
        return new PublicMethodBasedOptionDescription<>(
            option,
            "vcs.Mercurial",
            () -> project.getInstance(HgProjectSettings.class),
            getter,
            setter
        );
    }
}
