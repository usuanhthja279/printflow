package com.printflow.identity;

import java.time.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LoginThrottleTest {
    @Test void accountLimitCannotBeBypassedByChangingAddress() {
        LoginThrottle throttle=new LoginThrottle(Clock.fixed(Instant.now(),ZoneOffset.UTC));
        for(int i=0;i<8;i++)assertTrue(throttle.allow("address-"+i,"staff"));
        assertFalse(throttle.allow("another-address","staff"));
        assertTrue(throttle.allow("another-address","other-staff"));
    }
    @Test void concurrentAttemptsCannotExceedTheLimit() throws Exception {
        LoginThrottle throttle=new LoginThrottle(Clock.systemUTC());
        try(var pool=Executors.newFixedThreadPool(12)) {
            var futures=new java.util.ArrayList<Future<Boolean>>();
            for(int i=0;i<40;i++)futures.add(pool.submit(()->throttle.allow("same-address","same-staff")));
            int allowed=0;for(var future:futures)if(future.get())allowed++;
            assertEquals(8,allowed);
        }
    }
    @Test void expiredWindowsAllowNewAttempts() {
        class MutableClock extends Clock {
            Instant time=Instant.parse("2026-10-23T00:00:00Z");
            public ZoneId getZone(){return ZoneOffset.UTC;}
            public Clock withZone(ZoneId zone){return this;}
            public Instant instant(){return time;}
        }
        var clock=new MutableClock();var throttle=new LoginThrottle(clock);
        for(int i=0;i<8;i++)assertTrue(throttle.allow("local","staff"));
        assertFalse(throttle.allow("local","staff"));
        clock.time=clock.time.plus(Duration.ofMinutes(15));
        assertTrue(throttle.allow("local","staff"));
    }
}
