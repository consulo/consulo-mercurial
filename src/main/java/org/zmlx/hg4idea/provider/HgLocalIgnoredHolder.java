/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.zmlx.hg4idea.provider;

import consulo.logging.Logger;
import consulo.proxy.EventDispatcher;
import consulo.ui.ex.awt.util.Alarm;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.util.collection.ContainerUtil;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.ui.ex.awt.util.Update;
import consulo.disposer.Disposable;
import org.zmlx.hg4idea.command.HgStatusCommand;
import org.zmlx.hg4idea.repo.HgRepository;

import jakarta.annotation.Nonnull;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class HgLocalIgnoredHolder implements Disposable {
  private static final Logger LOG = Logger.getInstance(HgLocalIgnoredHolder.class);
  @Nonnull
  private final MergingUpdateQueue myUpdateQueue;
  @Nonnull
  private final AtomicBoolean myInUpdateMode;
  @Nonnull
  private final HgRepository myRepository;
  @Nonnull
  private final Set<VirtualFile> myIgnoredSet;
  @Nonnull
  private final ReentrantReadWriteLock SET_LOCK = new ReentrantReadWriteLock();
  private final EventDispatcher<AsyncFilesManagerListener> myListeners = EventDispatcher.create(AsyncFilesManagerListener.class);

  public HgLocalIgnoredHolder(@Nonnull HgRepository repository) {
    myRepository = repository;
    myIgnoredSet = new HashSet<>();
    myInUpdateMode = new AtomicBoolean(false);
    myUpdateQueue = new MergingUpdateQueue("HgIgnoreUpdate", 500, true, null, this, null, Alarm.ThreadToUse.POOLED_THREAD);
  }

  public void addUpdateStateListener(@Nonnull AsyncFilesManagerListener listener) {
    myListeners.addListener(listener, this);
  }

  public void startRescan() {
    myUpdateQueue.queue(new Update("hgRescanIgnored") {
      @Override
      public boolean canEat(Update update) {
        return true;
      }

      @Override
      public void run() {
        if (myInUpdateMode.compareAndSet(false, true)) {
          fireUpdateStarted();
          rescanAllIgnored();
          myInUpdateMode.set(false);
          fireUpdateFinished();
        }
      }
    });
  }

  private void fireUpdateStarted() {
    myListeners.getMulticaster().updateStarted();
  }

  private void fireUpdateFinished() {
    myListeners.getMulticaster().updateFinished();
  }

  private void rescanAllIgnored() {
    Set<VirtualFile> ignored = new HashSet<>();
    try {
      ignored.addAll(new HgStatusCommand.Builder(false).ignored(true).build(myRepository.getProject())
                       .getFiles(myRepository.getRoot(), null));
    }
    catch (VcsException e) {
      LOG.error("Can't reload ignored files for: " + myRepository.getPresentableUrl(), e);
      return;
    }
    try {
      SET_LOCK.writeLock().lock();
      myIgnoredSet.clear();
      myIgnoredSet.addAll(ignored);
    }
    finally {
      SET_LOCK.writeLock().unlock();
    }
  }

  @Nonnull
  public List<FilePath> removeIgnoredFiles(@Nonnull Collection<FilePath> files) {
    List<FilePath> removedIgnoredFiles = ContainerUtil.newArrayList();
    try {
      SET_LOCK.writeLock().lock();
      Iterator<VirtualFile> iter = myIgnoredSet.iterator();
      while (iter.hasNext()) {
        FilePath filePath = VcsUtil.getFilePath(iter.next());
        if (files.contains(filePath)) {
          iter.remove();
          removedIgnoredFiles.add(filePath);
        }
      }
    }
    finally {
      SET_LOCK.writeLock().unlock();
    }
    return removedIgnoredFiles;
  }

  public void addFiles(@Nonnull List<VirtualFile> files) {
    try {
      SET_LOCK.writeLock().lock();
      myIgnoredSet.addAll(files);
    }
    finally {
      SET_LOCK.writeLock().unlock();
    }
  }

  public boolean contains(@Nonnull VirtualFile file) {
    try {
      SET_LOCK.readLock().lock();
      return myIgnoredSet.contains(file);
    }
    finally {
      SET_LOCK.readLock().unlock();
    }
  }

  public boolean isInUpdateMode() {
    return myInUpdateMode.get();
  }

  @Nonnull
  public Set<VirtualFile> getIgnoredFiles() {
    try {
      SET_LOCK.readLock().lock();
      return new HashSet<>(myIgnoredSet);
    }
    finally {
      SET_LOCK.readLock().unlock();
    }
  }

  @Override
  public void dispose() {
    try {
      myUpdateQueue.cancelAllUpdates();
      SET_LOCK.writeLock().lock();
      myIgnoredSet.clear();
    }
    finally {
      SET_LOCK.writeLock().unlock();
    }
  }

  public int getSize() {
    try {
      SET_LOCK.readLock().lock();
      return myIgnoredSet.size();
    }
    finally {
      SET_LOCK.readLock().unlock();
    }
  }
}
