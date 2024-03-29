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

import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.ui.ex.awt.util.Update;
import consulo.versionControlSystem.change.VcsDirtyScopeManager;
import consulo.versionControlSystem.distributed.DvcsUtil;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.VFileEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.HgRemoteUpdater;

import java.util.List;

/**
 * Listens to .hg service files changes and updates {@link HgRepository} when needed.
 */
final class HgRepositoryUpdater implements Disposable, BulkFileListener {
  private final Project myProject;
  @Nonnull
  private final HgRepositoryFiles myRepositoryFiles;
  @Nullable
  private final MessageBusConnection myMessageBusConnection;
  @Nonnull
  private final MergingUpdateQueue myUpdateQueue;
  @Nullable
  private final VirtualFile myBranchHeadsDir;
  private static final int TIME_SPAN = 300;
  @Nullable
  private VirtualFile myMqDir;
  @Nullable
  private final LocalFileSystem.WatchRequest myWatchRequest;
  @Nonnull
  private final MergingUpdateQueue myUpdateConfigQueue;
  private final HgRepository myRepository;
  private final VcsDirtyScopeManager myDirtyScopeManager;


  HgRepositoryUpdater(@Nonnull final HgRepository repository) {
    myRepository = repository;
    VirtualFile hgDir = myRepository.getHgDir();
    myWatchRequest = LocalFileSystem.getInstance().addRootToWatch(hgDir.getPath(), true);
    myRepositoryFiles = HgRepositoryFiles.getInstance(hgDir);
    DvcsUtil.visitVcsDirVfs(hgDir, HgRepositoryFiles.getSubDirRelativePaths());

    myBranchHeadsDir = VcsUtil.getVirtualFile(myRepositoryFiles.getBranchHeadsDirPath());
    myMqDir = VcsUtil.getVirtualFile(myRepositoryFiles.getMQDirPath());

    myProject = repository.getProject();
    myDirtyScopeManager = VcsDirtyScopeManager.getInstance(myProject);
    myUpdateQueue = new MergingUpdateQueue("HgRepositoryUpdate", TIME_SPAN, true, null, this, null, Alarm.ThreadToUse.POOLED_THREAD);
    myUpdateConfigQueue = new MergingUpdateQueue("HgConfigUpdate", TIME_SPAN, true, null, this, null, Alarm.ThreadToUse.POOLED_THREAD);
    if (!myProject.isDisposed()) {
      myMessageBusConnection = myProject.getMessageBus().connect();
      myMessageBusConnection.subscribe(BulkFileListener.class, this);
    }
    else {
      myMessageBusConnection = null;
    }
  }

  @Override
  public void dispose() {
    if (myWatchRequest != null) {
      LocalFileSystem.getInstance().removeWatchedRoot(myWatchRequest);
    }
    myUpdateQueue.cancelAllUpdates();
    myUpdateConfigQueue.cancelAllUpdates();
    if (myMessageBusConnection != null) {
      myMessageBusConnection.disconnect();
    }
  }

  @Override
  public void before(@Nonnull List<? extends VFileEvent> events) {
    // everything is handled in #after()
  }

  @Override
  public void after(@Nonnull List<? extends VFileEvent> events) {
    // which files in .hg were changed
    boolean branchHeadsChanged = false;
    boolean branchFileChanged = false;
    boolean dirstateFileChanged = false;
    boolean mergeFileChanged = false;
    boolean rebaseFileChanged = false;
    boolean bookmarksFileChanged = false;
    boolean tagsFileChanged = false;
    boolean localTagsFileChanged = false;
    boolean currentBookmarkFileChanged = false;
    boolean mqChanged = false;
    boolean hgIgnoreChanged = false;

    boolean configHgrcChanged = false;
    for (VFileEvent event : events) {
      String filePath = event.getPath();
      if (filePath == null) {
        continue;
      }
      if (myRepositoryFiles.isbranchHeadsFile(filePath)) {
        branchHeadsChanged = true;
      }
      else if (myRepositoryFiles.isBranchFile(filePath)) {
        branchFileChanged = true;
        DvcsUtil.ensureAllChildrenInVfs(myBranchHeadsDir);
      }
      else if (myRepositoryFiles.isDirstateFile(filePath)) {
        dirstateFileChanged = true;
      }
      else if (myRepositoryFiles.isMergeFile(filePath)) {
        mergeFileChanged = true;
      }
      else if (myRepositoryFiles.isRebaseFile(filePath)) {
        rebaseFileChanged = true;
      }
      else if (myRepositoryFiles.isBookmarksFile(filePath)) {
        bookmarksFileChanged = true;
      }
      else if (myRepositoryFiles.isTagsFile(filePath)) {
        tagsFileChanged = true;
      }
      else if (myRepositoryFiles.isLocalTagsFile(filePath)) {
        localTagsFileChanged = true;
      }
      else if (myRepositoryFiles.isCurrentBookmarksFile(filePath)) {
        currentBookmarkFileChanged = true;
      }
      else if (myRepositoryFiles.isMqFile(filePath)) {
        mqChanged = true;
        if (myMqDir == null) {
          myMqDir = VcsUtil.getVirtualFile(myRepositoryFiles.getMQDirPath());
        }
        DvcsUtil.ensureAllChildrenInVfs(myMqDir);
      }
      else if (myRepositoryFiles.isConfigHgrcFile(filePath)) {
        configHgrcChanged = true;
      }
      else if (myRepositoryFiles.isHgIgnore(filePath)) {
        hgIgnoreChanged = true;
      }
    }

    if (branchHeadsChanged || branchFileChanged || dirstateFileChanged || mergeFileChanged || rebaseFileChanged ||
      bookmarksFileChanged || currentBookmarkFileChanged || tagsFileChanged || localTagsFileChanged ||
      mqChanged) {
      myUpdateQueue.queue(new MyUpdater("hgrepositoryUpdate"));
    }
    if (configHgrcChanged) {
      myUpdateConfigQueue.queue(new MyUpdater("hgconfigUpdate"));
    }
    if (dirstateFileChanged || hgIgnoreChanged) {
      myRepository.getLocalIgnoredHolder().startRescan();
      final VirtualFile root = myRepository.getRoot();
      myDirtyScopeManager.dirDirtyRecursively(root);
      if (dirstateFileChanged) {
        //update async incoming/outgoing model
        myProject.getMessageBus().syncPublisher(HgRemoteUpdater.class).update(myProject, root);
      }
    }
  }

  private class MyUpdater extends Update {
    public MyUpdater(String name) {
      super(name);
    }

    @Override
    public boolean canEat(Update update) {
      return true;
    }

    @Override
    public void run() {
      myRepository.update();
    }
  }
}
