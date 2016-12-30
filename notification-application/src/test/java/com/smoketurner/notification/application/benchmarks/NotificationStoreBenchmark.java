package com.smoketurner.notification.application.benchmarks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import com.smoketurner.notification.api.Notification;
import com.smoketurner.notification.application.store.NotificationStore;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class NotificationStoreBenchmark {

    private final List<Notification> notifications = new ArrayList<>(1000000);

    @Setup
    public void setUp() {
        for (long i = 0; i < 1000000; i++) {
            notifications.add(createNotification(i));
        }
    }

    @Benchmark
    public Stream<Notification> setUnseenState() {
        return NotificationStore.setUnseenState(notifications, true);
    }

    @Benchmark
    public Optional<Notification> tryFind() {
        return NotificationStore.tryFind(notifications, 10000);
    }

    @Benchmark
    public int indexOf() {
        return NotificationStore.indexOf(notifications, 10000);
    }

    public static void main(String[] args) throws Exception {
        new Runner(new OptionsBuilder()
                .include(NotificationStoreBenchmark.class.getSimpleName())
                .forks(1).warmupIterations(5).measurementIterations(5).build())
                        .run();
    }

    private Notification createNotification(final long id) {
        return Notification.builder().withId(id).build();
    }
}
