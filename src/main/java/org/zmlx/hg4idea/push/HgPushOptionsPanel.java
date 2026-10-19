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
package org.zmlx.hg4idea.push;

import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.distributed.push.VcsPushOptionsPanel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class HgPushOptionsPanel extends VcsPushOptionsPanel {
    @Nonnull
    private final CheckBox myPushBookmarkCheckBox;

    @RequiredUIAccess
    public HgPushOptionsPanel() {
        myPushBookmarkCheckBox = CheckBox.create(LocalizeValue.localizeTODO("Export Active Bookmarks"));
    }

    @Nonnull
    @Override
    public Component getComponent() {
        return myPushBookmarkCheckBox;
    }

    @Override
    @Nullable
    public HgVcsPushOptionValue getValue() {
        return myPushBookmarkCheckBox.getValueOrError() ? HgVcsPushOptionValue.Current : null;
    }
}
