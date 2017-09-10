package org.eduprom.miners;

import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XTrace;
import org.eduprom.entities.Trace;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.AbstractBlock;
import org.processmining.processtree.impl.AbstractTask;
import org.processmining.processtree.impl.ProcessTreeImpl;

import static org.processmining.ptconversions.pn.ProcessTree2Petrinet.PetrinetWithMarkings;

/**
 * Created by ydahari on 22/10/2016.
 */
public class EnumeratePaths extends AbstractPetrinetMiner {

    private String[] getActivities(XTrace trace){
        String[] activities = new String[trace.size()];
        for(int i = 0; i < activities.length; i++){
            XEvent event = trace.get(i);
            String activity = event.getAttributes().get("concept:name").toString();
            activities[i] = activity;
        }

        return activities;
    }

    public EnumeratePaths(String filename) throws Exception {
		super(filename);
	}

    @Override
    protected PetrinetWithMarkings minePetrinet() throws Exception {
        ProcessTree pt = new ProcessTreeImpl();
        AbstractBlock.Xor root = new AbstractBlock.Xor("root");
        pt.addNode(root);
        pt.setRoot(root);

        for(XTrace trace : log){
            AbstractBlock.Seq seq = new AbstractBlock.Seq("");
            pt.addNode(seq);
            root.addChild(seq);

            for(String a : getActivities(trace)){
                AbstractTask.Manual task = new AbstractTask.Manual(a);
                pt.addNode(task);
                seq.addChild(task);
            }
        }

        PetrinetWithMarkings petrinetWithMarkings =  petrinetHelper.ConvertToPetrinet(pt);
        return petrinetWithMarkings;
    }


}
