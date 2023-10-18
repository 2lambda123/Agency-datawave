package datawave.query.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.accumulo.core.data.Key;
import org.apache.commons.collections.keyvalue.UnmodifiableMapEntry;
import org.junit.Test;

import datawave.marking.MarkingFunctions;
import datawave.query.Constants;
import datawave.query.attributes.Attributes;
import datawave.query.attributes.Document;
import datawave.query.attributes.Numeric;

public class RenameTransformTest {

    @Test
    public void renameFieldMapTest() throws MarkingFunctions.Exception {
        Key key = new Key("shard", "dataType" + Constants.NULL + "uid");

        Map<String,String> fieldMap = new HashMap<>();

        fieldMap.put("field2", "field3");
        fieldMap.put("field1", "field3");

        Document d = new Document();
        d.put("field1", new Numeric("1", key, true));
        d.put("field2", new Numeric("2", key, true));

        DocumentTransform transformer = new FieldRenameTransform(fieldMap, false, false);

        Map.Entry<Key,Document> transformed = transformer.apply(new UnmodifiableMapEntry(key, d));
        assertTrue(transformed.getValue() == d);

        assertTrue(d.containsKey("field3"));
        assertFalse(d.containsKey("field2"));
        assertFalse(d.containsKey("field1"));
        assertTrue(d.get("field3") instanceof Attributes);
        assertEquals(2, d.get("field3").size());
    }

    @Test
    public void renameFieldMapPreexistingTest() throws MarkingFunctions.Exception {
        Key key = new Key("shard", "dataType" + Constants.NULL + "uid");

        Map<String,String> fieldMap = new HashMap<>();

        fieldMap.put("field2", "field3");
        fieldMap.put("field1", "field3");

        Document d = new Document();
        d.put("field1", new Numeric("1", key, true));
        d.put("field2", new Numeric("2", key, true));
        d.put("field3", new Numeric("3", key, true));

        DocumentTransform transformer = new FieldRenameTransform(fieldMap, false, false);

        Map.Entry<Key,Document> transformed = transformer.apply(new UnmodifiableMapEntry(key, d));
        assertTrue(transformed.getValue() == d);

        assertTrue(d.containsKey("field3"));
        assertFalse(d.containsKey("field2"));
        assertFalse(d.containsKey("field1"));
        assertTrue(d.get("field3") instanceof Attributes);
        assertEquals(3, d.get("field3").size());
    }
}
