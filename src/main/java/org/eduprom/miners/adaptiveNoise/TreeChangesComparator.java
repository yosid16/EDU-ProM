package org.eduprom.miners.adaptiveNoise;

import java.util.Comparator;

public class TreeChangesComparator<T> implements Comparator<TreeChanges> {
    @Override
    public int compare(TreeChanges treeChanges, TreeChanges t1) {
        return Double.compare(treeChanges.getConformanceInfo().maxValue(), t1.getConformanceInfo().maxValue());
    }
}
