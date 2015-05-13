pacakage net.techcable.jplayerindex.request;

import java.util.UUID;

import com.google.common.collect.ImmutableSet;

public class UUIDRequest {
    private String[] names;
    
    public ImmutableSet<String> getNames() {
        return ImmutableSet.copyOf(names);
    }
}