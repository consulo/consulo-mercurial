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
package org.zmlx.hg4idea;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.ide.ServiceManager;
import consulo.project.Project;
import consulo.versionControlSystem.AbstractVcs;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;
import org.zmlx.hg4idea.util.HgUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Kirill Likhodedov
 */
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class HgRootsHandler implements AbstractVcs.RootsConvertor {

  public HgRootsHandler() {
  }

  public static HgRootsHandler getInstance(Project project) {
    return project.getInstance(HgRootsHandler.class);
  }

  @Nonnull
  @Override
  public List<VirtualFile> convertRoots(@Nonnull List<VirtualFile> original) {
    final Set<VirtualFile> result = new HashSet<>(original.size());
    for (VirtualFile vf : original) {
      final VirtualFile root = convertRoot(vf);
      if (root != null) {
        result.add(root);
      }
    }
    return new ArrayList<>(result);
  }

  @Nullable
  private static VirtualFile convertRoot(@Nullable VirtualFile root) {
    //check only selected root, do not scan all dirs above
    return HgUtil.isHgRoot(root) ? root : null;
  }
}

