package com.mathtutor.tutor;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;

class GenerationCircuitTest {
    @Test void ignoresOlderCallsAndAdmitsOnlyOneProbeAtTheBoundary() {
        var clock = new AtomicLong();
        var circuit = new GenerationCircuit(clock::get);
        var stale = circuit.acquire();
        for (int error = 0; error < 3; error++) circuit.failed(circuit.acquire());
        circuit.succeeded(stale);
        circuit.failed(stale);
        clock.set(Duration.ofSeconds(30).toNanos() - 1);
        assertThatThrownBy(circuit::acquire).isInstanceOf(GenerationCircuit.Unavailable.class);
        clock.incrementAndGet();
        var probe = circuit.acquire();
        assertThat(probe.probe()).isTrue();
        assertThatThrownBy(circuit::acquire).isInstanceOf(GenerationCircuit.Unavailable.class);
        circuit.release(probe);
        circuit.failed(circuit.acquire());
        assertThatThrownBy(circuit::acquire).isInstanceOf(GenerationCircuit.Unavailable.class);
        clock.addAndGet(Duration.ofSeconds(30).toNanos());
        circuit.succeeded(circuit.acquire());
        assertThat(circuit.acquire().probe()).isFalse();
    }

    @Test void aSuccessfulRequestResetsTheConsecutiveFailureCounter() {
        var circuit = new GenerationCircuit(() -> 0);
        circuit.failed(circuit.acquire());
        circuit.failed(circuit.acquire());
        circuit.succeeded(circuit.acquire());
        circuit.failed(circuit.acquire());
        assertThat(circuit.acquire().probe()).isFalse();
    }
}
