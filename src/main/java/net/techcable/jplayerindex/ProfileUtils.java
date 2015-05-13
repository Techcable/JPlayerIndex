/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2015 Techcable
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.techcable.jplayerindex;
 
import java.lang.ref.SoftReference;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Utilities to lookup player names and uuids from mojang
 * This caches results so you won't have issues with the rate limit
 * 
 * <p>
 * <b>DONT Rely on Bukkit.getOfflinePlayer()</b>
 * It doesn't cache and is a workaround solution
 * 
 * evilmidgets fetchers are a fair solution, but they don't cache so you can run into ratelimits
 * 
 * @author Techcable
 */
public class ProfileUtils {
    private ProfileUtils() {}

    /**
     * Lookup the profiles with the given names
     * 
     * The reuturned player profile doesn't include properties
     * If properties are needed, proceed to use a uuid lookup
     * 
     * @param name look for a profile with this name
     * @return a profile with the given name
     */
    public static Collection<PlayerProfile> lookup(Collection<String> names) {
        List<PlayerProfile> response = postNames(names.toArray(new String[names.size()]));
        if (response == null) return new ArrayList<>();
        return response;
    }
    
    /**
     * Lookup a profile with the given uuid
     * 
     * The reuturned player profile may or may not include properties
     * 
     * @param id look for a profile with this uuid
     * @return a profile with the given id
     */
    public static PlayerProfile lookup(UUID id) {
        return lookupProperties(id);
    }
    
    /**
     * Lookup the players properties
     * 
     * @param id player to lookup
     * 
     * @return the player's profile with properties
     */
    public static PlayerProfile lookupProperties(UUID id) {
        Object rawResponse = getJson("https://sessionserver.mojang.com/session/minecraft/profile/" + id.toString().replace("-", ""));
        if (rawResponse == null || !(rawResponse instanceof JSONObject)) return null;
        JSONObject response = (JSONObject) rawResponse;
        PlayerProfile profile = deserializeProfile(response);
        if (profile == null) return null;
        return profile;
    }
    
    
    private static List<PlayerProfile> postNames(String[] names) { //This one doesn't cache
        JSONArray request = new JSONArray();
        for (String name : names) {
            request.add(name);
        }
        Object rawResponse = postJson("https://api.mojang.com/profiles/minecraft", request);
        if (!(rawResponse instanceof JSONArray)) return null;
        JSONArray response = (JSONArray) rawResponse;
        List<PlayerProfile> profiles = new ArrayList<>();
        for (Object rawEntry : response) {
            if (!(rawEntry instanceof JSONObject)) return null;
            JSONObject entry = (JSONObject) rawEntry;
            PlayerProfile profile = deserializeProfile(entry);
            if (profile != null) profiles.add(profile);
        }
        return profiles;
    }
    
    //Json Serialization
    
    private static PlayerProfile deserializeProfile(JSONObject json) {
        if (!json.containsKey("name") || !json.containsKey("id")) return null;
        if (!(json.get("name") instanceof String) || !(json.get("id") instanceof String)) return null;
        String name = (String) json.get("name");
        UUID id = toUUID((String)json.get("id"));
        if (id == null) return null;
        PlayerProfile profile = new PlayerProfile(id, name);
        if (json.containsKey("properties") && json.get("properties") instanceof JSONArray) {
            profile.properties = (JSONArray) json.get("properties");
        }
        return profile;
    }
    
    //Utilities
    
    private static String toString(UUID id) {
        return id.toString().replace("-", "");
    }
    
    private static UUID toUUID(String raw) {
        String dashed;
        if (raw.length() == 32) {
            dashed = raw.substring(0, 8) + "-" + raw.substring(8, 12) + "-" + raw.substring(12, 16) + "-" + raw.substring(16, 20) + "-" + raw.substring(20, 32);
        } else {
            dashed = raw;
        }
        return UUID.fromString(raw);
    }
    
    private static JSONParser PARSER = new JSONParser();
    
    private static Object getJson(String rawUrl) {
        BufferedReader reader = null;
        try {
            URL url = new URL(rawUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuffer result = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) result.append(line);
            return PARSER.parse(result.toString());
        } catch (Exception ex) {
            return null;
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }
    
    private static Object postJson(String url, JSONArray body) {
        String rawResponse = post(url, body.toJSONString());
        if (rawResponse == null) return null;
        try {
            return PARSER.parse(rawResponse);
        } catch (Exception e) {
            return null;
        }
    }
    
    private static String post(String rawUrl, String body) {
        BufferedReader reader = null;
        OutputStream out = null;
        
        try {
            URL url = new URL(rawUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            out = connection.getOutputStream();
            out.write(body.getBytes());
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuffer result = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) result.append(line);
            return result.toString();
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            try {
                if (out != null) out.close();
                if (reader != null) reader.close();
            } catch (IOException ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }
}