// Copyright 2008-2010 Victor Iacoban
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distributed under
// the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
// either express or implied. See the License for the specific language governing permissions and
// limitations under the License.
package org.zmlx.hg4idea.test;

import consulo.versionControlSystem.VcsConfiguration;
import consulo.virtualFileSystem.VirtualFile;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.zmlx.hg4idea.test.HgTestOutputParser.added;

/**
 * Tests adding files to the Mercurial repository.
 */
public abstract class HgAddTest extends HgSingleUserTest {

  @Before
  @Override
  protected void setUp(Method testMethod) throws Exception {
    super.setUp(testMethod);
    doNothingSilently(VcsConfiguration.StandardConfirmation.ADD);
  }

  /**
   * 1. Create a file in the file system.
   * 2. Add the unversioned file to the VCS using the ChangeListManager.
   * 3. Verify that the file was added to the VCS.
   */
  @Test
  public void fileAddedViaChangeListShouldBeAddedToHg() throws Exception {
    final VirtualFile vf = createFileInCommand(AFILE, INITIAL_FILE_CONTENT);
    myChangeListManager.addUnversionedFilesToVcs(vf);
    verifyStatus(added(AFILE));
    myChangeListManager.checkFilesAreInList(true, vf);
  }

  /**
   * 1. Create a file in the file system.
   * 2. Add the file to the VCS using the native command.
   * 4. Verify that the file is in the default change list.
   */
  @Test
  public void fileAddedViaHgShouldBeAddedInChangeList() throws Exception {
    final VirtualFile vf = createFileInCommand(AFILE, INITIAL_FILE_CONTENT);
    myRepo.add();
    myChangeListManager.checkFilesAreInList(true, vf);
  }

  /**
   * 1. Create some files and directories in the file system.
   * 2. Add them to the VCS via the ChangeListManager.
   * 3. Verify that they are added to the VCS.
   */
  @Test
  public void filesInDirsAddedViaChangeListShouldBeAddedToHg() throws Exception {
    final VirtualFile afile = createFileInCommand(AFILE, INITIAL_FILE_CONTENT);
    final VirtualFile bdir = createDirInCommand(myWorkingCopyDir, BDIR);
    final VirtualFile bfile = createFileInCommand(bdir, BFILE, INITIAL_FILE_CONTENT);
    myChangeListManager.addUnversionedFilesToVcs(afile, bdir, bfile);
    verifyStatus(added(AFILE), added(BFILE_PATH));
    myChangeListManager.checkFilesAreInList(true, afile, bfile);
  }

  /**
   * 1. Create some files and directories in the file system.
   * 2. Add them to the VCS natively.
   * 3. Check that they were added to the default change list.
   */
  @Test
  public void filesInDirsAddedViaHgShouldBeAddedInChangeList() throws Exception {
    final VirtualFile afile = createFileInCommand(AFILE, INITIAL_FILE_CONTENT);
    final VirtualFile bdir = createDirInCommand(myWorkingCopyDir, BDIR);
    final VirtualFile bfile = createFileInCommand(bdir, BFILE, INITIAL_FILE_CONTENT);
    myRepo.add();
    verifyStatus(added(AFILE), added(BFILE_PATH));
    myChangeListManager.checkFilesAreInList(true, afile, bfile);
  }

}
