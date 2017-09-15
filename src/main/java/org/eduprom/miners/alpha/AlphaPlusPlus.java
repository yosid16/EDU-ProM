package org.eduprom.miners.alpha;

import org.eduprom.exceptions.LogFileNotFoundException;
import org.processmining.alphaminer.parameters.AlphaVersion;

public class AlphaPlusPlus extends Alpha {

    public AlphaPlusPlus(String filename) throws LogFileNotFoundException {
        super(filename);
    }

    @Override
    public AlphaVersion getVersion(){
        return AlphaVersion.PLUS_PLUS;
    }
}
