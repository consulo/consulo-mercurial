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
package org.zmlx.hg4idea.execution;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.util.collection.ContainerUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class HgPromptCommandExecutor extends HgCommandExecutor {

  public HgPromptCommandExecutor(@Nonnull Project project) {
    super(project);
  }

  @Override
  @Nullable
  public HgCommandResult executeInCurrentThread(@Nullable final VirtualFile repo,
                                                @Nonnull final String operation,
                                                @Nullable final List<String> arguments) {
    SocketServer promptServer = new SocketServer(new PromptReceiver(myProject, new HgDeleteModifyPromptHandler(myProject)));
    try {
      int promptPort = promptServer.start();
      return super.executeInCurrentThread(repo, operation, prepareArguments(arguments, promptPort));
    }
    catch (IOException e) {
      showError(e);
      LOG.info("IOException during preparing command", e);
      return null;
    }
    finally {
      promptServer.stop();
    }
  }

  private List<String> prepareArguments(List<String> arguments, int port) {
    List<String> cmdArguments = ContainerUtil.newArrayList();
    cmdArguments.add("--config");
    cmdArguments.add("extensions.hg4ideapromptextension=" + myVcs.getPromptHooksExtensionFile().getAbsolutePath());
    cmdArguments.add("--config");
    cmdArguments.add("hg4ideaprompt.port=" + port);

    if (arguments != null && arguments.size() != 0) {
      cmdArguments.addAll(arguments);
    }
    return cmdArguments;
  }

  private static class PromptReceiver extends SocketServer.Protocol {
    @Nonnull
    private final Project myProject;
    @Nullable
    private final HgPromptHandler myHandler;

    public PromptReceiver(@Nonnull Project project, @Nullable HgPromptHandler handler) {
      myProject = project;
      myHandler = handler;
    }

    public boolean handleConnection(Socket socket) throws IOException {
      //noinspection IOResourceOpenedButNotSafelyClosed
      DataInputStream dataInput = new DataInputStream(socket.getInputStream());
      DataOutputStream out = new DataOutputStream(socket.getOutputStream());
      final String message = new String(readDataBlock(dataInput));
      int numOfChoices = dataInput.readInt();
      final HgPromptChoice[] choices = new HgPromptChoice[numOfChoices];
      for (int i = 0; i < numOfChoices; i++) {
        String choice = new String(readDataBlock(dataInput));
        choices[i] = new HgPromptChoice(i, choice);
      }
      int defaultChoiceInt = dataInput.readInt();
      final HgPromptChoice defaultChoice = choices[defaultChoiceInt];
      HgPromptChoice chosen = myHandler != null && myHandler.shouldHandle(message)
        ? myHandler.promptUser(message, choices, defaultChoice)
        : HgPromptChoice.ask(myProject, LocalizeValue.localizeTODO("Mercurial Prompt Message"), message, choices, defaultChoice);
      sendChoiceToHg(out, chosen.getChosenIndex());
      return true;
    }

    private static void sendChoiceToHg(@Nonnull DataOutputStream outStream, int choice) throws IOException {
      if (choice == HgPromptChoice.CLOSED_OPTION) {
        outStream.writeInt(-1);
      }
      else {
        outStream.writeInt(choice);
      }
    }
  }
}
