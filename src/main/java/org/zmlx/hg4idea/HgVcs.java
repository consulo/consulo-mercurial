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
package org.zmlx.hg4idea;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.component.messagebus.MessageBusConnection;
import consulo.configurable.Configurable;
import consulo.disposer.Disposer;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.ide.ServiceManager;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.language.file.FileTypeManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.mercurial.localize.HgLocalize;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.annotate.AnnotationProvider;
import consulo.versionControlSystem.change.ChangeProvider;
import consulo.versionControlSystem.change.CommitExecutor;
import consulo.versionControlSystem.checkin.CheckinEnvironment;
import consulo.versionControlSystem.diff.DiffProvider;
import consulo.versionControlSystem.history.VcsHistoryProvider;
import consulo.versionControlSystem.merge.MergeProvider;
import consulo.versionControlSystem.rollback.RollbackEnvironment;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.versionControlSystem.root.VcsRootDetector;
import consulo.versionControlSystem.update.UpdateEnvironment;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.zmlx.hg4idea.provider.*;
import org.zmlx.hg4idea.provider.annotate.HgAnnotationProvider;
import org.zmlx.hg4idea.provider.commit.HgCheckinEnvironment;
import org.zmlx.hg4idea.provider.commit.HgCloseBranchExecutor;
import org.zmlx.hg4idea.provider.commit.HgCommitAndPushExecutor;
import org.zmlx.hg4idea.provider.commit.HgMQNewExecutor;
import org.zmlx.hg4idea.provider.update.HgUpdateEnvironment;
import org.zmlx.hg4idea.roots.HgIntegrationEnabler;
import org.zmlx.hg4idea.status.HgRemoteStatusUpdater;
import org.zmlx.hg4idea.status.ui.HgWidgetUpdater;
import org.zmlx.hg4idea.util.HgUtil;
import org.zmlx.hg4idea.util.HgVersion;

import javax.swing.event.HyperlinkEvent;
import java.io.File;
import java.util.*;
import java.util.function.Function;

public class HgVcs extends AbstractVcs<CommittedChangeList> {

  @Deprecated
  public static final Class<HgRemoteUpdater> REMOTE_TOPIC = HgRemoteUpdater.class;
  @Deprecated
  public static final Class<HgStatusUpdater> STATUS_TOPIC = HgStatusUpdater.class;
  @Deprecated
  public static final Class<HgWidgetUpdater> INCOMING_OUTGOING_CHECK_TOPIC = HgWidgetUpdater.class;

  private static final Logger LOG = Logger.getInstance(HgVcs.class);

  public static final String VCS_ID = "hg4idea";
  private final static VcsKey ourKey = createKey(VCS_ID);
  private static final int MAX_CONSOLE_OUTPUT_SIZE = 10000;

  private static final String ORIG_FILE_PATTERN = "*.orig";
  @Nullable
  public static final String HGENCODING = System.getenv("HGENCODING");

  private final HgChangeProvider changeProvider;
  private final HgRollbackEnvironment rollbackEnvironment;
  private final HgDiffProvider diffProvider;
  private final HgHistoryProvider historyProvider;
  private final HgCheckinEnvironment checkinEnvironment;
  private final HgAnnotationProvider annotationProvider;
  private final HgUpdateEnvironment updateEnvironment;
  private final HgCachingCommittedChangesProvider committedChangesProvider;
  private MessageBusConnection messageBusConnection;
  @Nonnull
  private final HgGlobalSettings globalSettings;
  @Nonnull
  private final HgProjectSettings projectSettings;
  private final ProjectLevelVcsManager myVcsManager;

  private HgVFSListener myVFSListener;
  private final HgMergeProvider myMergeProvider;
  private HgExecutableValidator myExecutableValidator;
  private final Object myExecutableValidatorLock = new Object();
  private File myPromptHooksExtensionFile;
  private final CommitExecutor myCommitAndPushExecutor;
  private final CommitExecutor myMqNewExecutor;
  private final HgCloseBranchExecutor myCloseBranchExecutor;

