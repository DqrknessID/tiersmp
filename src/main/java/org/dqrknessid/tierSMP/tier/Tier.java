package org.dqrknessid.tierSMP.tier;

public enum Tier {
    S(500),
    A(300),
    B(150),
    C(50),
    UNRANKED(0);

    private final int scoreMin;

    Tier(int scoreMin) {
        this.scoreMin = scoreMin;
    }

    public int getScoreMin() {
        return scoreMin;
    }
}
