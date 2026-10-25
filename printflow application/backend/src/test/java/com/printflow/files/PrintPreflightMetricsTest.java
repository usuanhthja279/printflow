package com.printflow.files;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PrintPreflightMetricsTest {
    @Test void calculatesEffectiveDpiFromSourcePixelsAndPhysicalMillimetres() {
        assertEquals(254.0,PrintPreflightMetrics.effectiveDpi(900,600,90,60),.01);
    }
    @Test void containDpiUsesTheActuallyRenderedImageSizeRatherThanUnusedFramePadding() {
        // A 1600 × 800 image in a 100 × 100 mm CONTAIN frame renders at 100 × 50 mm.
        assertEquals(406.4,PrintPreflightMetrics.effectiveDpi(1600,800,100,100,"CONTAIN"),.01);
        assertEquals(203.2,PrintPreflightMetrics.effectiveDpi(1600,800,100,100,"STRETCH"),.01);
        assertEquals(203.2,PrintPreflightMetrics.effectiveDpi(1600,800,100,100,"COVER"),.01);
        assertEquals(406.4,PrintPreflightMetrics.effectiveDpi(800,1600,100,100,"CONTAIN"),.01);
    }
    @Test void rejectsInvalidPhysicalOrPixelDimensions() {
        assertThrows(IllegalArgumentException.class,()->PrintPreflightMetrics.effectiveDpi(0,100,90,54));
        assertThrows(IllegalArgumentException.class,()->PrintPreflightMetrics.effectiveDpi(100,100,0,54));
        assertThrows(IllegalArgumentException.class,()->PrintPreflightMetrics.effectiveDpi(100,100,90,54,"INVALID"));
    }
}
