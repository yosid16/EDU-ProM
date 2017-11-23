package org.eduprom.benchmarks.configuration;

import org.eduprom.exceptions.MiningException;

import java.util.ArrayList;
import java.util.List;

public class NoiseThreshold {
    static final float MAX_THRESHOLD = 1.0f;

    private final float[] thresholds;

    public NoiseThreshold(float... thresholds) {
        this.thresholds = thresholds;

    }

    public float[] getThresholds() {
        return thresholds;
    }

    public static NoiseThreshold uniform(float interval, float min, float max) {
        List<Float> values = new ArrayList<>();
        for (float value = min; value <= max; value+=interval){
            values.add(value);
        }
        //values.add(1.0f);
        Float[] thresholds = values.toArray(new Float[0]);
        final float[] result = new float[thresholds.length];
        for (int i = 0; i < thresholds.length; i++) {
            result[i] = thresholds[i].floatValue();
        }

        return new NoiseThreshold(result);
    }

    public static NoiseThreshold uniform(float interval)  {
        return uniform(interval, 0.0f, MAX_THRESHOLD);
    }

    public static NoiseThreshold single(float interval){
        return new NoiseThreshold(interval);
    }
}
