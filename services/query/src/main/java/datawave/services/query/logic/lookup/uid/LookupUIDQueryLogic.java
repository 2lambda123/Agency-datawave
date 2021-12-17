package datawave.services.query.logic.lookup.uid;

import datawave.services.query.logic.BaseQueryLogic;
import datawave.services.query.logic.lookup.LookupQueryLogic;
import org.springframework.util.MultiValueMap;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class LookupUIDQueryLogic<T> extends LookupQueryLogic<T> {
    private static final String UID_TERM_SEPARATOR = " ";
    private static final String EVENT_FIELD = "event";
    
    public LookupUIDQueryLogic(BaseQueryLogic<T> delegateQueryLogic) {
        super(delegateQueryLogic);
    }
    
    public LookupUIDQueryLogic(LookupQueryLogic<T> other) throws CloneNotSupportedException {
        super(other);
    }
    
    @Override
    public String createQueryFromLookupTerms(MultiValueMap<String,String> lookupUIDPairs) {
        // @formatter:off
        return lookupUIDPairs
                .entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(value -> String.join(LOOKUP_KEY_VALUE_DELIMITER, entry.getKey(), value)))
                .collect(Collectors.joining(UID_TERM_SEPARATOR));
        // @formatter:on
    }
    
    @Override
    public boolean isEventLookupRequired(MultiValueMap<String,String> lookupTerms) {
        return !(lookupTerms.keySet().size() == 1 && lookupTerms.containsKey(EVENT_FIELD));
    }
    
    @Override
    public Set<String> getContentLookupTerms(MultiValueMap<String,String> lookupTerms) {
        return lookupTerms.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
    }
    
    @Override
    public Object clone() throws CloneNotSupportedException {
        return new LookupUIDQueryLogic<>(this);
    }
}
