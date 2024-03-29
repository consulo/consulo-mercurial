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
package org.zmlx.hg4idea;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.ide.ServiceManager;
import consulo.versionControlSystem.distributed.DvcsRememberedInputs;
import jakarta.inject.Singleton;

/**
 * @author Nadya Zabrodina
 */
@State(
  name = "HgRememberedInputs",
  storages = @Storage(file = StoragePathMacros.APP_CONFIG + "/vcs.xml")
)
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@Singleton
public class HgRememberedInputs extends DvcsRememberedInputs implements PersistentStateComponent<DvcsRememberedInputs.State> {
  public static DvcsRememberedInputs getInstance() {
    return ServiceManager.getService(HgRememberedInputs.class);
  }
}
