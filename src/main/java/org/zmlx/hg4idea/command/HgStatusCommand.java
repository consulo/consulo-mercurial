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
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.VcsNotifier;
import consulo.versionControlSystem.util.ObjectsConvertor;
import consulo.versionControlSystem.util.VcsFileUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.HgChange;
import org.zmlx.hg4idea.HgFile;
import org.zmlx.hg4idea.HgFileStatusEnum;
import org.zmlx.hg4idea.HgRevisionNumber;
import org.zmlx.hg4idea.execution.HgCommandExecutor;
import org.zmlx.hg4idea.execution.HgCommandResult;

import java.io.File;
import java.util.*;

public class HgStatusCommand {

  private static final Logger LOG = Logger.getInstance(HgStatusCommand.class.getName());

  private static final int ITEM_COUNT = 3;
  private static final int STATUS_INDEX = 0;

  @Nonnull
  private final Project myProject;

  private final boolean myIncludeAdded;
  private final boolean myIncludeModified;
  private final boolean myIncludeRemoved;
  private final boolean myIncludeDeleted;
  private final boolean myIncludeUnknown;
  private final boolean myIncludeIgnored;
  private final boolean myIncludeCopySource;
  private boolean myCleanStatus = false; // should be always false, except checking file existence in revision

  @Nullable private final HgRevisionNumber myBaseRevision;
  @Nullable private final HgRevisionNumber myTargetRevision;

  public void cleanFilesOption(boolean clean) {
    myCleanStatus = clean;
  }

  public static class Builder {
    private boolean includeAdded;
    private boolean includeModified;
    private boolean includeRemoved;
    private boolean includeDeleted;
    private boolean includeUnknown;
    private boolean includeIgnored;
    private boolean includeCopySource;

    private HgRevisionNumber baseRevision;
    private HgRevisionNumber targetRevision;

    public Builder(boolean initValue) {
      includeAdded = initValue;
      includeModified = initValue;
      includeRemoved = initValue;
      includeDeleted = initValue;
      includeUnknown = initValue;
      includeIgnored = initValue;
      includeCopySource = initValue;
      baseRevision = null;
      targetRevision = null;
    }

    public Builder removed(boolean val) {
      includeRemoved = val;
      return this;
    }

    public Builder unknown(boolean val) {
      includeUnknown = val;
      return this;
    }

    public Builder ignored(boolean val) {
      includeIgnored = val;
      return this;
    }

    public Builder copySource(boolean val) {
      includeCopySource = val;
      return this;
    }

    public Builder baseRevision(HgRevisionNumber val) {
      baseRevision = val;
      return this;
    }

    public Builder targetRevision(HgRevisionNumber val) {
      targetRevision = val;
      return this;
    }

    public HgStatusCommand build(@Nonnull Project project) {
      return new HgStatusCommand(project, this);
    }

  }

  private HgStatusCommand(@Nonnull Project project, @Nonnull Builder builder) {
    myProject = project;
    myIncludeAdded = builder.includeAdded;
    myIncludeModified = builder.includeModified;
    myIncludeRemoved = builder.includeRemoved;
    myIncludeDeleted = builder.includeDeleted;
    myIncludeUnknown = builder.includeUnknown;
    myIncludeIgnored = builder.includeIgnored;
    myIncludeCopySource = builder.includeCopySource;
    myBaseRevision = builder.baseRevision;
    myTargetRevision = builder.targetRevision;
  }

  public Set<HgChange> executeInCurrentThread(VirtualFile repo) {
    return executeInCurrentThread(repo, null);
  }