  private HgRemoteStatusUpdater myHgRemoteStatusUpdater;

  @Nonnull
  private HgVersion myVersion = HgVersion.NULL;  // version of Hg which this plugin uses.

  public HgVcs(@Nonnull Project project,
               @Nonnull HgGlobalSettings globalSettings,
               @Nonnull HgProjectSettings projectSettings,
               ProjectLevelVcsManager vcsManager) {
    super(project, VCS_ID);
    this.globalSettings = globalSettings;
    this.projectSettings = projectSettings;
    myVcsManager = vcsManager;
    changeProvider = new HgChangeProvider(project, getKeyInstanceMethod());
    rollbackEnvironment = new HgRollbackEnvironment(project);
    diffProvider = new HgDiffProvider(project);
    historyProvider = new HgHistoryProvider(project);
    checkinEnvironment = new HgCheckinEnvironment(project);
    annotationProvider = new HgAnnotationProvider(project);
    updateEnvironment = new HgUpdateEnvironment(project);
    committedChangesProvider = new HgCachingCommittedChangesProvider(project, this);
    myMergeProvider = new HgMergeProvider(myProject);
    myCommitAndPushExecutor = new HgCommitAndPushExecutor(checkinEnvironment);
    myMqNewExecutor = new HgMQNewExecutor(checkinEnvironment);
    myCloseBranchExecutor = new HgCloseBranchExecutor(checkinEnvironment);
  }

  public LocalizeValue getDisplayName() {
    return HgLocalize.hg4ideaMercurial();
  }

  public Configurable getConfigurable() {
    return new HgProjectConfigurable(getProject(), projectSettings);
  }

  @Nonnull
  public HgProjectSettings getProjectSettings() {
    return projectSettings;
  }

  @Override
  public ChangeProvider getChangeProvider() {
    return changeProvider;
  }

  @Nullable
  @Override
  public RollbackEnvironment createRollbackEnvironment() {
    return rollbackEnvironment;
  }

  @Override
  public DiffProvider getDiffProvider() {
    return diffProvider;
  }

  @Override
  public VcsHistoryProvider getVcsHistoryProvider() {
    return historyProvider;
  }

  @Override
  public VcsHistoryProvider getVcsBlockHistoryProvider() {
    return getVcsHistoryProvider();
  }

  @Nullable
  @Override
  public CheckinEnvironment createCheckinEnvironment() {
    return checkinEnvironment;
  }

  @Override
  public AnnotationProvider getAnnotationProvider() {
    return annotationProvider;
  }

  @Override
  public MergeProvider getMergeProvider() {
    return myMergeProvider;
  }

  @Nullable
  @Override
  public UpdateEnvironment createUpdateEnvironment() {
    return updateEnvironment;
  }

  @Override
  public UpdateEnvironment getIntegrateEnvironment() {
    return null;
  }

  @Override
  public boolean fileListenerIsSynchronous() {
    return false;
  }

  @Override
  public CommittedChangesProvider getCommittedChangesProvider() {
    return committedChangesProvider;
  }

  @Override
  public boolean allowsNestedRoots() {
    return true;
  }

  @Nonnull
  @Override
  public <S> List<S> filterUniqueRoots(@Nonnull List<S> in, @Nonnull Function<S, VirtualFile> convertor) {
    Collections.sort(in, Comparator.comparing(convertor, FilePathComparator.getInstance()));

    for (int i = 1; i < in.size(); i++) {
      final S sChild = in.get(i);
      final VirtualFile child = convertor.apply(sChild);
      final VirtualFile childRoot = HgUtil.getHgRootOrNull(myProject, child);
      if (childRoot == null) {
        continue;
      }
      for (int j = i - 1; j >= 0; --j) {
        final S sParent = in.get(j);
        final VirtualFile parent = convertor.apply(sParent);
        // if the parent is an ancestor of the child and that they share common root, the child is removed
        if (VirtualFileUtil.isAncestor(parent, child, false) && VirtualFileUtil.isAncestor(childRoot,
                                                                                   parent,
                                                                                   false)) {
          in.remove(i);
          //noinspection AssignmentToForLoopParameter
          --i;
          break;
        }
      }
    }
    return in;
  }

