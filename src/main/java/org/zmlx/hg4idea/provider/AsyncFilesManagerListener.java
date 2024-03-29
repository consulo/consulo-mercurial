/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.zmlx.hg4idea.provider;

import consulo.versionControlSystem.change.ChangeListManager;

import java.util.EventListener;

/**
 * <p>
 * The listener interface for handling async {@link ChangeListManager} node models updating.
 * </p>
 * <p>
 * When model starts/finishes to update itself -> updateStarted/Finished will be called for all subscribers.<br/>
 * e.g. vcs updates its ignored file model after that we should trigger ui update in ChangesView;<br/>
 * moreover, when long duration update starts we should also trigger some action;
 * </p>
 */
public interface AsyncFilesManagerListener extends EventListener
{

	void updateStarted();

	void updateFinished();
}