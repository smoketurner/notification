package com.smoketurner.notification.application.benchmarks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import com.google.common.collect.ImmutableMap;
import com.smoketurner.notification.api.Notification;
import com.smoketurner.notification.api.Rule;
import com.smoketurner.notification.application.core.Rollup;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class RollupBenchmark {

    private static final String CATEGORY = "test";
    private final Rule sizeRule = Rule.builder().withMaxSize(3).build();
    private final List<Notification> notifications = new ArrayList<>(1000);

    @Setup
    public void setUp() {
        for (long i = 0; i < 1000; i++) {
            notifications.add(createNotification(i));
        }
    }

    @Benchmark
    public Stream<Notification> rollupNoRules() {
        final Rollup rollup = new Rollup(ImmutableMap.of());
        return rollup.rollup(notifications.stream());
    }

    @Benchmark
    public Stream<Notification> rollupNoMatches() {
        final Rollup rollup = new Rollup(ImmutableMap.of("other", sizeRule));
        return rollup.rollup(notifications.stream());
    }

    @Benchmark
    public Stream<Notification> rollupEveryMatch() {
        final Rollup rollup = new Rollup(ImmutableMap.of(CATEGORY, sizeRule));
        return rollup.rollup(notifications.stream());
    }

    public static void main(String[] args) throws Exception {
        new Runner(new OptionsBuilder()
                .include(RollupBenchmark.class.getSimpleName()).forks(1)
                .warmupIterations(5).measurementIterations(5).build()).run();
    }

    private Notification createNotification(final long id) {
        return Notification.builder(CATEGORY).withId(id).build();
    }
}
