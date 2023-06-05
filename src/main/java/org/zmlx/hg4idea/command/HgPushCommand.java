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
package org.zmlx.hg4idea.command;

import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import org.zmlx.hg4idea.HgRemoteUpdater;
import org.zmlx.hg4idea.execution.HgCommandResult;
import org.zmlx.hg4idea.execution.HgRemoteCommandExecutor;

import java.util.LinkedList;
import java.util.List;

public class HgPushCommand {

  private final Project myProject;
  private final VirtualFile myRepo;
  private final String myDestination;

  private String myRevision;
  private boolean myForce;
  private String myBranchName;
  private String myBookmarkName;
  private boolean myIsNewBranch;

  public HgPushCommand(Project project, @Nonnull VirtualFile repo, String destination) {
    myProject = project;
    myRepo = repo;
    myDestination = destination;
  }

  public void setRevision(String revision) {
    myRevision = revision;
  }

  public void setForce(boolean force) {
    myForce = force;
  }

  public void setBranchName(String branchName) {
    myBranchName = branchName;
  }

  public void setIsNewBranch(boolean isNewBranch) {
    myIsNewBranch = isNewBranch;
  }

  public void setBookmarkName(String bookmark) {
    myBookmarkName = bookmark;
  }

  public HgCommandResult executeInCurrentThread() {
    final List<String> arguments = new LinkedList<>();
    if (!StringUtil.isEmptyOrSpaces(myRevision)) {
      arguments.add("-r");
      arguments.add(myRevision);
    }
    if (myBranchName != null) {
      arguments.add("-b");
      arguments.add(myBranchName);
    }
    if (myIsNewBranch) {
      arguments.add("--new-branch");
    }
    if (!StringUtil.isEmptyOrSpaces(myBookmarkName)) {
      arguments.add("-B");
      arguments.add(myBookmarkName);
    }
    if (myForce) {
      arguments.add("-f");
    }
    arguments.add(myDestination);

    final HgRemoteCommandExecutor executor = new HgRemoteCommandExecutor(myProject, myDestination);
    executor.setShowOutput(true);
    HgCommandResult result = executor.executeInCurrentThread(myRepo, "push", arguments);
    myProject.getMessageBus().syncPublisher(HgRemoteUpdater.class).update(myProject, null);
    return result;
  }

  public VirtualFile getRepo() {
    return myRepo;
  }
}
