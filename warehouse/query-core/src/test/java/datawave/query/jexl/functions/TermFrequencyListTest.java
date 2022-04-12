package datawave.query.jexl.functions;

import com.google.common.collect.Maps;
import datawave.ingest.protobuf.TermWeightPosition;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class TermFrequencyListTest {
    
    @Test
    public void testDeser() throws IOException, ClassNotFoundException {
        String eventId = "shard\0type\0uid";
        List<TermWeightPosition> positions = new ArrayList<>();
        positions.add(new TermWeightPosition.Builder().setOffset(15).setZeroOffsetMatch(true).build());
        TermFrequencyList.Zone zone = new TermFrequencyList.Zone("BODY", true, eventId);
        TermFrequencyList tfl = new TermFrequencyList(Maps.immutableEntry(zone, positions));
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(tfl);
        oos.flush();
        
        byte[] data = baos.toByteArray();
        
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais);
        TermFrequencyList next = (TermFrequencyList) ois.readObject();
        
        assertTrue(next.zones().contains(zone));
    }
    
}