  @Override
  public RootsConvertor getCustomConvertor() {
    return HgRootsHandler.getInstance(myProject);
  }

  @Override
  public boolean isVersionedDirectory(VirtualFile dir) {
    return HgUtil.getNearestHgRoot(dir) != null;
  }

  @Nullable
  public HgRemoteStatusUpdater getRemoteStatusUpdater() {
    return myHgRemoteStatusUpdater;
  }

  /**
   * @return the prompthooks.py extension used for capturing prompts from Mercurial and requesting IDEA's user about authentication.
   */
  @Nonnull
  public File getPromptHooksExtensionFile() {
    if (myPromptHooksExtensionFile == null || !myPromptHooksExtensionFile.exists()) {
      // check that hooks are available
      myPromptHooksExtensionFile = HgUtil.getTemporaryPythonFile("prompthooks");
      if (myPromptHooksExtensionFile == null || !myPromptHooksExtensionFile.exists()) {
        LOG.error("prompthooks.py Mercurial extension is not found. Please reinstall " + Application.get().getName());
      }
    }
    return myPromptHooksExtensionFile;
  }

  @Override
  public void activate() {
    // validate hg executable on start and update hg version
    checkExecutableAndVersion();

    // updaters and listeners
    myHgRemoteStatusUpdater = new HgRemoteStatusUpdater(this, projectSettings);
    myHgRemoteStatusUpdater.activate();

    messageBusConnection = myProject.getMessageBus().connect();
    myVFSListener = new HgVFSListener(myProject, this);

    // ignore temporary files
    final String ignoredPattern = FileTypeManager.getInstance().getIgnoredFilesList();
    if (!ignoredPattern.contains(ORIG_FILE_PATTERN)) {
      final String newPattern = ignoredPattern + (ignoredPattern.endsWith(";") ? "" : ";") + ORIG_FILE_PATTERN;
      HgUtil.runWriteActionLater(new Runnable() {
        public void run() {
          FileTypeManager.getInstance().setIgnoredFilesList(newPattern);
        }
      });
    }
  }

  private void checkExecutableAndVersion() {
    if (!ApplicationManager.getApplication().isUnitTestMode() && getExecutableValidator().checkExecutableAndNotifyIfNeeded()) {
      checkVersion();
    }
  }

  @Override
  public void deactivate() {
    if (myHgRemoteStatusUpdater != null) {
      myHgRemoteStatusUpdater.deactivate();
      myHgRemoteStatusUpdater = null;
    }
    if (messageBusConnection != null) {
      messageBusConnection.disconnect();
    }

    if (myVFSListener != null) {
      Disposer.dispose(myVFSListener);
      myVFSListener = null;
    }

    super.deactivate();
  }

  @Nullable
  public static HgVcs getInstance(Project project) {
    if (project == null || project.isDisposed()) {
      return null;
    }
    final ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
    if (vcsManager == null) {
      return null;
    }
    return (HgVcs)vcsManager.findVcsByName(VCS_ID);
  }

  @Nonnull
  public HgGlobalSettings getGlobalSettings() {
    return globalSettings;
  }

  public void showMessageInConsole(@Nonnull String message, @Nonnull ConsoleViewContentType contentType) {
    if (message.length() > MAX_CONSOLE_OUTPUT_SIZE) {
      message = message.substring(0, MAX_CONSOLE_OUTPUT_SIZE);
    }
    myVcsManager.addMessageToConsoleWindow(message, contentType);
  }

