package com.rankify.client.tier;

public class TierData {
    private final String tierName;
    private final int color;
    private final long timestamp;
    private final int score; // Optional: for sorting or display
    
    public TierData(String tierName, int color) {
        this(tierName, color, 0);
    }
    
    public TierData(String tierName, int color, int score) {
        this.tierName = tierName;
        this.color = color;
        this.score = score;
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getTierName() {
        return tierName;
    }
    
    public int getColor() {
        return color;
    }
    
    public int getScore() {
        return score;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    // Longer cache for cracked servers (10 minutes) since names change less frequently
    public boolean isExpired() {
        return System.currentTimeMillis() - timestamp > 600000; // 10 minutes
    }
    
    @Override
    public String toString() {
        return "TierData{name='" + tierName + "', color=" + color + "}";
    }
}
