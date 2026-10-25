package com.printflow.files;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PrintPreflightMetricsTest {
    @Test void calculatesEffectiveDpiFromSourcePixelsAndPhysicalMillimetres() {
        assertEquals(254.0,PrintPreflightMetrics.effectiveDpi(900,600,90,60),.01);
    }
    @Test void rejectsInvalidPhysicalOrPixelDimensions() {
        assertThrows(IllegalArgumentException.class,()->PrintPreflightMetrics.effectiveDpi(0,100,90,54));
        assertThrows(IllegalArgumentException.class,()->PrintPreflightMetrics.effectiveDpi(100,100,0,54));
    }
}
