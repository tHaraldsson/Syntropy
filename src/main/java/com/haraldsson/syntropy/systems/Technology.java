package com.haraldsson.syntropy.systems;

/**
 * Represents a single researchable technology.
 */
public class Technology {
    private final String id;
    private final String name;
    private final String description;
    private final float researchTime; // seconds to complete
    private float progress;           // seconds accumulated
    private boolean unlocked;

    public Technology(String id, String name, String description, float researchTime) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.researchTime = researchTime;
    }

    public void addProgress(float delta) {
        if (unlocked) return;
        progress += delta;
        if (progress >= researchTime) {
            progress = researchTime;
            unlocked = true;
        }
    }

    public float getProgressRatio() {
        return researchTime > 0 ? progress / researchTime : 1f;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public float getResearchTime() { return researchTime; }
    public float getProgress() { return progress; }
    public boolean isUnlocked() { return unlocked; }

    public void setProgress(float progress) { this.progress = progress; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }
}

