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

import consulo.logging.Logger;
import consulo.project.Project;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.zmlx.hg4idea.HgFile;
import org.zmlx.hg4idea.HgFileRevision;
import org.zmlx.hg4idea.HgVcs;
import org.zmlx.hg4idea.execution.HgCommandException;
import org.zmlx.hg4idea.execution.HgCommandExecutor;
import org.zmlx.hg4idea.execution.HgCommandResult;
import org.zmlx.hg4idea.log.HgBaseLogParser;
import org.zmlx.hg4idea.log.HgFileRevisionLogParser;
import org.zmlx.hg4idea.log.HgHistoryUtil;
import org.zmlx.hg4idea.util.HgChangesetUtil;
import org.zmlx.hg4idea.util.HgUtil;
import org.zmlx.hg4idea.util.HgVersion;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class HgLogCommand {

  private static final Logger LOG = Logger.getInstance(HgLogCommand.class.getName());

  @Nonnull
  private final Project myProject;
  @Nonnull
  private HgVersion myVersion;
  private boolean myIncludeRemoved;
  private boolean myFollowCopies;
  private boolean myLogFile = true;
  private boolean myLargeFilesWithFollowSupported = false;

  public void setIncludeRemoved(boolean includeRemoved) {
    myIncludeRemoved = includeRemoved;
  }

  public void setFollowCopies(boolean followCopies) {
    myFollowCopies = followCopies;
  }

  public void setLogFile(boolean logFile) {
    myLogFile = logFile;
  }

  public HgLogCommand(@Nonnull Project project) {
    myProject = project;
    HgVcs vcs = HgVcs.getInstance(myProject);
    if (vcs == null) {
      LOG.info("Vcs couldn't be null for project");
      return;
    }
    myVersion = vcs.getVersion();
    myLargeFilesWithFollowSupported = myVersion.isLargeFilesWithFollowSupported();
  }

  /**
   * @param limit Pass -1 to set no limits on history
   */
  public final List<HgFileRevision> execute(final HgFile hgFile, int limit, boolean includeFiles) throws HgCommandException {
    return execute(hgFile, limit, includeFiles, null);
  }

  @Nonnull
  public HgVersion getVersion() {
    return myVersion;
  }

  /**
   * @param limit Pass -1 to set no limits on history
   */
  public final List<HgFileRevision> execute(final HgFile hgFile, int limit, boolean includeFiles, @Nullable List<String> argsForCmd) {
    if ((limit <= 0 && limit != -1) || hgFile == null) {
      return Collections.emptyList();
    }

    String[] templates = HgBaseLogParser.constructFullTemplateArgument(includeFiles, myVersion);
    String template = HgChangesetUtil.makeTemplate(templates);
    FilePath originalFileName = HgUtil.getOriginalFileName(hgFile.toFilePath(), ChangeListManager.getInstance(myProject));
    HgFile originalHgFile = new HgFile(hgFile.getRepo(), originalFileName);
    HgCommandResult result = execute(hgFile.getRepo(), template, limit, originalHgFile, argsForCmd);

    return  HgHistoryUtil.getCommitRecords(myProject, result,
                                           new HgFileRevisionLogParser(myProject, originalHgFile, myVersion));
  }

  @Nullable
  public HgCommandResult execute(@Nonnull VirtualFile repo, @Nonnull String template, int limit, @Nullable HgFile hgFile,
								 @Nullable List<String> argsForCmd) {
    List<String> arguments = new LinkedList<>();
    if (myIncludeRemoved) {
      // There is a bug in mercurial that causes --follow --removed <file> to cause
      // an error (http://mercurial.selenic.com/bts/issue2139). Avoid this combination
      // for now, preferring to use --follow over --removed.
      if (!(myFollowCopies && myLogFile)) {
        arguments.add("--removed");
      }
    }
    if (myFollowCopies) {
      arguments.add("--follow");
      //workaround: --follow  options doesn't work with largefiles extension, so we need to switch off this extension in log command
      //see http://selenic.com/pipermail/mercurial-devel/2013-May/051209.html  fixed since 2.7
      if (!myLargeFilesWithFollowSupported) {
        arguments.add("--config");
        arguments.add("extensions.largefiles=!");
      }
    }
    arguments.add("--template");
    arguments.add(template);
    if (limit != -1) {
      arguments.add("--limit");
      arguments.add(String.valueOf(limit));
    }
    if (argsForCmd != null) {
      arguments.addAll(argsForCmd);
    }  //to do  double check the same condition should be simplified

    if (myLogFile && hgFile != null) {
      arguments.add(hgFile.getRelativePath());
    }
    HgCommandExecutor commandExecutor = new HgCommandExecutor(myProject);
    commandExecutor.setOutputAlwaysSuppressed(true);
    return commandExecutor.executeInCurrentThread(repo, "log", arguments);
  }

}
