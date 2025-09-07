/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package org.zmlx.hg4idea.repo;

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.util.lang.function.Condition;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.change.ChangesViewManager;
import consulo.versionControlSystem.distributed.repository.RepositoryImpl;
import consulo.versionControlSystem.log.Hash;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.HgNameWithHashInfo;
import org.zmlx.hg4idea.HgStatusUpdater;
import org.zmlx.hg4idea.HgVcs;
import org.zmlx.hg4idea.command.HgBranchesCommand;
import org.zmlx.hg4idea.execution.HgCommandResult;
import org.zmlx.hg4idea.provider.AsyncFilesManagerListener;
import org.zmlx.hg4idea.provider.HgLocalIgnoredHolder;
import org.zmlx.hg4idea.util.HgUtil;

import java.util.*;

public class HgRepositoryImpl extends RepositoryImpl implements HgRepository {

    private static final Logger LOG = Logger.getInstance(HgRepositoryImpl.class);

    @Nonnull
    private HgVcs myVcs;
    @Nonnull
    private final HgRepositoryReader myReader;
    @Nonnull
    private final VirtualFile myHgDir;
    @Nonnull
    private volatile HgRepoInfo myInfo;
    @Nonnull
    private Set<String> myOpenedBranches = Collections.emptySet();

    @Nonnull
    private volatile HgConfig myConfig;
    private boolean myIsFresh = true;
    private final HgLocalIgnoredHolder myLocalIgnoredHolder;


    @SuppressWarnings("ConstantConditions")
    private HgRepositoryImpl(@Nonnull VirtualFile rootDir, @Nonnull HgVcs vcs,
                             @Nonnull Disposable parentDisposable) {
        super(vcs.getProject(), rootDir, parentDisposable);
        myVcs = vcs;
        myHgDir = rootDir.findChild(HgUtil.DOT_HG);
        assert myHgDir != null : ".hg directory wasn't found under " + rootDir.getPresentableUrl();
        myReader = new HgRepositoryReader(vcs, VirtualFileUtil.virtualToIoFile(myHgDir));
        myConfig = HgConfig.getInstance(getProject(), rootDir);
        myLocalIgnoredHolder = new HgLocalIgnoredHolder(this);
        Disposer.register(this, myLocalIgnoredHolder);
        myLocalIgnoredHolder.addUpdateStateListener(new MyIgnoredHolderAsyncListener(getProject()));
        update();
    }

    @Nonnull
    public static HgRepository getInstance(@Nonnull VirtualFile root, @Nonnull Project project,
                                           @Nonnull Disposable parentDisposable) {
        HgVcs vcs = HgVcs.getInstance(project);
        if (vcs == null) {
            throw new IllegalArgumentException("Vcs not found for project " + project);
        }
        HgRepositoryImpl repository = new HgRepositoryImpl(root, vcs, parentDisposable);
        repository.setupUpdater();
        return repository;
    }

    private void setupUpdater() {
        HgRepositoryUpdater updater = new HgRepositoryUpdater(this);
        Disposer.register(this, updater);
        myLocalIgnoredHolder.startRescan();
    }

    @Nonnull
    @Override
    public VirtualFile getHgDir() {
        return myHgDir;
    }

    @Nonnull
    @Override
    public State getState() {
        return myInfo.getState();
    }

    @Nullable
    @Override
    /**
     * Return active bookmark name if exist or heavy branch name otherwise
     */
    public String getCurrentBranchName() {
        String branchOrBookMarkName = getCurrentBookmark();
        if (StringUtil.isEmptyOrSpaces(branchOrBookMarkName)) {
            branchOrBookMarkName = getCurrentBranch();
        }
        return branchOrBookMarkName;
    }

    @Nonnull
    @Override
    public AbstractVcs getVcs() {
        return myVcs;
    }

    @Override
    @Nonnull
    public String getCurrentBranch() {
        return myInfo.getCurrentBranch();
    }

    @Override
    @Nullable
    public String getCurrentRevision() {
        return myInfo.getCurrentRevision();
    }

    @Override
    @Nullable
    public String getTipRevision() {
        return myInfo.getTipRevision();
    }

    @Override
    @Nonnull
    public Map<String, LinkedHashSet<Hash>> getBranches() {
        return myInfo.getBranches();
    }

