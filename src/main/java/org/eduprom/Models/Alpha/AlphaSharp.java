package org.eduprom.Models.Alpha;

import org.processmining.alphaminer.parameters.AlphaVersion;

/**
 * Created by ydahari on 4/12/2017.
 */
public class AlphaSharp extends Alpha {

    public AlphaSharp(String filename) throws Exception {
        super(filename);
    }

    public AlphaVersion GetVersion(){
        return AlphaVersion.SHARP;
    }
}
