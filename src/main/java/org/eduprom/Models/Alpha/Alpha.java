package org.eduprom.Models.Alpha;

import org.eduprom.Models.AbstractModel;
import org.eduprom.Models.AbstractPetrinetModel;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.model.XLog;
import org.processmining.alphaminer.abstractions.AlphaClassicAbstraction;
import org.processmining.alphaminer.algorithms.AlphaMiner;
import org.processmining.alphaminer.algorithms.AlphaMinerFactory;
import org.processmining.alphaminer.parameters.AlphaMinerParameters;
import org.processmining.alphaminer.parameters.AlphaVersion;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;

import org.processmining.ptconversions.pn.ProcessTree2Petrinet;

/**
 * Created by ydahari on 4/12/2017.
 */
public class Alpha extends AbstractPetrinetModel {

    public Alpha(String filename) throws Exception {
        super(filename);
    }

    @Override
    protected ProcessTree2Petrinet.PetrinetWithMarkings TrainPetrinet() throws Exception {
        Pair<Petrinet, Marking> runResult = Mine(_log);

        ProcessTree2Petrinet.PetrinetWithMarkings pn = new ProcessTree2Petrinet.PetrinetWithMarkings();
        pn.petrinet = runResult.getFirst();
        pn.initialMarking = runResult.getSecond();

        return pn;
    }

    public Pair<Petrinet, Marking> Mine(XLog log){
        AlphaMinerParameters p = new AlphaMinerParameters();
        p.setVersion(GetVersion());
        AlphaMiner<XEventClass, ? extends AlphaClassicAbstraction<XEventClass>, ? extends AlphaMinerParameters> alphaMiner
                = AlphaMinerFactory.createAlphaMiner(_promPluginContext, log, GetClassifier(), p);
        Pair<Petrinet, Marking> runResult = alphaMiner.run();
        return runResult;
    }

    public AlphaVersion GetVersion(){
        return AlphaVersion.CLASSIC;
    }
}