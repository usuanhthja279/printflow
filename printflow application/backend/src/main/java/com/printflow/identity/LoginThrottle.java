package com.printflow.identity;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/** Bounded single-instance login throttle. Shared deployments need shared counters. */
public final class LoginThrottle {
    private record Window(long start,int count) {}
    private final Map<String,Window> windows=new HashMap<>();
    private final Clock clock;
    public LoginThrottle(Clock clock) { this.clock=clock; }
    public synchronized boolean allow(String address,String username) {
        long now=clock.millis(), cutoff=now-Duration.ofMinutes(15).toMillis();
        windows.entrySet().removeIf(e->e.getValue().start()<=cutoff);
        if(windows.size()>=10000)return false;
        boolean ip=increment("ip:"+address,now,30);
        boolean account=increment("user:"+username,now,8);
        return ip&&account;
    }
    private boolean increment(String key,long now,int limit) {
        Window value=windows.getOrDefault(key,new Window(now,0));
        int count=Math.min(value.count()+1,limit+1);
        windows.put(key,new Window(value.start(),count));
        return count<=limit;
    }
}
