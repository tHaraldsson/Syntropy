package com.haraldsson.syntropy.entities;

import java.util.HashMap;
import java.util.Map;

public class Colonist {
    private static final float HUNGER_DECAY_PER_SECOND = 2f;
    private static final float ENERGY_DECAY_PER_SECOND = 1.2f;
    private static final float MOOD_DECAY_PER_SECOND = 0.5f;
    private static final float HUNGER_EAT_AMOUNT = 40f;
    private static final float ENERGY_REST_AMOUNT = 30f;
    private static final float MOOD_BOOST_AMOUNT = 15f;

    private final String name;
    private final int age;
    private float hunger = 100f;
    private float energy = 100f;
    private float mood = 100f;
    private boolean dead;

    private float x;
    private float y;

    private boolean aiDisabled;
    private Item carriedItem;

    private TaskType taskType = TaskType.IDLE;
    private int targetX = -1;
    private int targetY = -1;

    private float wanderTimer;
    private float wanderCooldown;

    private final Map<String, Integer> skills = new HashMap<>();

    public Colonist(String name, int age, float x, float y) {
        this.name = name;
        this.age = age;
        this.x = x;
        this.y = y;
        skills.put("mining", 1);
        skills.put("hauling", 1);
    }

    public void updateNeeds(float delta) {
        if (dead) return;
        hunger = Math.max(0f, hunger - HUNGER_DECAY_PER_SECOND * delta);
        energy = Math.max(0f, energy - ENERGY_DECAY_PER_SECOND * delta);

        // Mood affected by other needs
        if (hunger < 20f || energy < 20f) {
            mood = Math.max(0f, mood - MOOD_DECAY_PER_SECOND * 3f * delta);
        } else if (hunger > 60f && energy > 60f) {
            mood = Math.min(100f, mood + MOOD_DECAY_PER_SECOND * 0.5f * delta);
        } else {
            mood = Math.max(0f, mood - MOOD_DECAY_PER_SECOND * delta);
        }

        if (hunger <= 0f) {
            dead = true;
        }
    }

    public boolean isHungry() {
        return hunger <= 35f;
    }

    public boolean isTired() {
        return energy <= 25f;
    }

    public void eat() {
        hunger = Math.min(100f, hunger + HUNGER_EAT_AMOUNT);
        mood = Math.min(100f, mood + MOOD_BOOST_AMOUNT);
    }

    public void rest() {
        energy = Math.min(100f, energy + ENERGY_REST_AMOUNT);
    }

    public Map<String, Integer> getSkills() {
        return skills;
    }

    public int getSkill(String skill) {
        return skills.getOrDefault(skill, 0);
    }

    public void setTask(TaskType taskType, int targetX, int targetY) {
        this.taskType = taskType;
        this.targetX = targetX;
        this.targetY = targetY;
    }

    public void clearTask() {
        this.taskType = TaskType.IDLE;
        this.targetX = -1;
        this.targetY = -1;
    }

    public void moveTowardTarget(float delta, float speed) {
        if (targetX < 0 || targetY < 0) {
            return;
        }
        float targetCenterX = targetX + 0.5f;
        float targetCenterY = targetY + 0.5f;
        float dx = targetCenterX - x;
        float dy = targetCenterY - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < 0.05f) {
            x = targetCenterX;
            y = targetCenterY;
            return;
        }
        float step = speed * delta;
        x += (dx / dist) * step;
        y += (dy / dist) * step;
    }

    public boolean isAtTarget() {
        if (targetX < 0 || targetY < 0) {
            return false;
        }
        float dx = x - (targetX + 0.5f);
        float dy = y - (targetY + 0.5f);
        return dx * dx + dy * dy < 0.02f;
    }

    public void resetWanderCooldown(float cooldownSeconds) {
        wanderCooldown = cooldownSeconds;
        wanderTimer = 0f;
    }

    public boolean shouldPickNewWanderTarget(float delta) {
        wanderTimer += delta;
        return wanderCooldown <= 0f || wanderTimer >= wanderCooldown || taskType == TaskType.IDLE;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public float getHunger() {
        return hunger;
    }

    public void setHunger(float hunger) {
        this.hunger = hunger;
    }

    public float getEnergy() {
        return energy;
    }

    public void setEnergy(float energy) {
        this.energy = energy;
    }

    public float getMood() {
        return mood;
    }

    public void setMood(float mood) {
        this.mood = mood;
    }

    public boolean isDead() {
        return dead;
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public boolean isAiDisabled() {
        return aiDisabled;
    }

    public void setAiDisabled(boolean aiDisabled) {
        this.aiDisabled = aiDisabled;
    }

    public Item getCarriedItem() {
        return carriedItem;
    }

    public void setCarriedItem(Item carriedItem) {
        this.carriedItem = carriedItem;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public int getTargetX() {
        return targetX;
    }

    public int getTargetY() {
        return targetY;
    }
}