  public Set<HgChange> executeInCurrentThread(VirtualFile repo, @Nullable Collection<FilePath> paths) {
    if (repo == null) {
      return Collections.emptySet();
    }

    HgCommandExecutor executor = new HgCommandExecutor(myProject);
    executor.setSilent(true);

    List<String> options = new LinkedList<>();
    if (myIncludeAdded) {
      options.add("--added");
    }
    if (myIncludeModified) {
      options.add("--modified");
    }
    if (myIncludeRemoved) {
      options.add("--removed");
    }
    if (myIncludeDeleted) {
      options.add("--deleted");
    }
    if (myIncludeUnknown) {
      options.add("--unknown");
    }
    if (myIncludeIgnored) {
      options.add("--ignored");
    }
    if (myIncludeCopySource) {
      options.add("--copies");
    }
    if (myCleanStatus) {
      options.add("--clean");
    }
    executor.setOutputAlwaysSuppressed(myCleanStatus || myIncludeUnknown || myIncludeIgnored);
    if (myBaseRevision != null && (!myBaseRevision.getRevision().isEmpty() || !myBaseRevision.getChangeset().isEmpty())) {
      options.add("--rev");
      options.add(StringUtil.isEmptyOrSpaces(myBaseRevision.getChangeset()) ? myBaseRevision.getRevision() : myBaseRevision.getChangeset());
      if (myTargetRevision != null) {
        options.add("--rev");
        options.add(myTargetRevision.getChangeset());
      }
    }

    final Set<HgChange> changes = new HashSet<>();

    if (paths != null) {
      final List<List<String>> chunked = VcsFileUtil.chunkPaths(repo, paths);
      for (List<String> chunk : chunked) {
        List<String> args = new ArrayList<>();
        args.addAll(options);
        args.addAll(chunk);
        HgCommandResult result = executor.executeInCurrentThread(repo, "status", args);
        changes.addAll(parseChangesFromResult(repo, result, args));
      }
    } else {
      HgCommandResult result = executor.executeInCurrentThread(repo, "status", options);
      changes.addAll(parseChangesFromResult(repo, result, options));
    }
    return changes;
  }

  private  Collection<HgChange> parseChangesFromResult(VirtualFile repo, HgCommandResult result, List<String> args) {
    final Set<HgChange> changes = new HashSet<>();
    HgChange previous = null;
    if (result == null) {
      return changes;
    }
    List<String> errors = result.getErrorLines();
    if (!errors.isEmpty()) {
      if (result.getExitValue() != 0 && !myProject.isDisposed()) {
        String title = "Could not execute hg status command ";
        LOG.warn(title + errors.toString());
        VcsNotifier.getInstance(myProject).logInfo(title, errors.toString());
        return changes;
      }
      LOG.warn(errors.toString());
    }
    for (String line : result.getOutputLines()) {
      if (StringUtil.isEmptyOrSpaces(line) || line.length() < ITEM_COUNT) {
        LOG.warn("Unexpected line in status '" + line + '\'');
        continue;
      }
      char statusChar = line.charAt(STATUS_INDEX);
      HgFileStatusEnum status = HgFileStatusEnum.parse(statusChar);
      if (status == null) {
        LOG.error("Unknown status [" + statusChar + "] in line [" + line + "]" + "\n with arguments " + args);
        continue;
      }
      File ioFile = new File(repo.getPath(), line.substring(2));
      if (HgFileStatusEnum.COPY == status && previous != null
        && previous.getStatus() == HgFileStatusEnum.ADDED) {
        previous.setStatus(HgFileStatusEnum.COPY);
        previous.setBeforeFile(new HgFile(repo, ioFile));
        previous = null;
      } else {
        previous = new HgChange(new HgFile(repo, ioFile), status);
        changes.add(previous);
      }
    }
    return changes;
  }

  @Nonnull
  public Collection<VirtualFile> getFiles(@Nonnull VirtualFile repo, @Nullable List<VirtualFile> files) throws VcsException
  {
    Collection<VirtualFile> resultFiles = new HashSet<>();
    Set<HgChange> change = executeInCurrentThread(repo, files != null ? ObjectsConvertor.vf2fp(files) : null);
    for (HgChange hgChange : change) {
      resultFiles.add(hgChange.afterFile().toFilePath().getVirtualFile());
    }
    return resultFiles;
  }
}
