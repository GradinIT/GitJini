package net.jini.space;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.jini.core.entry.Entry;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JsonPersistenceStore implements BasicJavaSpace.PersistenceStore {
    private final File directory;
    private final ObjectMapper mapper;

    public JsonPersistenceStore(File directory) {
        this.directory = directory;
        if (!directory.exists()) {
            directory.mkdirs();
        }
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // We need to enable default typing to preserve Entry class information
        this.mapper.activateDefaultTyping(this.mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL);
    }

    @Override
    public void write(UUID id, Entry entry, long expiration) {
        File file = new File(directory, id.toString() + ".json");
        try {
            BasicJavaSpace.StoredEntry stored = new BasicJavaSpace.StoredEntry(entry, expiration);
            mapper.writeValue(file, stored);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to JSON", e);
        }
    }

    @Override
    public void remove(UUID id) {
        File file = new File(directory, id.toString() + ".json");
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    public Map<UUID, BasicJavaSpace.StoredEntry> loadAll() {
        Map<UUID, BasicJavaSpace.StoredEntry> result = new HashMap<>();
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
        long now = System.currentTimeMillis();
        if (files != null) {
            for (File file : files) {
                try {
                    BasicJavaSpace.StoredEntry stored = mapper.readValue(file, BasicJavaSpace.StoredEntry.class);
                    if (stored.expiration > now) {
                        String name = file.getName();
                        String idStr = name.substring(0, name.length() - 5);
                        result.put(UUID.fromString(idStr), stored);
                    } else {
                        file.delete();
                    }
                } catch (IOException e) {
                    // Ignore corrupted or unreadable files
                }
            }
        }
        return result;
    }
}
