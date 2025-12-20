/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import consulo.ui.ex.JBColor;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;
import consulo.versionControlSystem.log.*;
import consulo.versionControlSystem.log.base.SimpleRefGroup;
import consulo.versionControlSystem.log.base.SingletonRefGroup;
import consulo.versionControlSystem.log.util.VcsLogUtil;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.function.Function;

public class HgRefManager implements VcsLogRefManager {
    private static final Color CLOSED_BRANCH_COLOR = new JBColor(new Color(0x823139), new Color(0xff5f6f));
    private static final Color LOCAL_TAG_COLOR = new JBColor(new Color(0x009090), new Color(0x00f3f3));
    private static final Color MQ_TAG_COLOR = new JBColor(new Color(0x002f90), new Color(0x0055ff));

    public static final VcsRefType TIP = new SimpleRefType("TIP", true, VcsLogStandardColors.Refs.TIP);
    public static final VcsRefType HEAD = new SimpleRefType("HEAD", true, VcsLogStandardColors.Refs.LEAF);
    public static final VcsRefType BRANCH = new SimpleRefType("BRANCH", true, VcsLogStandardColors.Refs.BRANCH);
    public static final VcsRefType CLOSED_BRANCH = new SimpleRefType("CLOSED_BRANCH", false, CLOSED_BRANCH_COLOR);
    public static final VcsRefType BOOKMARK = new SimpleRefType("BOOKMARK", true, VcsLogStandardColors.Refs.BRANCH_REF);
    public static final VcsRefType TAG = new SimpleRefType("TAG", false, VcsLogStandardColors.Refs.TAG);
    public static final VcsRefType LOCAL_TAG = new SimpleRefType("LOCAL_TAG", false, LOCAL_TAG_COLOR);
    public static final VcsRefType MQ_APPLIED_TAG = new SimpleRefType("MQ_TAG", false, MQ_TAG_COLOR);

    // first has the highest priority
    private static final List<VcsRefType> REF_TYPE_PRIORITIES = Arrays.asList(TIP, HEAD, BRANCH, BOOKMARK, TAG);
    private static final List<VcsRefType> REF_TYPE_INDEX =
        Arrays.asList(TIP, HEAD, BRANCH, CLOSED_BRANCH, BOOKMARK, TAG, LOCAL_TAG, MQ_APPLIED_TAG);

    // -1 => higher priority
    public static final Comparator<VcsRefType> REF_TYPE_COMPARATOR = (type1, type2) -> {
        int p1 = REF_TYPE_PRIORITIES.indexOf(type1);
        int p2 = REF_TYPE_PRIORITIES.indexOf(type2);
        return p1 - p2;
    };

    private static final String DEFAULT = "default";

    // @NotNull private final RepositoryManager<HgRepository> myRepositoryManager;

    // -1 => higher priority, i. e. the ref will be displayed at the left
    private final Comparator<VcsRef> REF_COMPARATOR = (ref1, ref2) -> {
        VcsRefType type1 = ref1.getType();
        VcsRefType type2 = ref2.getType();

        int typeComparison = REF_TYPE_COMPARATOR.compare(type1, type2);
        if (typeComparison != 0) {
            return typeComparison;
        }

        int nameComparison = ref1.getName().compareTo(ref2.getName());
        if (nameComparison != 0) {
            if (type1 == BRANCH) {
                if (ref1.getName().equals(DEFAULT)) {
                    return -1;
                }
                if (ref2.getName().equals(DEFAULT)) {
                    return 1;
                }
            }
            return nameComparison;
        }

        return VcsLogUtil.compareRoots(ref1.getRoot(), ref2.getRoot());
    };

    @Nonnull
    @Override
    public Comparator<VcsRef> getLabelsOrderComparator() {
        return REF_COMPARATOR;
    }

    @Nonnull
    @Override
    public List<RefGroup> groupForBranchFilter(@Nonnull Collection<VcsRef> refs) {
        return ContainerUtil.map(sort(refs), (Function<VcsRef, RefGroup>) SingletonRefGroup::new);
    }

    @Nonnull
    @Override
    public List<RefGroup> groupForTable(@Nonnull Collection<VcsRef> references, boolean compact, boolean showTagNames) {
        List<VcsRef> sortedReferences = sort(references);
        MultiMap<VcsRefType, VcsRef> groupedRefs = ContainerUtil.groupBy(sortedReferences, VcsRef::getType);

        List<Map.Entry<VcsRefType, Collection<VcsRef>>> headAndTip = ContainerUtil.filter(
            groupedRefs.entrySet(),
            entry -> entry.getKey().equals(HEAD) || entry.getKey().equals(TIP)
        );
        headAndTip.forEach(entry -> groupedRefs.remove(entry.getKey()));

        List<RefGroup> result = SimpleRefGroup.buildGroups(List.of(), groupedRefs, compact, showTagNames);

        RefGroup firstGroup = ContainerUtil.getFirstItem(result);
        if (firstGroup != null) {
            firstGroup.getRefs().addAll(
                0,
                headAndTip.stream().flatMap(entry -> entry.getValue().stream()).toList()
            );
        }

        return result;
    }

    @Override
    public void serialize(@Nonnull DataOutput out, @Nonnull VcsRefType type) throws IOException {
        out.writeInt(REF_TYPE_INDEX.indexOf(type));
    }

    @Nonnull
    @Override
    public VcsRefType deserialize(@Nonnull DataInput in) throws IOException {
        int id = in.readInt();
        if (id < 0 || id > REF_TYPE_INDEX.size() - 1) {
            throw new IOException("Reference type by id " + id + " does not exist");
        }
        return REF_TYPE_INDEX.get(id);
    }

    @Nonnull
    @Override
    public Comparator<VcsRef> getBranchLayoutComparator() {
        return REF_COMPARATOR;
    }

    @Nonnull
    private List<VcsRef> sort(@Nonnull Collection<VcsRef> refs) {
        return ContainerUtil.sorted(refs, getLabelsOrderComparator());
    }

    private static record SimpleRefType(@Nonnull String name, boolean isBranch, @Nonnull Color color) implements VcsRefType {
        @Nonnull
        @Override
        public Color getBackgroundColor() {
            return color;
        }
    }
}
