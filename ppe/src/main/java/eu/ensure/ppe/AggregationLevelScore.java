package eu.ensure.ppe;

import eu.ensure.commons.lang.Number;
import eu.ensure.commons.statistics.MovingAverage;
import eu.ensure.ppe.model.Consequence;

import java.util.Collection;
import java.util.LinkedList;

public class AggregationLevelScore {
    public static final double FAILURE_SCORE = 0.0;

    final String id; // Id of aggregation
    final String name;

    final double score;
    final double stdDev;
    final double cv;
    final long datasetSize;
    final Collection<String> storyLine;
    final Collection<Consequence> consequences;

    AggregationLevelScore(String aggrId, String aggrName, MovingAverage stats,
                          Collection<String> storyLine, Collection<Consequence> consequences) {
        this.id = aggrId;
        this.name = aggrName;
        this.storyLine = storyLine;
        if (null == consequences) {
            this.consequences = new LinkedList<Consequence>();
        } else {
            this.consequences = consequences;
        }

        //
        this.score = Number.roundTwoDecimals(stats.getAverage());
        this.stdDev = Number.roundTwoDecimals(stats.getStdDev());
        this.cv = Number.roundTwoDecimals(stats.getCV());
        this.datasetSize = stats.getCount();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getScore() {
        return score;
    }

    public double getStdDev() {
        return stdDev;
    }

    public double getCV() {
        return cv;
    }

    public long getDatasetSize() {
        return datasetSize;
    }

    public Collection<String> getStoryLine() {
        return storyLine;
    }

    public Collection<Consequence> getConsequences() {
        return consequences;
    }
}