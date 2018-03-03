package org.eduprom.miners;

import org.eduprom.utils.PetrinetHelper;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet;

public interface IPetrinetMiner extends IMiner {
    ProcessTree2Petrinet.PetrinetWithMarkings getModel();

    PetrinetHelper getHelper();
}
