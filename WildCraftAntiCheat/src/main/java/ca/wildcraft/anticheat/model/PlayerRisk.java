package ca.wildcraft.anticheat.model;

import org.bukkit.Location;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerRisk {
    private final UUID uuid;
    private String name;
    private double score;
    private int veins;
    private long lastAlert;
    private long lastUpdated;
    private CaseStatus status = CaseStatus.OPEN;
    private final Map<String, Integer> ores = new LinkedHashMap<>();
    private final Deque<String> locations = new ArrayDeque<>();
    private final Deque<String> timeline = new ArrayDeque<>();

    public PlayerRisk(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.lastUpdated = System.currentTimeMillis();
    }

    public UUID uuid() { return uuid; }
    public String name() { return name; }
    public double score() { return score; }
    public int veins() { return veins; }
    public long lastAlert() { return lastAlert; }
    public long lastUpdated() { return lastUpdated; }
    public CaseStatus status() { return status; }
    public Map<String, Integer> ores() { return ores; }
    public Deque<String> locations() { return locations; }
    public Deque<String> timeline() { return timeline; }

    public void name(String value) { name = value; }
    public void score(double value) { score = Math.max(0, value); }
    public void veins(int value) { veins = Math.max(0, value); }
    public void lastAlert(long value) { lastAlert = value; }
    public void lastUpdated(long value) { lastUpdated = value; }
    public void status(CaseStatus value) { status = value; }

    public void addOre(String material, double points, boolean hidden, Location location) {
        score += points;
        veins++;
        lastUpdated = System.currentTimeMillis();
        ores.merge(material, 1, Integer::sum);
        addLimited(locations, location.getWorld().getName() + " " + location.getBlockX() + ", "
            + location.getBlockY() + ", " + location.getBlockZ() + " - " + material, 15);
        addLimited(timeline, Instant.now() + " | " + material + " | hidden=" + hidden
            + " | score=" + String.format("%.2f", score), 50);
    }

    public void addTimeline(String value) {
        addLimited(timeline, Instant.now() + " | " + value, 50);
    }

    public void decay(double perHour) {
        long now = System.currentTimeMillis();
        double hours = Math.max(0, now - lastUpdated) / 3_600_000.0;
        if (hours > 0 && perHour > 0) score(Math.max(0, score - hours * perHour));
        lastUpdated = now;
    }

    public void reset() {
        score = 0;
        veins = 0;
        lastAlert = 0;
        lastUpdated = System.currentTimeMillis();
        status = CaseStatus.CLEARED;
        ores.clear();
        locations.clear();
        timeline.clear();
    }

    private static void addLimited(Deque<String> deque, String value, int limit) {
        deque.addFirst(value);
        while (deque.size() > limit) deque.removeLast();
    }
}
