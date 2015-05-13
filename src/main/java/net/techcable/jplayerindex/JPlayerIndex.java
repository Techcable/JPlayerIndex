package net.techcable.jplayerindex;

import lombok.*;

import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuiler;
import com.google.gson.JSONArray;
import com.google.gson.JsonDeserializer;
import com.google.gson.JSONObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializer;

import net.techcable.jplayerindex.request.UUIDRequest;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static spark.Spark.*;
import static spark.SparkBase.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JPlayerIndex {
    private static final Database database = new Database();
    private static final Gson gson = buildGson();
    private static final JsonParser parser = new JsonParser();
    public static void main(String[] args) {
        post("/uuid/", (request, response) -> {
            JSONObject obj = new JSONObject();
            obj.put((JSONArray)parser.parse(request.body()));
            UUIDRequest request = gson.fromJson(obj, UUIDRequest.class);
            Set<UUID> ids = ProfileUtils.stream().map((profile) -> profile.getId()).collect(Collectors.toSet());
            UUIDResponse response = new UUIDResponse(ids);
            return gson.toJson(response.getIds());
        });
    }
    
    private static Gson buildGson() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(UUID.class, (JsonDeserializer)(json, typeOfT, context) -> UUID.fromString(json.getAsString());
        builder.registerTypeAdapter(UUID.class, (JsonSerializer<UUID>)(id, typeOfT, context) -> return id.toString());
        return builder.create();
    }
}