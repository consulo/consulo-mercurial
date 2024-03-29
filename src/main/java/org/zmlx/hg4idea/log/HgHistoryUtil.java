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
import consulo.versionControlSystem.util.VcsFileUtil;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.*;
import org.zmlx.hg4idea.command.HgLogCommand;
import org.zmlx.hg4idea.execution.HgCommandResult;
import org.zmlx.hg4idea.provider.HgChangeProvider;
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

      HgRevisionNumber vcsRevisionNumber = revision.getRevisionNumber();
      List<HgRevisionNumber> parents = vcsRevisionNumber.getParents();
      HgRevisionNumber firstParent = parents.isEmpty() ? null : parents.get(0); // can have no parents if it is a root
      List<Hash> parentsHash = new SmartList<>();
      for (HgRevisionNumber parent : parents) {
        parentsHash.add(factory.createHash(parent.getChangeset()));
      }

      final Collection<Change> changes = new ArrayList<>();
      for (String file : revision.getModifiedFiles()) {
        changes.add(createChange(project, root, file, firstParent, file, vcsRevisionNumber, FileStatus.MODIFIED));
      }
      for (String file : revision.getAddedFiles()) {
        changes.add(createChange(project, root, null, null, file, vcsRevisionNumber, FileStatus.ADDED));
      }
      for (String file : revision.getDeletedFiles()) {
        changes.add(createChange(project, root, file, firstParent, null, vcsRevisionNumber, FileStatus.DELETED));
      }
      for (Map.Entry<String, String> copiedFile : revision.getMovedFiles().entrySet()) {
        changes.add(createChange(project, root, copiedFile.getKey(), firstParent, copiedFile.getValue(), vcsRevisionNumber,
                                 HgChangeProvider.RENAMED));
      }

      vcsFullCommitDetailsList.add(factory.createFullDetails(factory.createHash(vcsRevisionNumber.getChangeset()), parentsHash,
                                                             revision.getRevisionDate().getTime(), root,
                                                             vcsRevisionNumber.getSubject(),
                                                             vcsRevisionNumber.getName(), vcsRevisionNumber.getEmail(),
                                                             vcsRevisionNumber.getCommitMessage(), vcsRevisionNumber.getName(),
                                                             vcsRevisionNumber.getEmail(), revision.getRevisionDate().getTime(),
                                                             () -> changes
      ));
    }
    return vcsFullCommitDetailsList;
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
