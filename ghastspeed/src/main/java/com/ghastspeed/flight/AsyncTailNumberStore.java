package com.ghastspeed.flight;

import com.ghastspeed.GhostSpeed;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/** Coalesces full YAML rewrites and keeps disk I/O off Folia region threads. */
final class AsyncTailNumberStore {

    private final GhostSpeed plugin;
    private final File file;
    private final Supplier<Map<UUID, String>> snapshotSupplier;
    private final AtomicBoolean dirty = new AtomicBoolean();
    private final AtomicBoolean scheduled = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final ReentrantLock fileLock = new ReentrantLock();

    AsyncTailNumberStore(GhostSpeed plugin, File file, Supplier<Map<UUID, String>> snapshotSupplier) {
        this.plugin = plugin;
        this.file = file;
        this.snapshotSupplier = snapshotSupplier;
    }

    void requestSave() {
        if (closed.get()) {
            return;
        }
        dirty.set(true);
        scheduleIfNeeded();
    }

    void close() {
        closed.set(true);
        dirty.set(false);
        writeSnapshot();
    }

    private void scheduleIfNeeded() {
        if (!scheduled.compareAndSet(false, true)) {
            return;
        }
        plugin.getServer().getAsyncScheduler().runDelayed(
                plugin, task -> flushAsync(), 1L, TimeUnit.SECONDS);
    }

    private void flushAsync() {
        try {
            while (!closed.get() && dirty.getAndSet(false)) {
                writeSnapshot();
            }
        } finally {
            scheduled.set(false);
            if (!closed.get() && dirty.get()) {
                scheduleIfNeeded();
            }
        }
    }

    private void writeSnapshot() {
        fileLock.lock();
        try {
            Map<UUID, String> snapshot = Map.copyOf(snapshotSupplier.get());
            YamlConfiguration output = new YamlConfiguration();
            snapshot.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> output.set(entry.getKey().toString(), entry.getValue()));
            output.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("无法保存 jijia.yml：" + exception.getMessage());
        } finally {
            fileLock.unlock();
        }
    }
}
