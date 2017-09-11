package org.eduprom.miners.alpha;

import org.processmining.alphaminer.parameters.AlphaVersion;

public class AlphaSharp extends Alpha {

    public AlphaSharp(String filename) throws Exception {
        super(filename);
    }

    @Override
    public AlphaVersion getVersion(){
        return AlphaVersion.SHARP;
    }
}
