package uk.ac.york.eng2.reactive.service;

import jakarta.inject.Singleton;

@Singleton
public class BatteryDecisionService {

    private static final double CHEAP_IMPORT_THRESHOLD = 10.0;
    private static final double HIGH_EXPORT_THRESHOLD = 20.0;

    public String decideTargetState(Double importRate, Double exportRate) {
        if (importRate == null || exportRate == null) {
            return "sell_excess";
        }
        if (exportRate > HIGH_EXPORT_THRESHOLD) {
            return "export_to_grid";
        }
        if (importRate < CHEAP_IMPORT_THRESHOLD) {
            return "import_to_100";
        }
        return "sell_excess";
    }
}