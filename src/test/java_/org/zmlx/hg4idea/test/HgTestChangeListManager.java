/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package org.zmlx.hg4idea.test;

import consulo.application.ApplicationManager;
import consulo.project.Project;
import com.intellij.openapi.vcs.changes.Change;
import consulo.ide.impl.idea.openapi.vcs.changes.ChangeListManagerImpl;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.awt.UIUtil;
import org.junit.Assert;

import java.util.*;

import static org.junit.Assert.assertNotNull;

/**
 * The ChangeListManagerImpl extension with some useful helper methods for tests.
 * @author Kirill Likhodedov
 */
public class HgTestChangeListManager {

  private ChangeListManagerImpl peer;

  public HgTestChangeListManager(Project project) {
    peer = ChangeListManagerImpl.getInstanceImpl(project);
  }

  /**
   * Adds the specified unversioned files to the repository (to the default change list).
   * Shortcut for ChangeListManagerImpl.addUnversionedFiles().
   */
  public void addUnversionedFilesToVcs(VirtualFile... files) {
    peer.addUnversionedFiles(peer.getDefaultChangeList(), Arrays.asList(files));
    ensureUpToDate();
  }

  /**
   * Updates the change list manager and checks that the given files are in the default change list.
   * @param only Set this to true if you want ONLY the specified files to be in the change list.
   *             If set to false, the change list may contain some other files apart from the given ones.
   * @param files Files to be checked.
   */
  public void checkFilesAreInList(boolean only, VirtualFile... files) {
    ensureUpToDate();

    final Collection<Change> changes = peer.getDefaultChangeList().getChanges();
    if (only) {
      Assert.assertEquals(changes.size(), files.length);
    }
    final Collection<VirtualFile> filesInChangeList = new HashSet<>();
    for (Change c : changes) {
      filesInChangeList.add(c.getVirtualFile());
    }
    for (VirtualFile f : files) {
      Assert.assertTrue(filesInChangeList.contains(f));
    }
  }

  /**
   * Commits all changes of the given files.
   */
  public void commitFiles(VirtualFile... files) {
    ensureUpToDate();
    final List<Change> changes = new ArrayList<>(files.length);
    for (VirtualFile f : files) {
      changes.addAll(peer.getChangesIn(f));
    }
    final LocalChangeList list = peer.getDefaultChangeList();
    assertNotNull(list);
    list.setComment("A comment to a commit");
    UIUtil.invokeAndWaitIfNeeded(new Runnable() {
      @Override
      public void run() {
        Assert.assertTrue(peer.commitChangesSynchronouslyWithResult(list, changes));
      }
    });
    ensureUpToDate();
  }

  /**
   * Ensures the ChangelistManager is up to date.
   * It is called after each operation in the HgTestChangeListManager.
   */
  public void ensureUpToDate() {
    if (!ApplicationManager.getApplication().isDispatchThread()) { // for dispatch thread no need to force update.
      peer.ensureUpToDate(false);
    }
  }

}
