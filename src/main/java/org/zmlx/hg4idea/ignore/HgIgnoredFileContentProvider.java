// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.zmlx.hg4idea.ignore;

import consulo.annotation.component.ExtensionImpl;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.VcsKey;
import consulo.versionControlSystem.action.VcsContextFactory;
import consulo.versionControlSystem.change.IgnoredFileContentProvider;
import consulo.versionControlSystem.change.IgnoredFileDescriptor;
import consulo.versionControlSystem.change.IgnoredFileProvider;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;
import org.zmlx.hg4idea.HgVcs;
import org.zmlx.hg4idea.command.HgStatusCommand;
import org.zmlx.hg4idea.repo.HgRepositoryFiles;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@ExtensionImpl
public class HgIgnoredFileContentProvider implements IgnoredFileContentProvider {
    private static final Logger LOG = Logger.getInstance(HgIgnoredFileContentProvider.class);

    private final Project myProject;

    @Inject
    public HgIgnoredFileContentProvider(Project project) {
        myProject = project;
    }

    @Override
    public VcsKey getSupportedVcs() {
        return HgVcs.getKey();
    }

    @Override
    public String getFileName() {
        return HgRepositoryFiles.HGIGNORE;
    }

    @Override
    public String buildIgnoreFileContent(VirtualFile ignoreFileRoot, IgnoredFileProvider[] ignoredFileProviders) {
        VirtualFile hgRepoRoot = VcsUtil.getVcsRootFor(myProject, ignoreFileRoot);
        if (hgRepoRoot == null || !hgRepoRoot.equals(ignoreFileRoot)) {
            return ""; //generate .hgignore only in hg root
        }

        StringBuilder content = new StringBuilder();
        String lineSeparator = System.lineSeparator();
        Set<VirtualFile> untrackedFiles = getUntrackedFiles(hgRepoRoot, ignoreFileRoot);

        if (untrackedFiles.isEmpty()) {
            return ""; //if there is no untracked files this mean nothing to ignore
        }

        for (IgnoredFileProvider provider : ignoredFileProviders) {
            List<String> ignoredFiles = ignoreBeansToRelativePaths(provider.getIgnoredFiles(), ignoreFileRoot, untrackedFiles);

            if (ignoredFiles.isEmpty()) {
                continue;
            }

            if (!content.isEmpty()) {
                content.append(lineSeparator).append(lineSeparator);
            }

            String description = provider.getIgnoredGroupDescription();
            if (!description.isBlank()) {
                content.append(buildIgnoreGroupDescription(provider));
                content.append(lineSeparator);
            }
            content.append(String.join(lineSeparator, ignoredFiles));
        }
        return !content.isEmpty() ? "syntax: glob" + lineSeparator + lineSeparator + content : "";
    }

    private Set<VirtualFile> getUntrackedFiles(VirtualFile hgRepoRoot, VirtualFile ignoreFileRoot) {
        try {
            Set<VirtualFile> result = new HashSet<>();
            for (VirtualFile file : new HgStatusCommand.Builder(false).unknown(true).removed(true).build(myProject).getFiles(hgRepoRoot, null)) {
                if (VirtualFileUtil.isAncestor(ignoreFileRoot, file, true)) {
                    result.add(file);
                }
            }
            return result;
        }
        catch (VcsException e) {
            LOG.warn("Cannot get untracked files: ", e);
            return Collections.emptySet();
        }
    }

    private List<String> ignoreBeansToRelativePaths(
        Iterable<IgnoredFileDescriptor> iterable,
        VirtualFile ignoreFileRoot,
        Set<VirtualFile> untrackedFiles
    ) {
        VirtualFile vcsRoot = VcsUtil.getVcsRootFor(myProject, ignoreFileRoot);
        VcsContextFactory vcsContextFactory = VcsContextFactory.getInstance();
        List<String> result = new ArrayList<>();
        for (IgnoredFileDescriptor ignoredBean : iterable) {
            boolean shouldIgnore = switch (ignoredBean.getType()) {
                case UNDER_DIR -> shouldIgnoreUnderDir(ignoredBean, untrackedFiles, ignoreFileRoot, vcsRoot, vcsContextFactory);
                case FILE -> shouldIgnoreFile(ignoredBean, untrackedFiles, ignoreFileRoot, vcsRoot, vcsContextFactory);
                case MASK -> shouldIgnoreByMask(ignoredBean, untrackedFiles);
            };
            if (!shouldIgnore) {
                continue;
            }

            result.add(switch (ignoredBean.getType()) {
                case MASK -> Objects.requireNonNull(ignoredBean.getMask());
                case UNDER_DIR, FILE -> buildIgnoreEntryContent(ignoreFileRoot, ignoredBean);
            });
        }
        return result;
    }

    private boolean shouldIgnoreUnderDir(
        IgnoredFileDescriptor ignoredBean,
        Set<VirtualFile> untrackedFiles,
        VirtualFile ignoreFileRoot,
        @Nullable VirtualFile vcsRoot,
        VcsContextFactory vcsContextFactory
    ) {
        String path = ignoredBean.getPath();
        return path != null
            && Files.exists(Paths.get(path))
            && untrackedFiles.stream().anyMatch(it -> FileUtil.isAncestor(path, it.getPath(), true))
            && FileUtil.isAncestor(ignoreFileRoot.getPath(), path, false)
            && Objects.equals(vcsRoot, VcsUtil.getVcsRootFor(myProject, vcsContextFactory.createFilePath(path, true)));
    }

    private boolean shouldIgnoreFile(
        IgnoredFileDescriptor ignoredBean,
        Set<VirtualFile> untrackedFiles,
        VirtualFile ignoreFileRoot,
        @Nullable VirtualFile vcsRoot,
        VcsContextFactory vcsContextFactory
    ) {
        String path = ignoredBean.getPath();
        return path != null
            && Files.exists(Paths.get(path))
            && untrackedFiles.stream().anyMatch(it -> ignoredBean.matchesFile(VcsUtil.getFilePath(it)))
            && FileUtil.isAncestor(ignoreFileRoot.getPath(), path, false)
            && Objects.equals(vcsRoot, VcsUtil.getVcsRootFor(myProject, vcsContextFactory.createFilePath(path, false)));
    }

    private static boolean shouldIgnoreByMask(IgnoredFileDescriptor ignoredBean, Set<VirtualFile> untrackedFiles) {
        return untrackedFiles.stream().anyMatch(it -> ignoredBean.matchesFile(VcsUtil.getFilePath(it)));
    }

    @Override
    public String buildUnignoreContent(String ignorePattern) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String buildIgnoreGroupDescription(IgnoredFileProvider ignoredFileProvider) {
        return prependCommentHashCharacterIfNeeded(ignoredFileProvider.getIgnoredGroupDescription());
    }

    @Override
    public String buildIgnoreEntryContent(VirtualFile ignoreEntryRoot, IgnoredFileDescriptor ignoredFileDescriptor) {
        String relativePath = FileUtil.getRelativePath(ignoreEntryRoot.getPath(), Objects.requireNonNull(ignoredFileDescriptor.getPath()), '/');
        return relativePath != null ? relativePath : "";
    }

    @Override
    public boolean supportIgnoreFileNotInVcsRoot() {
        return false;
    }

    private static String prependCommentHashCharacterIfNeeded(String description) {
        return description.startsWith("#") ? description : "# " + description;
    }
}
