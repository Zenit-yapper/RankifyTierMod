package com.rankify.client.tier;

import java.util.concurrent.ConcurrentHashMap;

public class TierCache {
    // Key is USERNAME (lowercase) for cracked servers, not UUID
    private final ConcurrentHashMap<String, TierData> cache = new ConcurrentHashMap<>();
    
    public void put(String usernameKey, TierData data) {
        cache.put(usernameKey.toLowerCase(), data);
    }
    
    public TierData get(String username) {
        if (username == null) return null;
        TierData data = cache.get(username.toLowerCase());
        if (data != null && data.isExpired()) {
            cache.remove(username.toLowerCase());
            return null;
        }
        return data;
    }
    
    public void remove(String username) {
        cache.remove(username.toLowerCase());
    }
    
    public void cleanup() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    public void clear() {
        cache.clear();
    }
    
    public int size() {
        return cache.size();
    }
}
