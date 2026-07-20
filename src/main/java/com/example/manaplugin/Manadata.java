package com.example.manaplugin;

public class ManaData {
    private int currentMana;
    private int maxMana;

    public ManaData(int currentMana, int maxMana) {
        this.currentMana = currentMana;
        this.maxMana = maxMana;
    }

    public int getCurrentMana() {
        return currentMana;
    }

    public void setCurrentMana(int currentMana) {
        this.currentMana = Math.max(0, Math.min(currentMana, maxMana));
    }

    public int getMaxMana() {
        return maxMana;
    }

    public void setMaxMana(int maxMana) {
        this.maxMana = maxMana;
        if (currentMana > maxMana) {
            currentMana = maxMana;
        }
    }
}
