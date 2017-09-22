package org.eduprom.miners.adaptiveNoise;

import org.eduprom.exceptions.LogFileNotFoundException;
import org.eduprom.exceptions.MiningException;
import org.eduprom.miners.AbstractPetrinetMiner;
import org.eduprom.miners.adaptiveNoise.IntermediateMiners.NoiseInductiveMiner;
import org.eduprom.partitioning.ILogSplitter;
import org.eduprom.partitioning.InductiveLogSplitting;
import org.eduprom.partitioning.Partitioning;
import org.eduprom.utils.PetrinetHelper;
import org.eduprom.utils.PocessTreeHelper;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;

import static org.processmining.ptconversions.pn.ProcessTree2Petrinet.PetrinetWithMarkings;

public class TestTreeChangesMiner extends AbstractPetrinetMiner {

    public TestTreeChangesMiner(String filename) throws LogFileNotFoundException {
		super(filename);
	}

    @Override
    protected PetrinetWithMarkings minePetrinet() throws MiningException {
        ILogSplitter logSplitter = new InductiveLogSplitting();
        Partitioning pratitioning = logSplitter.split(this.log);

        ProcessTree targetTree = pratitioning.getProcessTree().toTree();
        Node target = targetTree.getNodes().stream()
                .filter(x->x.isLeaf() && x.getName().equalsIgnoreCase("c")).findAny().get()
                .getParents().stream().findAny().get();


        NoiseInductiveMiner inductiveFiltered = new NoiseInductiveMiner(filename, 0.1f);
        Node source = inductiveFiltered.mineProcessTree(pratitioning.getPartitions().get(target.getID()).getLog()).getRoot();

        PocessTreeHelper helper = new PocessTreeHelper();
        helper.merge(source, target);


        return  PetrinetHelper.ConvertToPetrinet(target.getProcessTree().toTree());
    }
}
