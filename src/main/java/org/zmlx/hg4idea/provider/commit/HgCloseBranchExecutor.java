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
package org.zmlx.hg4idea.provider.commit;

import consulo.localize.LocalizeValue;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.change.CommitContext;
import consulo.versionControlSystem.change.CommitExecutorBase;
import consulo.versionControlSystem.change.CommitSession;
import jakarta.annotation.Nonnull;
import org.zmlx.hg4idea.repo.HgRepository;

import java.util.Collection;
import java.util.List;

public class HgCloseBranchExecutor extends CommitExecutorBase {
    private static final Key<Boolean> IS_CLOSE_BRANCH_KEY = Key.create("Hg.Commit.IsCloseBranch");
    private static final Key<Collection<HgRepository>> COMMIT_WITHOUT_CHANGES_REPOSITORIES_KEY =
        Key.create("Hg.Commit.CommitWithoutChangesRepositories");

    @Nonnull
    private Collection<HgRepository> myRepositories = List.of();

    static boolean isCloseBranch(CommitContext commitContext) {
        return commitContext.getExecutorProperty(IS_CLOSE_BRANCH_KEY, Boolean.FALSE);
    }

    @Nonnull
    static Collection<HgRepository> getCommitWithoutChangesRepositories(CommitContext commitContext) {
        return commitContext.getExecutorProperty(COMMIT_WITHOUT_CHANGES_REPOSITORIES_KEY, List.of());
    }

    @Override
    public boolean areChangesRequired() {
        return false;
    }

    @Nonnull
    @Override
    public LocalizeValue getActionText() {
        return LocalizeValue.localizeTODO("Commit And Close");
    }

    @Nonnull
    @Override
    public CommitSession createCommitSession(@Nonnull CommitContext commitContext) {
        commitContext.putExecutorProperty(IS_CLOSE_BRANCH_KEY, Boolean.TRUE);
        commitContext.putExecutorProperty(COMMIT_WITHOUT_CHANGES_REPOSITORIES_KEY, myRepositories);
        return CommitSession.VCS_COMMIT;
    }

    public void setRepositories(@Nonnull Collection<HgRepository> repositories) {
        myRepositories = List.copyOf(repositories);
    }
}
