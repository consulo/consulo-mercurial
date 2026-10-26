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
package org.zmlx.hg4idea.log;

import consulo.ide.ServiceManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.VcsNotifier;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.versionControlSystem.change.CurrentContentRevision;
import consulo.versionControlSystem.log.*;
import consulo.versionControlSystem.log.base.VcsChangesLazilyParsedDetails;
import consulo.versionControlSystem.log.base.VcsFileStatusInfo;
import consulo.versionControlSystem.util.VcsFileUtil;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.*;
import org.zmlx.hg4idea.command.HgLogCommand;
import org.zmlx.hg4idea.command.HgStatusCommand;
import org.zmlx.hg4idea.execution.HgCommandResult;
import org.zmlx.hg4idea.util.HgChangesetUtil;
import org.zmlx.hg4idea.util.HgUtil;
import org.zmlx.hg4idea.util.HgVersion;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class HgHistoryUtil {

  private static final Logger LOG = Logger.getInstance(HgHistoryUtil.class);

  private HgHistoryUtil() {
  }

  @Nonnull
  public static List<VcsCommitMetadata> loadMetadata(@Nonnull final Project project,
                                                     @Nonnull final VirtualFile root, int limit,
                                                     @Nonnull List<String> parameters) throws VcsException {

    final VcsLogObjectsFactory factory = getObjectsFactoryWithDisposeCheck(project);
    if (factory == null) {
      return Collections.emptyList();
    }
    HgVcs hgvcs = HgVcs.getInstance(project);
    assert hgvcs != null;
    HgVersion version = hgvcs.getVersion();
    List<String> templateList = HgBaseLogParser.constructDefaultTemplate(version);
    templateList.add("{desc}");
    String[] templates = ArrayUtil.toStringArray(templateList);
    HgCommandResult result = getLogResult(project, root, version, limit, parameters, HgChangesetUtil.makeTemplate(templates));
    HgBaseLogParser<VcsCommitMetadata> baseParser = new HgBaseLogParser<VcsCommitMetadata>() {

      @Override
      protected VcsCommitMetadata convertDetails(@Nonnull String rev,
                                                 @Nonnull String changeset,
                                                 @Nonnull SmartList<HgRevisionNumber> parents,
                                                 @Nonnull Date revisionDate,
                                                 @Nonnull String author,
                                                 @Nonnull String email,
                                                 @Nonnull List<String> attributes) {
        String message = parseAdditionalStringAttribute(attributes, MESSAGE_INDEX);
        String subject = extractSubject(message);
        List<Hash> parentsHash = new SmartList<>();
        for (HgRevisionNumber parent : parents) {
          parentsHash.add(factory.createHash(parent.getChangeset()));
        }
        return factory.createCommitMetadata(factory.createHash(changeset), parentsHash, revisionDate.getTime(), root,
                                            subject, author, email, message, author, email, revisionDate.getTime());
      }
    };
    return getCommitRecords(project, result, baseParser);
  }

  @Nonnull
  public static List<? extends VcsFullCommitDetails> history(@Nonnull final Project project,
                                                             @Nonnull final VirtualFile root,
                                                             int limit,
                                                             @Nonnull List<String> hashParameters) throws VcsException {
    return history(project, root, limit, hashParameters, false);
  }

  /**
   * <p>Get & parse hg log detailed output with commits, their parents and their changes.
   * For null destination return log command result</p>
   * <p/>
   * <p>Warning: this is method is efficient by speed, but don't query too much, because the whole log output is retrieved at once,
   * and it can occupy too much memory. The estimate is ~600Kb for 1000 commits.</p>
   */
  @Nonnull
  public static List<? extends VcsFullCommitDetails> history(@Nonnull final Project project,
                                                             @Nonnull final VirtualFile root, final int limit,
                                                             @Nonnull List<String> hashParameters, final boolean silent)
    throws VcsException {
    HgVcs hgvcs = HgVcs.getInstance(project);
    assert hgvcs != null;
    final HgVersion version = hgvcs.getVersion();
    final String[] templates = HgBaseLogParser.constructFullTemplateArgument(true, version);

    return VcsFileUtil.foreachChunk(hashParameters, 2,
                                    strings -> {
                                      HgCommandResult logResult =
                                        getLogResult(project, root, version, limit, strings, HgChangesetUtil.makeTemplate(templates));
                                      if (logResult == null) return Collections.emptyList();
                                      if (!logResult.getErrorLines().isEmpty()) throw new VcsException(logResult.getRawError());
                                      return createFullCommitsFromResult(project, root, logResult, version, silent);
                                    });
  }

  public static List<? extends VcsFullCommitDetails> createFullCommitsFromResult(@Nonnull Project project,
                                                                                 @Nonnull VirtualFile root,
                                                                                 @Nullable HgCommandResult result,
                                                                                 @Nonnull HgVersion version, boolean silent) {
    final VcsLogObjectsFactory factory = getObjectsFactoryWithDisposeCheck(project);
    if (factory == null) {
      return Collections.emptyList();
    }
    List<HgFileRevision> hgRevisions =
      getCommitRecords(project, result, new HgFileRevisionLogParser(project, getOriginalHgFile(project, root), version), silent);
    List<VcsFullCommitDetails> vcsFullCommitDetailsList = new ArrayList<>();
    for (HgFileRevision revision : hgRevisions) {
      vcsFullCommitDetailsList.add(createDetails(project, root, factory, revision));
    }
    return vcsFullCommitDetailsList;
  }

  public static VcsFullCommitDetails createDetails(Project project,
                                                   VirtualFile root,
                                                   VcsLogObjectsFactory factory,
                                                   HgFileRevision revision) {
    List<List<VcsFileStatusInfo>> reportedChanges = new ArrayList<>();
    reportedChanges.add(getStatusInfo(revision));

    HgRevisionNumber vcsRevisionNumber = revision.getRevisionNumber();
    List<? extends HgRevisionNumber> parents = vcsRevisionNumber.getParents();
    for (HgRevisionNumber parent : parents.stream().skip(1).toList()) {
      reportedChanges.add(getChangesFromParent(project, root, vcsRevisionNumber, parent));
    }

    Hash hash = factory.createHash(vcsRevisionNumber.getChangeset());
    List<Hash> parentsHashes = ContainerUtil.map(parents, p -> factory.createHash(p.getChangeset()));
    long time = revision.getRevisionDate().getTime();
    VcsUser author = factory.createUser(vcsRevisionNumber.getName(), vcsRevisionNumber.getEmail());
    return new VcsChangesLazilyParsedDetails(project, hash, parentsHashes, time, root, vcsRevisionNumber.getSubject(), author,
                                             vcsRevisionNumber.getCommitMessage(), author,
                                             time, reportedChanges, new HgChangesParser(vcsRevisionNumber));
  }

  private static List<VcsFileStatusInfo> getChangesFromParent(Project project, VirtualFile root,
                                                              HgRevisionNumber commit, HgRevisionNumber parent) {
    HgStatusCommand status = new HgStatusCommand.Builder(true).ignored(false).unknown(false).copySource(true)
      .baseRevision(parent).targetRevision(commit).build(project);
    return convertHgChanges(status.executeInCurrentThread(root));
  }

  private static List<VcsFileStatusInfo> getStatusInfo(HgFileRevision revision) {
    List<VcsFileStatusInfo> firstParentChanges = new ArrayList<>();
    for (String file : revision.getModifiedFiles()) {
      firstParentChanges.add(new VcsFileStatusInfo(Change.Type.MODIFICATION, file, null));
    }
    for (String file : revision.getAddedFiles()) {
      firstParentChanges.add(new VcsFileStatusInfo(Change.Type.NEW, file, null));
    }
    for (String file : revision.getDeletedFiles()) {
      firstParentChanges.add(new VcsFileStatusInfo(Change.Type.DELETED, file, null));
    }
    for (Map.Entry<String, String> copiedFile : revision.getMovedFiles().entrySet()) {
      firstParentChanges.add(new VcsFileStatusInfo(Change.Type.MOVED, copiedFile.getKey(), copiedFile.getValue()));
    }
    return firstParentChanges;
  }

  private static List<VcsFileStatusInfo> convertHgChanges(Set<HgChange> changes) {
    Set<String> deleted = new HashSet<>();
    Set<String> copied = new HashSet<>();

    for (HgChange change : changes) {
      Change.Type type = getType(change.getStatus());
      if (Change.Type.DELETED.equals(type)) {
        deleted.add(change.beforeFile().getRelativePath());
      }
      else if (Change.Type.MOVED.equals(type)) {
        copied.add(change.beforeFile().getRelativePath());
      }
    }

    List<VcsFileStatusInfo> result = new ArrayList<>();
    for (HgChange change : changes) {
      Change.Type type = getType(change.getStatus());
      LOG.assertTrue(type != null, "Unsupported status for change " + change);

      String firstPath;
      String secondPath;
      switch (type) {
        case DELETED -> {
          firstPath = change.beforeFile().getRelativePath();
          secondPath = null;
          if (copied.contains(firstPath)) continue; // file was renamed
        }
        case MOVED -> {
          firstPath = change.beforeFile().getRelativePath();
          secondPath = change.afterFile().getRelativePath();
          if (!deleted.contains(firstPath)) {
            type = Change.Type.NEW; // file was copied, treating it like an addition
            firstPath = change.afterFile().getRelativePath();
            secondPath = null;
          }
        }
        //case MODIFICATION, NEW ->
        default -> {
          firstPath = change.afterFile().getRelativePath();
          secondPath = null;
        }
      }
      result.add(new VcsFileStatusInfo(type, Objects.requireNonNull(firstPath), secondPath));
    }
    return result;
  }

  @Nullable
  private static Change.Type getType(HgFileStatusEnum status) {
    return switch (status) {
      case ADDED -> Change.Type.NEW;
      case MODIFIED -> Change.Type.MODIFICATION;
      case DELETED -> Change.Type.DELETED;
      case COPY -> Change.Type.MOVED;
      case UNVERSIONED, MISSING, UNMODIFIED, IGNORED -> null;
    };
  }


  @Nullable
  public static HgCommandResult getLogResult(@Nonnull final Project project,
                                             @Nonnull final VirtualFile root, @Nonnull HgVersion version, int limit,
                                             @Nonnull List<String> parameters, @Nonnull String template) {
    HgFile originalHgFile = getOriginalHgFile(project, root);
    HgLogCommand hgLogCommand = new HgLogCommand(project);
    List<String> args = new ArrayList<>(parameters);
    hgLogCommand.setLogFile(false);
    if (!version.isParentRevisionTemplateSupported()) {
      args.add("--debug");
    }
    return hgLogCommand.execute(root, template, limit, originalHgFile, args);
  }

  public static HgFile getOriginalHgFile(Project project, VirtualFile root) {
    HgFile hgFile = new HgFile(root, VcsUtil.getFilePath(root.getPath()));
    if (project.isDisposed()) {
      return hgFile;
    }
    FilePath originalFileName = HgUtil.getOriginalFileName(hgFile.toFilePath(), ChangeListManager.getInstance(project));
    return new HgFile(hgFile.getRepo(), originalFileName);
  }

  @Nonnull
  public static <CommitInfo> List<CommitInfo> getCommitRecords(@Nonnull Project project,
                                                               @Nullable HgCommandResult result,
                                                               @Nonnull Function<String, CommitInfo> converter) {
    return getCommitRecords(project, result, converter, false);
  }

  @Nonnull
  public static <CommitInfo> List<CommitInfo> getCommitRecords(@Nonnull Project project,
                                                               @Nullable HgCommandResult result,
                                                               @Nonnull Function<String, CommitInfo> converter, boolean silent) {
    final List<CommitInfo> revisions = new LinkedList<>();
    if (result == null) {
      return revisions;
    }

    List<String> errors = result.getErrorLines();
    if (!errors.isEmpty()) {
      if (result.getExitValue() != 0) {
        if (silent) {
          LOG.debug(errors.toString());
        }
        else {
          VcsNotifier.getInstance(project)
                     .notifyError(HgVcsMessages.message("hg4idea.error.log.command.execution"), errors.toString());
        }
        return Collections.emptyList();
      }
      LOG.warn(errors.toString());
    }
    String output = result.getRawOutput();
    List<String> changeSets = StringUtil.split(output, HgChangesetUtil.CHANGESET_SEPARATOR);
    return ContainerUtil.mapNotNull(changeSets, converter);
  }

  @Nonnull
  public static List<? extends VcsShortCommitDetails> readMiniDetails(@Nonnull final Project project,
                                                                      @Nonnull final VirtualFile root,
                                                                      @Nonnull List<String> hashes)
    throws VcsException {
    final VcsLogObjectsFactory factory = getObjectsFactoryWithDisposeCheck(project);
    if (factory == null) {
      return Collections.emptyList();
    }

    HgVcs hgvcs = HgVcs.getInstance(project);
    assert hgvcs != null;
    final HgVersion version = hgvcs.getVersion();
    List<String> templateList = HgBaseLogParser.constructDefaultTemplate(version);
    templateList.add("{desc}");
    final String[] templates = ArrayUtil.toStringArray(templateList);

    return VcsFileUtil.foreachChunk(prepareHashes(hashes), 2,
                                    strings -> {
                                      HgCommandResult logResult =
                                        getLogResult(project, root, version, -1, strings, HgChangesetUtil.makeTemplate(templates));

                                      return getCommitRecords(project, logResult, new HgBaseLogParser<VcsShortCommitDetails>() {
                                        @Override
                                        protected VcsShortCommitDetails convertDetails(@Nonnull String rev,
                                                                                       @Nonnull String changeset,
                                                                                       @Nonnull SmartList<HgRevisionNumber> parents,
                                                                                       @Nonnull Date revisionDate,
                                                                                       @Nonnull String author,
                                                                                       @Nonnull String email,
                                                                                       @Nonnull List<String> attributes) {
                                          String message = parseAdditionalStringAttribute(attributes, MESSAGE_INDEX);
                                          String subject = extractSubject(message);
                                          List<Hash> parentsHash = new SmartList<>();
                                          for (HgRevisionNumber parent : parents) {
                                            parentsHash.add(factory.createHash(parent.getChangeset()));
                                          }
                                          return factory
                                            .createShortDetails(factory.createHash(changeset), parentsHash, revisionDate.getTime(), root,
                                                                subject, author, email, author, email, revisionDate.getTime());
                                        }
                                      });
                                    });
  }

  @Nonnull
  public static List<TimedVcsCommit> readAllHashes(@Nonnull Project project, @Nonnull VirtualFile root,
                                                   @Nonnull final Consumer<VcsUser> userRegistry, @Nonnull List<String> params)
    throws VcsException {

    final VcsLogObjectsFactory factory = getObjectsFactoryWithDisposeCheck(project);
    if (factory == null) {
      return Collections.emptyList();
    }
    HgVcs hgvcs = HgVcs.getInstance(project);
    assert hgvcs != null;
    HgVersion version = hgvcs.getVersion();
    String[] templates = ArrayUtil.toStringArray(HgBaseLogParser.constructDefaultTemplate(version));
    HgCommandResult result = getLogResult(project, root, version, -1, params, HgChangesetUtil.makeTemplate(templates));
    return getCommitRecords(project, result, new HgBaseLogParser<TimedVcsCommit>() {

      @Override
      protected TimedVcsCommit convertDetails(@Nonnull String rev,
                                              @Nonnull String changeset,
                                              @Nonnull SmartList<HgRevisionNumber> parents,
                                              @Nonnull Date revisionDate,
                                              @Nonnull String author,
                                              @Nonnull String email,
                                              @Nonnull List<String> attributes) {
        List<Hash> parentsHash = new SmartList<>();
        for (HgRevisionNumber parent : parents) {
          parentsHash.add(factory.createHash(parent.getChangeset()));
        }
        userRegistry.accept(factory.createUser(author, email));
        return factory.createTimedCommit(factory.createHash(changeset),
                                         parentsHash, revisionDate.getTime());
      }
    });
  }

  private static VcsLogObjectsFactory getObjectsFactoryWithDisposeCheck(Project project) {
    if (!project.isDisposed()) {
      return ServiceManager.getService(project, VcsLogObjectsFactory.class);
    }
    return null;
  }

  @Nonnull
  public static Change createChange(@Nonnull Project project, @Nonnull VirtualFile root,
                                    @Nullable String fileBefore,
                                    @Nullable HgRevisionNumber revisionBefore,
                                    @Nullable String fileAfter,
                                    HgRevisionNumber revisionAfter,
                                    FileStatus aStatus) {

    HgContentRevision beforeRevision =
      fileBefore == null || aStatus == FileStatus.ADDED ? null
        : HgContentRevision
        .create(project, new HgFile(root, new File(root.getPath(), fileBefore)), revisionBefore);
    ContentRevision afterRevision;
    if (aStatus == FileStatus.DELETED) {
      afterRevision = null;
    }
    else if (revisionAfter == null && fileBefore != null) {
      afterRevision =
        CurrentContentRevision.create(new HgFile(root, new File(root.getPath(), fileAfter != null ? fileAfter : fileBefore)).toFilePath());
    }
    else {
      assert revisionAfter != null;
      afterRevision = fileAfter == null ? null :
        HgContentRevision.create(project, new HgFile(root, new File(root.getPath(), fileAfter)), revisionAfter);
    }
    return new Change(beforeRevision, afterRevision, aStatus);
  }

  @Nonnull
  public static List<String> prepareHashes(@Nonnull List<String> hashes) {
    List<String> hashArgs = new ArrayList<>();
    for (String hash : hashes) {
      hashArgs.add("-r");
      hashArgs.add(hash);
    }
    return hashArgs;
  }

  @Nonnull
  public static Collection<String> getDescendingHeadsOfBranches(@Nonnull Project project, @Nonnull VirtualFile root, @Nonnull Hash hash)
    throws VcsException {
    //hg log -r "descendants(659db54c1b6865c97c4497fa867194bcd759ca76) and head()" --template "{branch}{bookmarks}"
    Set<String> branchHeads = new HashSet<>();
    List<String> params = new ArrayList<>();
    params.add("-r");
    params.add("descendants(" + hash.asString() + ") and head()");
    HgLogCommand hgLogCommand = new HgLogCommand(project);
    hgLogCommand.setLogFile(false);
    String template = HgChangesetUtil.makeTemplate("{branch}", "{bookmarks}");
    HgCommandResult logResult = hgLogCommand.execute(root, template, -1, null, params);
    if (logResult == null || logResult.getExitValue() != 0) {
      throw new VcsException("Couldn't get commit details: log command execution error.");
    }
    String output = logResult.getRawOutput();
    List<String> changeSets = StringUtil.split(output, HgChangesetUtil.CHANGESET_SEPARATOR);
    for (String line : changeSets) {
      List<String> attributes = StringUtil.split(line, HgChangesetUtil.ITEM_SEPARATOR);
      branchHeads.addAll(attributes);
    }
    return branchHeads;
  }

  public static String prepareParameter(String paramName, String value) {
    return "--" + paramName + "=" + value; // no value escaping needed, because the parameter itself will be quoted by GeneralCommandLine
  }
}
