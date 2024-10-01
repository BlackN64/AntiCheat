package org.ayosynk.antiCheat;

public class AttackData {
    private int attackCount;
    private long lastAttackTime;
    private int warningCount;

    public AttackData(int attackCount, long lastAttackTime, int warningCount) {
        this.attackCount = attackCount;
        this.lastAttackTime = lastAttackTime;
        this.warningCount = warningCount;
    }

    public int getAttackCount() {
        return attackCount;
    }

    public void setAttackCount(int attackCount) {
        this.attackCount = attackCount;
    }

    public long getLastAttackTime() {
        return lastAttackTime;
    }

    public void setLastAttackTime(long lastAttackTime) {
        this.lastAttackTime = lastAttackTime;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(int warningCount) {
        this.warningCount = warningCount;
    }
}
