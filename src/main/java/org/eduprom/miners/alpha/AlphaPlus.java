package org.eduprom.miners.alpha;

import org.processmining.alphaminer.parameters.AlphaVersion;

public class AlphaPlus extends Alpha {

    public AlphaPlus(String filename) throws Exception {
        super(filename);
    }

    @Override
    public AlphaVersion getVersion(){
        return AlphaVersion.PLUS;
    }
}