  public HgExecutableValidator getExecutableValidator() {
    synchronized (myExecutableValidatorLock) {
      if (myExecutableValidator == null) {
        myExecutableValidator = new HgExecutableValidator(myProject, this);
      }
      return myExecutableValidator;
    }
  }

  @Override
  public List<CommitExecutor> getCommitExecutors() {
    return Arrays.asList(myCommitAndPushExecutor, myMqNewExecutor);
  }

  @Nonnull
  public HgCloseBranchExecutor getCloseBranchExecutor() {
    return myCloseBranchExecutor;
  }

  public static VcsKey getKey() {
    return ourKey;
  }

  @Override
  public VcsType getType() {
    return VcsType.distributed;
  }

  @Override
  @RequiredUIAccess
  public void enableIntegration() {
    myProject.getApplication().executeOnPooledThread(() -> {
      Collection<VcsRoot> roots = ServiceManager.getService(myProject, VcsRootDetector.class).detect();
      new HgIntegrationEnabler(HgVcs.this).enable(roots);
    });
  }

  /**
   * Checks Hg version and updates the myVersion variable.
   * In the case of nullable or unsupported version reports the problem.
   */
  public void checkVersion() {
    final String executable = getGlobalSettings().getHgExecutable();
    VcsNotifier vcsNotifier = VcsNotifier.getInstance(myProject);
    final String SETTINGS_LINK = "settings";
    final String UPDATE_LINK = "update";
    NotificationListener linkAdapter = new NotificationListener.Adapter() {
      @Override
      protected void hyperlinkActivated(@Nonnull Notification notification, @Nonnull HyperlinkEvent e) {
        if (SETTINGS_LINK.equals(e.getDescription())) {
          ShowSettingsUtil.getInstance().showSettingsDialog(myProject, getConfigurable().getDisplayName().get());
        }
        else if (UPDATE_LINK.equals(e.getDescription())) {
          Platform.current().openInBrowser("http://mercurial.selenic.com");
        }
      }
    };
    try {
      myVersion = HgVersion.identifyVersion(executable);
      //if version is not supported, but have valid hg executable
      if (!myVersion.isSupported()) {
        LOG.info("Unsupported Hg version: " + myVersion);
        String message =
          String.format("The <a href='" + SETTINGS_LINK + "'>configured</a> version of Hg is not supported: %s.<br/> " + "The minimal supported version is %s. Please <a " +
                          "href='" + UPDATE_LINK + "'>update</a>.", myVersion, HgVersion.MIN);
        vcsNotifier.notifyError("Unsupported Hg version", message, linkAdapter);
      }
      else if (myVersion.hasUnsupportedExtensions()) {
        String unsupportedExtensionsAsString = myVersion.getUnsupportedExtensions().toString();
        LOG.warn("Unsupported Hg extensions: " + unsupportedExtensionsAsString);
        String message =
          String.format("Some hg extensions %s are not found or not supported by your hg version and will be ignored.\n" + "Please, update your hgrc or Mercurial.ini file",
                        unsupportedExtensionsAsString);
        vcsNotifier.notifyWarning("Unsupported Hg version", message);
      }
    }
    catch (Exception e) {
      if (getExecutableValidator().checkExecutableAndNotifyIfNeeded()) {
        //sometimes not hg application has version command, but we couldn't parse an answer as valid hg,
        // so parse(output) throw ParseException, but hg and git executable seems to be valid in this case
        final String reason = (e.getCause() != null ? e.getCause() : e).getMessage();
        String message = HgVcsMessages.message("hg4idea.unable.to.run.hg", executable);
        vcsNotifier.notifyError(message,
                                reason + "<br/> Please check your hg executable path in <a href='" + SETTINGS_LINK + "'> settings </a>",
                                linkAdapter);
      }
    }
  }

  /**
   * @return the version number of Hg, which is used by IDEA. Or {@link HgVersion#NULL} if version info is unavailable.
   */
  @Nonnull
  public HgVersion getVersion() {
    return myVersion;
  }
}
