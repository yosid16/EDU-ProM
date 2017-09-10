package org.eduprom.miners.alpha;

import org.processmining.alphaminer.parameters.AlphaVersion;

/**
 * Created by ydahari on 4/12/2017.
 */
public class AlphaSharp extends Alpha {

    public AlphaSharp(String filename) throws Exception {
        super(filename);
    }

    @Override
    public AlphaVersion GetVersion(){
        return AlphaVersion.SHARP;
    }
}
