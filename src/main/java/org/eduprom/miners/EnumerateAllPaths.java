package org.eduprom.miners;

import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.eduprom.entities.Trace;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.AbstractBlock;
import org.processmining.processtree.impl.AbstractTask;
import org.processmining.processtree.impl.ProcessTreeImpl;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;
import static org.processmining.ptconversions.pn.ProcessTree2Petrinet.PetrinetWithMarkings;

/**
 * Created by ydahari on 22/10/2016.
 */
public class EnumerateAllPaths extends AbstractPetrinetMiner implements IProcessTreeMiner {

    public EnumerateAllPaths(String filename) throws Exception {
		super(filename);
	}

	@Override
    protected PetrinetWithMarkings minePetrinet() throws Exception {
		logger.info("Started mining a petri nets using enumerate paths miner");

		ProcessTree2Petrinet.PetrinetWithMarkings pn = petrinetHelper.ConvertToPetrinet(mineProcessTree(log));
		return pn;
    }

	@Override
	public ProcessTree mineProcessTree(XLog log) throws Exception {
		ProcessTree pt = new ProcessTreeImpl();
		AbstractBlock.Xor root = new AbstractBlock.Xor("root");
		pt.addNode(root);
		pt.setRoot(root);

		for(XTrace l : log){
			Trace t = new Trace(l);
			//traceHelper.Add(t);

			AbstractBlock.Seq seq = new AbstractBlock.Seq("");
			pt.addNode(seq);
			root.addChild(seq);
			for(String a : t.Activities){
				AbstractTask.Manual task = new AbstractTask.Manual(a);
				pt.addNode(task);
				seq.addChild(task);
			}
		}

		return pt;
	}
}
