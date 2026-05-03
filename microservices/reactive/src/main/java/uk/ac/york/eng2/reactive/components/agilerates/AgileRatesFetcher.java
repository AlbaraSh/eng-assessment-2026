package uk.ac.york.eng2.reactive.components.agilerates;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Context
@Singleton
@Requires(notEnv = Environment.TEST)
public class AgileRatesFetcher {

    @Inject
    private AgileRatesFetcherLogic logic;

    @Scheduled(fixedDelay = "12h", initialDelay = "12h")
    public void fetchEvery12Hours() {
        logic.tryFetch();
    }

    @Scheduled(fixedDelay = "10s", initialDelay = "5s")
    public void retryUntilAvailable() {
        logic.retryUntilAvailable();
    }
}
