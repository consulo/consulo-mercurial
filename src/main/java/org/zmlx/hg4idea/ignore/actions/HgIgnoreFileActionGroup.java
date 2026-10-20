// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.zmlx.hg4idea.ignore.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.versionControlSystem.change.ignore.action.IgnoreFileActionGroup;
import org.zmlx.hg4idea.ignore.lang.HgIgnoreFileType;

@ActionImpl(
    id = "Hg.Ignore.File",
    parents = {
        @ActionParentRef(
            value = @ActionRef(id = "ChangesViewPopupMenu"),
            anchor = ActionRefAnchor.AFTER,
            relatedToAction = @ActionRef(id = "ChangesView.AddUnversioned")
        ),
        @ActionParentRef(
            value = @ActionRef(id = "hg4idea.file.menu"),
            anchor = ActionRefAnchor.AFTER,
            relatedToAction = @ActionRef(id = "ChangesView.AddUnversioned")
        )
    }
)
public class HgIgnoreFileActionGroup extends IgnoreFileActionGroup {
    public HgIgnoreFileActionGroup() {
        super(HgIgnoreFileType.INSTANCE);
    }
}
