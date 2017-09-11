package org.eduprom.miners.alpha;

import org.processmining.alphaminer.parameters.AlphaVersion;

public class AlphaPlusPlus extends Alpha {

    public AlphaPlusPlus(String filename) throws Exception {
        super(filename);
    }

    @Override
    public AlphaVersion getVersion(){
        return AlphaVersion.PLUS_PLUS;
    }
}
