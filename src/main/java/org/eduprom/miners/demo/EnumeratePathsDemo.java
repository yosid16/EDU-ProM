package org.eduprom.miners.demo;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.eduprom.exceptions.LogFileNotFoundException;
import org.eduprom.exceptions.MiningException;
import org.eduprom.miners.AbstractPetrinetMiner;
import org.eduprom.utils.PetrinetHelper;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.AbstractBlock;
import org.processmining.processtree.impl.AbstractTask;
import org.processmining.processtree.impl.ProcessTreeImpl;

import static org.processmining.ptconversions.pn.ProcessTree2Petrinet.PetrinetWithMarkings;

public class EnumeratePathsDemo extends AbstractPetrinetMiner {

    public EnumeratePathsDemo(String filename) throws LogFileNotFoundException {
		super(filename);
	}

    @Override
    protected PetrinetWithMarkings minePetrinet() throws MiningException {
        ProcessTree pt = new ProcessTreeImpl();
        AbstractBlock.Xor root = new AbstractBlock.Xor("root");
        pt.addNode(root);
        pt.setRoot(root);

        for(XTrace trace : log){
            AbstractBlock.Seq seq = new AbstractBlock.Seq("");
            pt.addNode(seq);
            root.addChild(seq);

            int length = trace.size();
            for(int i = 0; i < length; i++){
                XEvent event = trace.get(i);
                String activity = event.getAttributes().get("concept:name").toString();

                AbstractTask.Manual task = new AbstractTask.Manual(activity);
                pt.addNode(task);
                seq.addChild(task);
            }
        }

        return PetrinetHelper.ConvertToPetrinet(pt);
    }
}
