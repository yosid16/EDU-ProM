package org.eduprom.miners.adaptiveNoise;

import java.util.HashSet;
import java.util.Set;

public class TreeChangesSet {

    private Set<Change> changes;

    public TreeChangesSet(){
        this.changes = new HashSet<>();
    }


    public Set<Change> getChanges(){
        return this.changes;
    }


    @Override
    public boolean equals(Object o) {
        return (o instanceof TreeChangesSet) && changes.equals(((TreeChangesSet)o).getChanges());
    }

    @Override
    public int hashCode() {
        return changes.hashCode();
    }

    public TreeChangesSet toTreeChangesSet(){
        TreeChangesSet changesSet = new TreeChangesSet();
        changesSet.getChanges().addAll(this.changes);
        return changesSet;
    }

    public TreeChangesSet add(Change change){
        changes.add(change);
        return this;
    }
}
