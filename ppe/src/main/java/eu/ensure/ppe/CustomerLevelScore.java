package eu.ensure.ppe;

import eu.ensure.vopn.statistics.MovingAverage;

import java.util.Collection;

public class CustomerLevelScore extends AggregationLevelScore {

    final double customerImpact;
    final double providerImpact;
    final double reconfigurationImpact;

    CustomerLevelScore(String gppId, MovingAverage stats,
                       double customerImpact, double providerImpact, double reconfigurationImpact,
                       Collection<String> storyLine) {
        super(gppId, "-GPP-", stats, storyLine, null);

        this.customerImpact = customerImpact;
        this.providerImpact = providerImpact;
        this.reconfigurationImpact = reconfigurationImpact;
    }

    public double getCustomerImpact() {
        return customerImpact;
    }

    public double getProviderImpact() {
        return providerImpact;
    }

    public double getReconfigurationImpact() {
        return reconfigurationImpact;
    }
}