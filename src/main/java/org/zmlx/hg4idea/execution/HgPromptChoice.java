/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package org.zmlx.hg4idea.execution;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.InputBoxBuilder;
import consulo.versionControlSystem.distributed.DvcsUtil;

import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;


public class HgPromptChoice {
  private final String fullString;
  private final String representation;
  private final int chosenIndex;
  public static final int CLOSED_OPTION = -1;
  public static final HgPromptChoice ABORT = new HgPromptChoice(-1, "AbortChoice");

  public int getChosenIndex() {
    return chosenIndex;
  }


  HgPromptChoice(int chosenIndex, String fullString) {
    this.fullString = fullString;
    this.representation = fullString.replaceAll("&", "");
    this.chosenIndex = chosenIndex;
  }

  /**
   * Asks from a background thread which choice to take; a dismissed box answers {@link #ABORT}.
   */
  public static HgPromptChoice ask(Project project,
                                   LocalizeValue title,
                                   String message,
                                   HgPromptChoice[] choices,
                                   HgPromptChoice defaultChoice) {
    try {
      return DvcsUtil.showAndWait(project, () -> InputBoxBuilder.items(Arrays.asList(choices))
        .asQuestion()
        .title(title)
        .text(LocalizeValue.of(message))
        .value(defaultChoice)
        .setupComponent(comboBox -> comboBox.setTextRenderer(
          choice -> choice == null ? LocalizeValue.empty() : LocalizeValue.of(choice.toString())
        ))
        .showAsync(project));
    }
    catch (CompletionException | CancellationException e) {
      return ABORT;
    }
  }

  @Override
  public String toString() {
    return representation;
  }

  @Override
  public boolean equals(Object o) {
    if (null == o) return true;
    if (getClass() != o.getClass()) return false;

    HgPromptChoice choice = (HgPromptChoice)o;

    if (!fullString.equals(choice.fullString)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return fullString.hashCode();
  }
}