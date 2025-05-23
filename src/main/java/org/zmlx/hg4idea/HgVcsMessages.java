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

import consulo.annotation.DeprecationInfo;
import consulo.annotation.internal.MigratedExtensionsTo;
import consulo.mercurial.localize.HgLocalize;
import jakarta.annotation.Nullable;

import consulo.application.CommonBundle;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.PropertyKey;

import java.util.ResourceBundle;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

@Deprecated
@DeprecationInfo("Use HgLocalize")
@MigratedExtensionsTo(HgLocalize.class)
public final class HgVcsMessages {

  public static String message(@Nonnull @PropertyKey(resourceBundle = BUNDLE) String key, @Nonnull @Nullable Object... params) {
    return CommonBundle.message(getBundle(), key, params);
  }

  private static Reference<ResourceBundle> ourBundle;
  private static final String BUNDLE = "org.zmlx.hg4idea.HgVcsMessages";

  private HgVcsMessages() {
  }

  private static ResourceBundle getBundle() {
    ResourceBundle bundle = consulo.util.lang.ref.SoftReference.dereference(ourBundle);
    if (bundle == null) {
      bundle = ResourceBundle.getBundle(BUNDLE);
      ourBundle = new SoftReference<>(bundle);
    }
    return bundle;
  }
}
