package uk.ac.york.eng2.reactive.resources;

import org.junit.jupiter.api.Test;
import uk.ac.york.eng2.reactive.service.BatteryDecisionService;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BatteryDecisionServiceTest {

    private final BatteryDecisionService service = new BatteryDecisionService();

    @Test
    public void cheapImportChoosesImportTo100() {
        assertEquals("import_to_100", service.decideTargetState(7.0, 15.0));
    }

    @Test
    public void highExportChoosesExportToGrid() {
        assertEquals("export_to_grid", service.decideTargetState(12.0, 25.0));
    }

    @Test
    public void neutralRatesChooseSellExcess() {
        assertEquals("sell_excess", service.decideTargetState(12.0, 15.0));
    }

    @Test
    public void missingRatesChooseSellExcess() {
        assertEquals("sell_excess", service.decideTargetState(null, 15.0));
        assertEquals("sell_excess", service.decideTargetState(7.0, null));
    }
}