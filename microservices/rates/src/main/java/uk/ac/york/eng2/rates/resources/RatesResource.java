package uk.ac.york.eng2.rates.resources;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.tags.Tag;
import uk.ac.york.eng2.rates.dto.*;
import uk.ac.york.eng2.rates.utils.SlidingWindowAverage;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Fake implementation of the relevant parts of the Agile Rates API, to make
 * sure the assessment can be done regardless of whether the real API is up
 * or not.
 */
@Tag(name="rates")
@Controller
public class RatesResource {

  private static final double
      RATE_CHEAP_IMPORT = 10,
      RATE_NORMAL_IMPORT = 15,
      RATE_EXPENSIVE_IMPORT = 20;

  private static final double
      RATE_CHEAP_EXPORT = 5,
      RATE_NORMAL_EXPORT = 10,
      RATE_EXPENSIVE_EXPORT = 15;

  private static final int MINUTES_PER_SLOT = 30;

  // Cheap imports and exports during the night - [start, end)
  private static final int HOUR_CHEAP_START = 1, HOUR_CHEAP_END = 5;

  // Expensive imports and exports during dinnertime - [start, end)
  private static final int HOUR_EXPENSIVE_START = 16, HOUR_EXPENSIVE_END = 19;

  // Window sizes to use to compute cheapest windows
  private static final int[] WINDOW_MINUTES = {60, 120, 180, 240, 300, 360};

  private static void addCheapestWindows(Instant from, Instant to, AgileRatesResponse response) {
    for (var time = from; time.isBefore(to); time = time.plus(MINUTES_PER_SLOT, ChronoUnit.MINUTES)) {
      var bestWindowByMinutes = new TreeMap<Integer, AgileRateWindow>();
      for (var windowMinutes : WINDOW_MINUTES) {
        addBestWindow(windowMinutes, response, bestWindowByMinutes);
      }
      response.getCheapestWindowsPerSlot().put(time, bestWindowByMinutes);
    }
  }

  private static void addBestWindow(int windowMinutes, AgileRatesResponse response, Map<Integer, AgileRateWindow> bestWindowByMinutes) {
    final int targetSlotCount = windowMinutes / MINUTES_PER_SLOT;
    final var slidingAverage = new SlidingWindowAverage<Instant>(targetSlotCount);

    // Form the first full window
    Iterator<AgileSlotRates> itRates = response.getRates().iterator();
    while (!slidingAverage.isFull() && itRates.hasNext()) {
      AgileSlotRates next = itRates.next();
      slidingAverage.add(next.getDeliveryStart(), next.getAgileRate().getResult().getRate());
    }
    if (!slidingAverage.isFull()) {
      // skip over durations where we cannot form a full window
      return;
    }
    double bestAverageRate = slidingAverage.getAverage();
    Instant bestWindowStart = slidingAverage.getMinimumKey();
    Instant bestWindowEnd = slidingAverage.getMaximumKey();
    bestWindowEnd = bestWindowEnd.plus(MINUTES_PER_SLOT, ChronoUnit.MINUTES);

    // Now slide over the rest of the entries
    while (itRates.hasNext()) {
      AgileSlotRates next = itRates.next();
      slidingAverage.add(next.getDeliveryStart(), next.getAgileRate().getResult().getRate());

      final double newAverageRate = slidingAverage.getAverage();
      if (newAverageRate < bestAverageRate) {
        bestAverageRate = newAverageRate;
        bestWindowStart = slidingAverage.getMinimumKey();
        bestWindowEnd = slidingAverage.getMaximumKey().plus(MINUTES_PER_SLOT, ChronoUnit.MINUTES);
      }
    }

    AgileRateWindow bestWindow = new AgileRateWindow();
    bestWindow.setStartTime(bestWindowStart);
    bestWindow.setEndTime(bestWindowEnd);
    bestWindow.setAverageRate(bestAverageRate);
    bestWindow.setPrediction(true);
    bestWindowByMinutes.put(windowMinutes, bestWindow);
  }

  private static void addSlotRates(Instant from, Instant to, AgileRatesResponse response) {
    for (var time = from; time.isBefore(to); time = time.plus(MINUTES_PER_SLOT, ChronoUnit.MINUTES)) {
      addSlotRates(time, response);
    }
  }

  private static void addSlotRates(Instant time, AgileRatesResponse response) {
    var slotRates = new AgileSlotRates();
    slotRates.setDeliveryStart(time);
    slotRates.setDeliveryEnd(time.plus(MINUTES_PER_SLOT, ChronoUnit.MINUTES));

    final int hour = LocalDateTime.ofInstant(time, ZoneId.systemDefault()).getHour();

    var importRate = new AgileRate();
    importRate.setResult(new AgileRateResult());
    importRate.getResult().setSource("dummy");
    importRate.getResult().setPrediction(true);
    importRate.getResult().setRate(getImportRate(hour));
    slotRates.setAgileRate(importRate);

    var exportRate = new AgileRate();
    exportRate.setResult(new AgileRateResult());
    exportRate.getResult().setSource("dummy");
    exportRate.getResult().setPrediction(true);
    exportRate.getResult().setRate(getExportRate(hour));
    slotRates.setAgileOutgoingRate(exportRate);

    response.getRates().add(slotRates);
  }

  protected static double getImportRate(int hour) {
    if (hour >= HOUR_CHEAP_START && hour < HOUR_CHEAP_END) {
      return RATE_CHEAP_IMPORT;
    } else if (hour >= HOUR_EXPENSIVE_START && hour < HOUR_EXPENSIVE_END) {
      return RATE_EXPENSIVE_IMPORT;
    } else {
      return RATE_NORMAL_IMPORT;
    }
  }

  protected static double getExportRate(int hour) {
    if (hour >= HOUR_CHEAP_START && hour < HOUR_CHEAP_END) {
      return RATE_CHEAP_EXPORT;
    } else if (hour >= HOUR_EXPENSIVE_START && hour < HOUR_EXPENSIVE_END) {
      return RATE_EXPENSIVE_EXPORT;
    } else {
      return RATE_NORMAL_EXPORT;
    }
  }

  @Get
  public AgileRatesResponse getRates() {
    var response = new AgileRatesResponse();
    response.setResult("ok");

    // Go from yesterday noon to tomorrow noon
    final var from = Instant.now()
        .truncatedTo(ChronoUnit.DAYS)
        .minus(1, ChronoUnit.DAYS)
        .plus(12, ChronoUnit.HOURS);
    final var to = from
        .plus(2, ChronoUnit.DAYS);

    addSlotRates(from, to, response);
    addCheapestWindows(from, to, response);
    return response;
  }
}
