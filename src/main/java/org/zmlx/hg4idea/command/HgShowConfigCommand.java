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
import org.zmlx.hg4idea.execution.HgCommandExecutor;
import org.zmlx.hg4idea.execution.HgCommandResult;

import java.util.*;

import jakarta.annotation.Nullable;

public class HgShowConfigCommand {

  @Nonnull
  private final Project project;

  public HgShowConfigCommand(@Nonnull Project project) {
    this.project = project;
  }

  @Nonnull
  public Map<String, Map<String, String>> execute(@Nullable VirtualFile repo) {
    if (repo == null) {
      return Collections.emptyMap();
    }

    final HgCommandExecutor executor = new HgCommandExecutor(project);
    executor.setSilent(true);
    //force override debug option while initialize hg configs
    HgCommandResult result = executor.executeInCurrentThread(repo, "showconfig", Arrays.asList("--config", "ui.debug=false"));

    if (result == null) {
      return Collections.emptyMap();
    }

    Map<String, Map<String, String>> configMap = new HashMap<>();
    for (String line : result.getOutputLines()) {
      List<String> option = StringUtil.split(line, "=", true, false);
      if (option.size() == 2) {
        String sectionAndName = option.get(0).trim();
        String value = option.get(1).trim();
        int dotIndex = sectionAndName.indexOf('.');

        if (dotIndex > 0) {
          String sectionName = sectionAndName.substring(0, dotIndex);
          String optionName = sectionAndName.substring(dotIndex + 1, sectionAndName.length());
          if (configMap.containsKey(sectionName)) {
            configMap.get(sectionName).put(optionName, value);
          }
          else {
            HashMap<String, String> sectionMap = new HashMap<>();
            sectionMap.put(optionName, value);
            configMap.put(sectionName, sectionMap);
          }
        }
      }
    }
    return configMap;
  }
}
