package uk.ac.york.eng2.iot.scheduled;

import io.micronaut.context.annotation.Value;
import io.micronaut.scheduling.annotation.Scheduled;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

import java.util.Optional;

/**
 * Example skeleton of a possible simulation of temperature sensors. Adapt it or discard it based on
 * the home automation scenario you intend to simulate.
 * <p>
 * (In a real home automation solution, instead of this class, we would have code that periodically
 * communicates with real sensors, or that subscribes to the updates coming from the sensors in some
 * way.)
 * <p>
 * This is a @Singleton, so that Micronaut will ensure that at most one instance of this class will
 * be instantiated for each instance of the microservice.
 */
@Singleton
public class HeatingSimulation {

  public static final String TYPE_TEMPERATURE = "temperature";

  /*
   * The @Value annotations allow for obtaining values from the Micronaut configuration framework
   * (e.g. from the .properties files, and/or environment variables). See the Micronaut docs for
   * more information:
   *
   * https://docs.micronaut.io/latest/guide/#propertySource
   */
  @Value("${heaters.simulation.outside:10}")
  private double outsideCelsius;

  @Value("${heaters.simulation.step:0.5}")
  private double stepCelsius;

  @Value("${heaters.simulation.enabled:true}")
  private boolean enabled;

  public double getOutsideCelsius() {
    return outsideCelsius;
  }

  public void setOutsideCelsius(double outsideCelsius) {
    this.outsideCelsius = outsideCelsius;
  }

  public double getStepCelsius() {
    return stepCelsius;
  }

  public void setStepCelsius(double stepCelsius) {
    this.stepCelsius = stepCelsius;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /*
   * The @Scheduled annotation tells Micronaut to invoke this method periodically,
   * possibly under certain conditions. For more information, check the Micronaut
   * documentation:
   *
   *   https://docs.micronaut.io/latest/guide/#scheduling
   */
  @Transactional
  @Scheduled(fixedDelay = "${heaters.simulation.rate:1m}", condition = "#{this.enabled}")
  public void simulateHeating() {
    // 1. Find all temperature sensors

    // 2. For every sensor, if there is a heater in the same room as the sensor...

    // 2.1. Update the temperature (see computeNewTemperature):
    // - Use latest reading as current temperature, or outside temp if no readings yet
    // - If heater is off, reduce temperature by the step, up to outside temp
    // - If heater is on, increase temperature by the step, up to target temp
  }

  /*
   * Simulates a step change in room temperature based on whether the heater actuator
   * is on or not. The target temperature would be obtained from the heater's target
   * state.
   */
  protected double computeNewTemperature(double currentTemp, boolean isOn,
                                         Optional<Integer> targetTemperature) {
    if (isOn) {
      return Math.min(targetTemperature.get(), currentTemp + stepCelsius);
    } else {
      return Math.max(outsideCelsius, currentTemp - stepCelsius);
    }
  }
}