    @Override
    @Nonnull
    public Set<String> getOpenedBranches() {
        return myOpenedBranches;
    }

    @Nonnull
    @Override
    public Collection<HgNameWithHashInfo> getBookmarks() {
        return myInfo.getBookmarks();
    }

    @Nullable
    @Override
    public String getCurrentBookmark() {
        return myInfo.getCurrentBookmark();
    }

    @Nonnull
    @Override
    public Collection<HgNameWithHashInfo> getTags() {
        return myInfo.getTags();
    }

    @Nonnull
    @Override
    public Collection<HgNameWithHashInfo> getLocalTags() {
        return myInfo.getLocalTags();
    }

    @Nonnull
    @Override
    public HgConfig getRepositoryConfig() {
        return myConfig;
    }

    @Override
    public boolean hasSubrepos() {
        return myInfo.hasSubrepos();
    }

    @Override
    @Nonnull
    public Collection<HgNameWithHashInfo> getSubrepos() {
        return myInfo.getSubrepos();
    }

    @Nonnull
    @Override
    public List<HgNameWithHashInfo> getMQAppliedPatches() {
        return myInfo.getMQApplied();
    }

    @Nonnull
    @Override
    public List<String> getAllPatchNames() {
        return myInfo.getMqPatchNames();
    }

    @Nonnull
    @Override
    public List<String> getUnappliedPatchNames() {
        final List<String> appliedPatches = HgUtil.getNamesWithoutHashes(getMQAppliedPatches());
        return ContainerUtil.filter(getAllPatchNames(), new Condition<String>() {
            @Override
            public boolean value(String s) {
                return !appliedPatches.contains(s);
            }
        });
    }

    @Override
    public boolean isFresh() {
        return myIsFresh;
    }

    @Override
    public void update() {
        HgRepoInfo currentInfo = readRepoInfo();
        // update only if something changed!!!   if update every time - new log will be refreshed every time, too.
        // Then blinking and do not work properly;
        final Project project = getProject();
        if (!project.isDisposed() && !currentInfo.equals(myInfo)) {
            myInfo = currentInfo;
            HgCommandResult branchCommandResult = new HgBranchesCommand(project, getRoot()).collectBranches();
            if (branchCommandResult == null || branchCommandResult.getExitValue() != 0) {
                LOG.warn("Could not collect hg opened branches."); // hg executable is not valid
                myOpenedBranches = myInfo.getBranches().keySet();
            }
            else {
                myOpenedBranches = HgBranchesCommand.collectNames(branchCommandResult);
            }

            Application.get().executeOnPooledThread(() -> {
                if (!project.isDisposed()) {
                    project.getMessageBus().syncPublisher(HgStatusUpdater.class).update(project, getRoot());
                }
            });
        }
    }

    @Nonnull
    @Override
    public String toLogString() {
        return "HgRepository " + getRoot() + " : " + myInfo;
    }

    @Nonnull
    private HgRepoInfo readRepoInfo() {
        myIsFresh = myIsFresh && myReader.isFresh();
        //in GitRepositoryImpl there are temporary state object for reader fields storing! Todo Check;
        return
            new HgRepoInfo(myReader.readCurrentBranch(), myReader.readCurrentRevision(), myReader.readCurrentTipRevision(), myReader.readState(),
                myReader.readBranches(),
                myReader.readBookmarks(), myReader.readCurrentBookmark(), myReader.readTags(), myReader.readLocalTags(),
                myReader.readSubrepos(), myReader.readMQAppliedPatches(), myReader.readMqPatchNames());
    }

    @Override
    public void updateConfig() {
        myConfig = HgConfig.getInstance(getProject(), getRoot());
    }

    @Override
    public HgLocalIgnoredHolder getLocalIgnoredHolder() {
        return myLocalIgnoredHolder;
    }

    private static class MyIgnoredHolderAsyncListener implements AsyncFilesManagerListener {
        @Nonnull
        private final ChangesViewManager myChangesViewManager;

        public MyIgnoredHolderAsyncListener(@Nonnull Project project) {
            myChangesViewManager = project.getInstance(ChangesViewManager.class);
        }

        @Override
        public void updateStarted() {
            myChangesViewManager.scheduleRefresh();
        }

        @Override
        public void updateFinished() {
            myChangesViewManager.scheduleRefresh();
        }
    }
}
