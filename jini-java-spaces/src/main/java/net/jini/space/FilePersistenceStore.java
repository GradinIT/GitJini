package net.jini.space;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple file-based persistence store using Java Serialization.
 * It stores each entry in its own file within a directory.
 */
public class FilePersistenceStore implements BasicJavaSpace.PersistenceStore {
    private final File directory;

    public FilePersistenceStore(File directory) {
        this.directory = directory;
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new RuntimeException("Failed to create persistence directory: " + directory);
            }
        }
    }

    @Override
    public void write(UUID id, net.jini.core.entry.Entry entry, long expiration) {
        File file = new File(directory, id.toString() + ".ser");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(new BasicJavaSpace.StoredEntry(entry, expiration));
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist entry " + id, e);
        }
    }

    @Override
    public void remove(UUID id) {
        File file = new File(directory, id.toString() + ".ser");
        if (file.exists()) {
            if (!file.delete()) {
                // In a more robust implementation, we might retry or log a warning
            }
        }
    }

    @Override
    public Map<UUID, BasicJavaSpace.StoredEntry> loadAll() {
        Map<UUID, BasicJavaSpace.StoredEntry> map = new ConcurrentHashMap<>();
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".ser"));
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                UUID id = UUID.fromString(name.substring(0, name.length() - 4));
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                    BasicJavaSpace.StoredEntry stored = (BasicJavaSpace.StoredEntry) ois.readObject();
                    map.put(id, stored);
                } catch (IOException | ClassNotFoundException e) {
                    // Log and skip or handle error
                    System.err.println("[DEBUG_LOG] Failed to load entry from " + file + ": " + e.getMessage());
                }
            }
        }
        return map;
    }
}
