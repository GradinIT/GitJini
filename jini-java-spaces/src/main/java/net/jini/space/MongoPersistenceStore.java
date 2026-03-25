package net.jini.space;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import net.jini.core.entry.Entry;
import org.bson.Document;
import org.bson.types.Binary;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MongoPersistenceStore implements BasicJavaSpace.PersistenceStore {
    private final MongoClient mongoClient;
    private final MongoCollection<Document> collection;

    public MongoPersistenceStore(String connectionString, String dbName, String collectionName) {
        this.mongoClient = MongoClients.create(connectionString);
        MongoDatabase database = mongoClient.getDatabase(dbName);
        this.collection = database.getCollection(collectionName);
    }

    @Override
    public void write(UUID id, Entry entry, long expiration) {
        try {
            byte[] data = serialize(entry);
            Document doc = new Document("_id", id.toString())
                    .append("data", new Binary(data))
                    .append("expiration", expiration);
            collection.replaceOne(Filters.eq("_id", id.toString()), doc, new ReplaceOptions().upsert(true));
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize entry for MongoDB", e);
        }
    }

    @Override
    public void remove(UUID id) {
        collection.deleteOne(Filters.eq("_id", id.toString()));
    }

    @Override
    public Map<UUID, BasicJavaSpace.StoredEntry> loadAll() {
        Map<UUID, BasicJavaSpace.StoredEntry> result = new HashMap<>();
        long now = System.currentTimeMillis();
        for (Document doc : collection.find()) {
            String idStr = doc.getString("_id");
            UUID id = UUID.fromString(idStr);
            long expiration = doc.getLong("expiration");
            if (expiration > now) {
                Binary binary = doc.get("data", Binary.class);
                try {
                    Entry entry = deserialize(binary.getData());
                    result.put(id, new BasicJavaSpace.StoredEntry(entry, expiration));
                } catch (IOException | ClassNotFoundException e) {
                    // Skip entries that cannot be deserialized
                }
            } else {
                remove(id);
            }
        }
        return result;
    }

    public void close() {
        mongoClient.close();
    }

    private byte[] serialize(Object obj) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
            return bos.toByteArray();
        }
    }

    private Entry deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (Entry) ois.readObject();
        }
    }
}
