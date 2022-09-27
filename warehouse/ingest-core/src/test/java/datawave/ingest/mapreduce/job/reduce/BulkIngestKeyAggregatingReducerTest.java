package datawave.ingest.mapreduce.job.reduce;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import datawave.ingest.mapreduce.job.BulkIngestKey;
import datawave.ingest.mapreduce.job.writer.BulkContextWriter;
import datawave.ingest.mapreduce.job.writer.ContextWriter;
import datawave.ingest.metric.IngestOutput;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.Combiner;
import org.apache.accumulo.core.iterators.IteratorEnvironment;
import org.apache.accumulo.core.iterators.SortedKeyValueIterator;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.TaskAttemptID;
import org.apache.hadoop.mapreduce.TaskID;
import org.apache.hadoop.mapreduce.TaskInputOutputContext;
import org.apache.hadoop.mapreduce.TaskType;
import org.apache.hadoop.mapreduce.counters.GenericCounter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static datawave.ingest.mapreduce.job.reduce.AggregatingReducer.MILLISPERDAY;
import static datawave.ingest.mapreduce.job.reduce.AggregatingReducer.USE_AGGREGATOR_PROPERTY;
import static datawave.ingest.mapreduce.job.reduce.BulkIngestKeyAggregatingReducer.CONTEXT_WRITER_CLASS;
import static datawave.ingest.mapreduce.job.reduce.BulkIngestKeyAggregatingReducer.CONTEXT_WRITER_OUTPUT_TABLE_COUNTERS;
import static datawave.ingest.mapreduce.job.reduce.BulkIngestKeyAggregatingReducer.VERBOSE_COUNTERS;
import static datawave.ingest.mapreduce.job.reduce.BulkIngestKeyAggregatingReducer.VERBOSE_PARTITIONING_COUNTERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@Disabled
@ExtendWith(MockitoExtension.class)
public class BulkIngestKeyAggregatingReducerTest {
    
    private Set<String> tables = ImmutableSet.of("table1", "table2", "table3", "table4");
    private Map<String,String> confMap;
    private Multimap<BulkIngestKey,Value> output;
    private Multimap<BulkIngestKey,Value> expected;
    private BulkIngestKeyAggregatingReducer<BulkIngestKey,Value> reducer;
    
    @Mock
    private Configuration conf;
    private ContextWriter<Key,Value> contextWriter;
    private TaskInputOutputContext<BulkIngestKey,Value,BulkIngestKey,Value> context;
    private Random rand = new Random();
    
    private Counter duplicateKey;
    private Counter r1Counter;
    private Counter r2Counter;
    private Counter r3Counter;
    private Counter tab1Counter;
    private Counter tab2Counter;
    private Counter tab3Counter;
    private Counter combinerCounter;
    private Counter dupCounter;
    
    private int expectedDuplicateKey;
    private int expectedR1Counter;
    private int expectedR2Counter;
    private int expectedR3Counter;
    private int expectedTab1Counter;
    private int expectedTab2Counter;
    private int expectedTab3Counter;
    private int expectedCombinerCounter;
    private int expectedDupCounter;
    
    @Mock
    private TaskID taskID;
    
    @Mock
    private TaskAttemptID taskAttemptID;
    
    private enum ExpectedValueType {
        NO_VALUE, FIRST_VALUE, COMBINED_VALUES, ALL_VALUES
    }
    
    @BeforeEach
    public void setup() throws Exception {
        confMap = new HashMap();
        expected = ArrayListMultimap.create();
        output = ArrayListMultimap.create();
        duplicateKey = (Counter) new GenericCounter();
        duplicateKey = (Counter) new GenericCounter();
        r1Counter = (Counter) new GenericCounter();
        r2Counter = (Counter) new GenericCounter();
        r3Counter = (Counter) new GenericCounter();
        tab1Counter = (Counter) new GenericCounter();
        tab2Counter = (Counter) new GenericCounter();
        tab3Counter = (Counter) new GenericCounter();
        combinerCounter = (Counter) new GenericCounter();
        dupCounter = (Counter) new GenericCounter();
        
        expectedDuplicateKey = 0;
        expectedR1Counter = 0;
        expectedR2Counter = 0;
        expectedR3Counter = 0;
        expectedTab1Counter = 0;
        expectedTab2Counter = 0;
        expectedTab3Counter = 0;
        expectedCombinerCounter = 0;
        expectedDupCounter = 0;
        
        when(conf.iterator()).thenReturn(confMap.entrySet().iterator());
        when(conf.getBoolean(Mockito.eq(CONTEXT_WRITER_OUTPUT_TABLE_COUNTERS), Mockito.eq(false))).thenReturn(false);
        Mockito.doReturn(BulkContextWriter.class).when(conf).getClass(Mockito.eq(CONTEXT_WRITER_CLASS), Mockito.any(), Mockito.any());
        
        // PowerMockito.mockStatic(TableConfigurationUtil.class, new Class[0]);
        // when(TableConfigurationUtil.getTables((Configuration) Mockito.any(Configuration.class))).thenReturn(tables);
        //
        // context = (TaskInputOutputContext<BulkIngestKey,Value,BulkIngestKey,Value>) PowerMockito.mock(TaskInputOutputContext.class);
        // Mockito.doAnswer(invocation -> {
        // BulkIngestKey k = invocation.getArgument(0);
        // Value v = invocation.getArgument(1);
        // output.put(k, v);
        // return null;
        // }).when(context).write(Mockito.any(BulkIngestKey.class), Mockito.any(Value.class));
        when(context.getCounter(IngestOutput.DUPLICATE_KEY)).thenReturn(duplicateKey);
        
        reducer = new BulkIngestKeyAggregatingReducer<>();
    }
    
    private void setupVerboseCounters() {
        tab1Counter = (Counter) new GenericCounter();
        tab2Counter = (Counter) new GenericCounter();
        tab3Counter = (Counter) new GenericCounter();
        
        when(conf.getBoolean(Mockito.eq(VERBOSE_COUNTERS), Mockito.eq(false))).thenReturn(true);
        when(context.getCounter(Mockito.eq("TABLE.KEY.VALUElen"), Mockito.startsWith("table1 reducer"))).thenReturn(tab1Counter);
        when(context.getCounter(Mockito.eq("TABLE.KEY.VALUElen"), Mockito.startsWith("table2 reducer"))).thenReturn(tab2Counter);
        when(context.getCounter(Mockito.eq("TABLE.KEY.VALUElen"), Mockito.startsWith("table3 reducer"))).thenReturn(tab3Counter);
    }
    
    private void setupVerbosePartitioningCounters() {
        r1Counter = (Counter) new GenericCounter();
        r2Counter = (Counter) new GenericCounter();
        r3Counter = (Counter) new GenericCounter();
        tab1Counter = (Counter) new GenericCounter();
        tab2Counter = (Counter) new GenericCounter();
        tab3Counter = (Counter) new GenericCounter();
        
        when(conf.getBoolean(Mockito.eq(VERBOSE_PARTITIONING_COUNTERS), Mockito.eq(false))).thenReturn(true);
        
        when(context.getCounter(Mockito.eq("REDUCER 1"), Mockito.startsWith("TABLE table"))).thenReturn(r1Counter);
        when(context.getCounter(Mockito.eq("REDUCER 2"), Mockito.startsWith("TABLE table"))).thenReturn(r2Counter);
        when(context.getCounter(Mockito.eq("REDUCER 3"), Mockito.startsWith("TABLE table"))).thenReturn(r3Counter);
        
        when(context.getCounter(Mockito.eq("TABLE table1"), Mockito.startsWith("REDUCER"))).thenReturn(tab1Counter);
        when(context.getCounter(Mockito.eq("TABLE table2"), Mockito.startsWith("REDUCER"))).thenReturn(tab2Counter);
        when(context.getCounter(Mockito.eq("TABLE table3"), Mockito.startsWith("REDUCER"))).thenReturn(tab3Counter);
        
        when(taskAttemptID.getTaskID()).thenReturn(taskID);
        when(taskAttemptID.getTaskType()).thenReturn(TaskType.REDUCE);
        
        when(context.getTaskAttemptID()).thenReturn(taskAttemptID);
        
    }
    
    private void setupTimestampDedup() {
        when(conf.getBoolean(Mockito.eq("table1" + USE_AGGREGATOR_PROPERTY), Mockito.eq(true))).thenReturn(true);
        when(conf.getBoolean(Mockito.eq("table2" + USE_AGGREGATOR_PROPERTY), Mockito.eq(true))).thenReturn(true);
        when(conf.getBoolean(Mockito.eq("table3" + USE_AGGREGATOR_PROPERTY), Mockito.eq(true))).thenReturn(true);
        when(context.getCounter(IngestOutput.TIMESTAMP_DUPLICATE)).thenReturn(dupCounter);
        
        reducer.TSDedupTables.addAll(Arrays.asList(new Text("table1"), new Text("table2"), new Text("table3")));
        confMap.put("combiner.table1", "");
        confMap.put("combiner.table1.1.iterClazz", "datawave.ingest.mapreduce.job.reduce.BulkIngestKeyAggregatingReducerTest$testCombiner");
        confMap.put("combiner.table2", "");
        confMap.put("combiner.table2.1.iterClazz", "datawave.ingest.mapreduce.job.reduce.BulkIngestKeyAggregatingReducerTest$testCombiner");
        Mockito.doAnswer(invocation -> {
            Iterator<Map.Entry<String,String>> iter = confMap.entrySet().iterator();
            return iter;
        }).when(conf).iterator();
        
    }
    
    private void setupUsingCombiner() {
        combinerCounter = new GenericCounter();
        
        when(conf.getBoolean(Mockito.eq("table1" + USE_AGGREGATOR_PROPERTY), Mockito.eq(true))).thenReturn(true);
        when(conf.getBoolean(Mockito.eq("table2" + USE_AGGREGATOR_PROPERTY), Mockito.eq(true))).thenReturn(true);
        when(conf.getBoolean(Mockito.eq("table3" + USE_AGGREGATOR_PROPERTY), Mockito.eq(true))).thenReturn(true);
        when(conf.getBoolean(Mockito.eq(BulkIngestKeyDedupeCombiner.USING_COMBINER), Mockito.eq(false))).thenReturn(true);
        when(context.getCounter(IngestOutput.MERGED_VALUE)).thenReturn(combinerCounter);
        
        confMap.put("combiner.table1", "");
        confMap.put("combiner.table1.1.iterClazz", "datawave.ingest.mapreduce.job.reduce.BulkIngestKeyAggregatingReducerTest$testCombiner");
        confMap.put("combiner.table3", "");
        confMap.put("combiner.table3.1.iterClazz", "datawave.ingest.mapreduce.job.reduce.BulkIngestKeyAggregatingReducerTest$testCombiner");
        Mockito.doAnswer(invocation -> {
            Iterator<Map.Entry<String,String>> iter = confMap.entrySet().iterator();
            return iter;
        }).when(conf).iterator();
    }
    
    private void checkCounterValues() {
        assertEquals(expectedDuplicateKey, duplicateKey.getValue());
        assertEquals(expectedR1Counter, r1Counter.getValue());
        assertEquals(expectedR2Counter, r2Counter.getValue());
        assertEquals(expectedR3Counter, r3Counter.getValue());
        assertEquals(expectedTab1Counter, tab1Counter.getValue());
        assertEquals(expectedTab2Counter, tab2Counter.getValue());
        assertEquals(expectedTab3Counter, tab3Counter.getValue());
        assertEquals(expectedCombinerCounter, combinerCounter.getValue());
    }
    
    @Test
    public void testDedupKeysOneTable() throws Exception {
        when(conf.getBoolean(Mockito.eq("table1" + USE_AGGREGATOR_PROPERTY), Mockito.eq(true))).thenReturn(true);
        reducer.setup(conf);
        
        performDoReduce("table1", "r1", 4, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table1", "r2", 3, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table1", "r3", 1, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table1", "r4", 0, ExpectedValueType.ALL_VALUES);
        performDoReduce("table2", "r1", 2, ExpectedValueType.ALL_VALUES);
        performDoReduce("table2", "r2", 5, ExpectedValueType.ALL_VALUES);
        performDoReduce("table2", "r2", 3, ExpectedValueType.ALL_VALUES);
        
        expectedDuplicateKey = 2;
        checkCounterValues();
        assertEquals(expected, output);
    }
    
    @Test
    public void testDedupKeysTwoTables() throws Exception {
        when(conf.getBoolean(Mockito.eq("table1" + USE_AGGREGATOR_PROPERTY), Mockito.eq(true))).thenReturn(true);
        when(conf.getBoolean(Mockito.eq("table2" + USE_AGGREGATOR_PROPERTY), Mockito.eq(true))).thenReturn(true);
        reducer.setup(conf);
        
        performDoReduce("table1", "r1", 4, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table1", "r2", 3, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table1", "r3", 1, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table2", "r1", 2, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table2", "r2", 0, ExpectedValueType.ALL_VALUES);
        performDoReduce("table2", "r3", 3, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table3", "r2", 3, ExpectedValueType.ALL_VALUES);
        
        expectedDuplicateKey = 4;
        checkCounterValues();
        assertEquals(expected, output);
    }
    
    @Test
    public void testVerboseCounters() throws Exception {
        setupVerboseCounters();
        reducer.setup(conf);
        
        performDoReduce("table1", "r1", 4, ExpectedValueType.ALL_VALUES);
        performDoReduce("table1", "r2", 3, ExpectedValueType.ALL_VALUES);
        performDoReduce("table1", "r3", 1, ExpectedValueType.ALL_VALUES);
        performDoReduce("table2", "r1", 2, ExpectedValueType.ALL_VALUES);
        performDoReduce("table2", "r2", 0, ExpectedValueType.ALL_VALUES);
        performDoReduce("table2", "r3", 3, ExpectedValueType.ALL_VALUES);
        performDoReduce("table3", "r2", 3, ExpectedValueType.ALL_VALUES);
        
        expectedTab1Counter = 8;
        expectedTab2Counter = 5;
        expectedTab3Counter = 3;
        checkCounterValues();
        assertEquals(expected, output);
    }
    
    @Test
    public void testDedupKeysWithVerboseCounters() throws Exception {
        setupVerboseCounters();
        when(conf.getBoolean(Mockito.eq("table1" + USE_AGGREGATOR_PROPERTY), Mockito.eq(true))).thenReturn(true);
        when(conf.getBoolean(Mockito.eq("table2" + USE_AGGREGATOR_PROPERTY), Mockito.eq(true))).thenReturn(true);
        reducer.setup(conf);
        
        performDoReduce("table1", "r1", 4, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table1", "r2", 3, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table1", "r3", 1, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table2", "r1", 2, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table2", "r2", 0, ExpectedValueType.ALL_VALUES);
        performDoReduce("table2", "r3", 3, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table3", "r2", 3, ExpectedValueType.ALL_VALUES);
        
        expectedDuplicateKey = 4;
        expectedTab1Counter = 8;
        expectedTab2Counter = 5;
        expectedTab3Counter = 3;
        checkCounterValues();
        assertEquals(expected, output);
    }
    
    @Test
    public void testVerbosePartitioningCounters() throws Exception {
        setupVerbosePartitioningCounters();
        reducer.setup(conf);
        
        when(taskID.getId()).thenReturn(1);
        performDoReduce("table1", "r1", 4, ExpectedValueType.ALL_VALUES);
        when(taskID.getId()).thenReturn(1);
        performDoReduce("table1", "r2", 3, ExpectedValueType.ALL_VALUES);
        when(taskID.getId()).thenReturn(2);
        performDoReduce("table1", "r3", 1, ExpectedValueType.ALL_VALUES);
        when(taskID.getId()).thenReturn(2);
        performDoReduce("table2", "r1", 2, ExpectedValueType.ALL_VALUES);
        when(taskID.getId()).thenReturn(2);
        performDoReduce("table2", "r2", 0, ExpectedValueType.ALL_VALUES);
        when(taskID.getId()).thenReturn(3);
        performDoReduce("table2", "r3", 3, ExpectedValueType.ALL_VALUES);
        when(taskID.getId()).thenReturn(3);
        performDoReduce("table3", "r2", 3, ExpectedValueType.ALL_VALUES);
        
        expectedR1Counter = 2;
        expectedR2Counter = 3;
        expectedR3Counter = 2;
        expectedTab1Counter = 3;
        expectedTab2Counter = 3;
        expectedTab3Counter = 1;
        checkCounterValues();
        assertEquals(expected, output);
    }
    
    @Test
    public void testDedupKeysWithVerbosePartitioningCounters() throws Exception {
        setupVerbosePartitioningCounters();
        when(conf.getBoolean(Mockito.eq("table1" + USE_AGGREGATOR_PROPERTY), Mockito.eq(true))).thenReturn(true);
        when(conf.getBoolean(Mockito.eq("table2" + USE_AGGREGATOR_PROPERTY), Mockito.eq(true))).thenReturn(true);
        
        reducer.setup(conf);
        
        when(taskID.getId()).thenReturn(1);
        performDoReduce("table1", "r1", 4, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table1", "r2", 3, ExpectedValueType.FIRST_VALUE);
        when(taskID.getId()).thenReturn(2);
        performDoReduce("table1", "r3", 1, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table2", "r1", 2, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table2", "r2", 0, ExpectedValueType.ALL_VALUES);
        when(taskID.getId()).thenReturn(3);
        performDoReduce("table2", "r3", 3, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table3", "r2", 3, ExpectedValueType.ALL_VALUES);
        
        expectedDuplicateKey = 4;
        expectedR1Counter = 2;
        expectedR2Counter = 3;
        expectedR3Counter = 2;
        expectedTab1Counter = 3;
        expectedTab2Counter = 3;
        expectedTab3Counter = 1;
        checkCounterValues();
        assertEquals(expected, output);
    }
    
    @Test
    public void testTimestampDedup() throws Exception {
        setupTimestampDedup();
        reducer.setup(conf);
        
        performDoReduce("table1", "r1", 4, 3 * MILLISPERDAY + MILLISPERDAY / 2, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table1", "r1", 3, 3 * MILLISPERDAY + MILLISPERDAY / 3, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table1", "r2", 3, 3 * MILLISPERDAY + MILLISPERDAY / 2, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table1", "r2", 2, 3 * MILLISPERDAY + MILLISPERDAY / 3, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table1", "r3", 1, 3 * MILLISPERDAY, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table2", "r1", 2, 3 * MILLISPERDAY, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table2", "r2", 0, 3 * MILLISPERDAY + MILLISPERDAY / 2, ExpectedValueType.NO_VALUE);
        performDoReduce("table2", "r2", 0, 3 * MILLISPERDAY + MILLISPERDAY / 3, ExpectedValueType.NO_VALUE);
        performDoReduce("table2", "r3", 3, 3 * MILLISPERDAY, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table2", "r3", 3, 4 * MILLISPERDAY, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table3", "r2", 3, 3 * MILLISPERDAY, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table4", "r2", 4, 3 * MILLISPERDAY, ExpectedValueType.ALL_VALUES);
        
        expectedDuplicateKey = 1;
        expectedDupCounter = 13;
        checkCounterValues();
        assertEquals(expected, output);
    }
    
    @Test
    public void testTimestampDedupWithVerboseCounters() throws Exception {
        setupTimestampDedup();
        setupVerboseCounters();
        reducer.setup(conf);
        
        performDoReduce("table1", "r1", 4, 3 * MILLISPERDAY + MILLISPERDAY / 2, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table1", "r1", 3, 3 * MILLISPERDAY + MILLISPERDAY / 3, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table1", "r2", 3, 3 * MILLISPERDAY + MILLISPERDAY / 2, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table1", "r2", 2, 3 * MILLISPERDAY + MILLISPERDAY / 3, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table1", "r3", 1, 3 * MILLISPERDAY, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table2", "r1", 2, 3 * MILLISPERDAY, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table2", "r2", 0, 3 * MILLISPERDAY + MILLISPERDAY / 2, ExpectedValueType.NO_VALUE);
        performDoReduce("table2", "r2", 0, 3 * MILLISPERDAY + MILLISPERDAY / 3, ExpectedValueType.NO_VALUE);
        performDoReduce("table2", "r3", 3, 3 * MILLISPERDAY, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table2", "r3", 3, 4 * MILLISPERDAY, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table3", "r2", 3, 3 * MILLISPERDAY, ExpectedValueType.FIRST_VALUE);
        
        expectedDuplicateKey = 1;
        expectedDupCounter = 13;
        expectedTab1Counter = 13;
        expectedTab2Counter = 8;
        expectedTab3Counter = 3;
        checkCounterValues();
        assertEquals(expected, output);
    }
    
    @Test
    public void testTimestampDedupWithVerbosePartitioningCounters() throws Exception {
        setupTimestampDedup();
        setupVerbosePartitioningCounters();
        reducer.setup(conf);
        
        when(taskID.getId()).thenReturn(1);
        performDoReduce("table1", "r1", 4, 3 * MILLISPERDAY + MILLISPERDAY / 2, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table1", "r1", 3, 3 * MILLISPERDAY + MILLISPERDAY / 3, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table1", "r2", 3, 3 * MILLISPERDAY + MILLISPERDAY / 2, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table1", "r2", 2, 3 * MILLISPERDAY + MILLISPERDAY / 3, ExpectedValueType.FIRST_VALUE);
        when(taskID.getId()).thenReturn(2);
        performDoReduce("table1", "r3", 1, 3 * MILLISPERDAY, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table2", "r1", 2, 3 * MILLISPERDAY, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table2", "r2", 0, 3 * MILLISPERDAY + MILLISPERDAY / 2, ExpectedValueType.NO_VALUE);
        performDoReduce("table2", "r2", 0, 3 * MILLISPERDAY + MILLISPERDAY / 3, ExpectedValueType.NO_VALUE);
        when(taskID.getId()).thenReturn(3);
        performDoReduce("table2", "r3", 3, 4 * MILLISPERDAY, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table3", "r2", 3, 3 * MILLISPERDAY, ExpectedValueType.FIRST_VALUE);
        
        expectedDupCounter = 13;
        expectedR1Counter = 4;
        expectedR2Counter = 4;
        expectedR3Counter = 2;
        expectedTab1Counter = 5;
        expectedTab2Counter = 4;
        expectedTab3Counter = 1;
        expectedDuplicateKey = 1;
        checkCounterValues();
        assertEquals(expected, output);
    }
    
    @Test
    public void testUsingCombiner() throws Exception {
        setupUsingCombiner();
        reducer.setup(conf);
        
        performDoReduce("table1", "r1", 4, ExpectedValueType.COMBINED_VALUES);
        performDoReduce("table1", "r2", 3, ExpectedValueType.COMBINED_VALUES);
        performDoReduce("table1", "r3", 1, ExpectedValueType.COMBINED_VALUES);
        performDoReduce("table2", "r1", 2, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table2", "r2", 0, ExpectedValueType.ALL_VALUES);
        performDoReduce("table2", "r3", 3, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table3", "r1", 3, ExpectedValueType.COMBINED_VALUES);
        performDoReduce("table3", "r2", 0, ExpectedValueType.COMBINED_VALUES);
        
        expectedDuplicateKey = 2;
        expectedCombinerCounter = 5;
        checkCounterValues();
        assertEquals(expected, output);
    }
    
    @Test
    public void testUsingCombinerWithVerboseCounters() throws Exception {
        setupUsingCombiner();
        setupVerboseCounters();
        reducer.setup(conf);
        
        performDoReduce("table1", "r1", 4, ExpectedValueType.COMBINED_VALUES);
        performDoReduce("table1", "r2", 3, ExpectedValueType.COMBINED_VALUES);
        performDoReduce("table1", "r3", 1, ExpectedValueType.COMBINED_VALUES);
        performDoReduce("table2", "r1", 2, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table2", "r2", 0, ExpectedValueType.ALL_VALUES);
        performDoReduce("table2", "r3", 3, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table3", "r1", 3, ExpectedValueType.COMBINED_VALUES);
        performDoReduce("table3", "r2", 0, ExpectedValueType.COMBINED_VALUES);
        
        expectedDuplicateKey = 2;
        expectedCombinerCounter = 5;
        expectedTab1Counter = 8;
        expectedTab2Counter = 5;
        expectedTab3Counter = 3;
        checkCounterValues();
        assertEquals(expected, output);
    }
    
    @Test
    public void testUsingCombinerWithVerbosePartitioningCounters() throws Exception {
        setupUsingCombiner();
        setupVerbosePartitioningCounters();
        reducer.setup(conf);
        
        when(taskID.getId()).thenReturn(1);
        performDoReduce("table1", "r1", 4, ExpectedValueType.COMBINED_VALUES);
        performDoReduce("table1", "r2", 3, ExpectedValueType.COMBINED_VALUES);
        when(taskID.getId()).thenReturn(2);
        performDoReduce("table1", "r3", 1, ExpectedValueType.COMBINED_VALUES);
        performDoReduce("table2", "r1", 2, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table2", "r2", 0, ExpectedValueType.ALL_VALUES);
        when(taskID.getId()).thenReturn(3);
        performDoReduce("table2", "r3", 3, ExpectedValueType.FIRST_VALUE);
        performDoReduce("table3", "r1", 3, ExpectedValueType.COMBINED_VALUES);
        performDoReduce("table3", "r2", 0, ExpectedValueType.COMBINED_VALUES);
        
        expectedDuplicateKey = 2;
        expectedCombinerCounter = 5;
        expectedR1Counter = 2;
        expectedR2Counter = 3;
        expectedR3Counter = 3;
        expectedTab1Counter = 3;
        expectedTab2Counter = 3;
        expectedTab3Counter = 2;
        checkCounterValues();
        assertEquals(expected, output);
    }
    
    private void performDoReduce(String table, String row, int numberOfValues) throws Exception {
        performDoReduce(table, row, numberOfValues, 1L, ExpectedValueType.FIRST_VALUE);
    }
    
    private void performDoReduce(String table, String row, int numberOfValues, long ts) throws Exception {
        performDoReduce(table, row, numberOfValues, ts, ExpectedValueType.FIRST_VALUE);
    }
    
    private void performDoReduce(String table, String row, int numberOfValues, ExpectedValueType expectedValueType) throws Exception {
        performDoReduce(table, row, numberOfValues, 1L, expectedValueType);
    }
    
    private void performDoReduce(String table, String row, int numberOfValues, long ts, ExpectedValueType expectedValueType) throws Exception {
        Key key = new Key(new Text(row), ts);
        BulkIngestKey bulkIngestKey = new BulkIngestKey(new Text(table), key);
        List<Value> values = new ArrayList<>();
        Value value = new Value(new Text(String.format("%015d", rand.nextInt())));
        if (expectedValueType == ExpectedValueType.FIRST_VALUE) {
            expected.put(bulkIngestKey, value);
        }
        for (int i = 0; i < numberOfValues; i++) {
            values.add(value);
            value = new Value(new Text(String.format("%015d", rand.nextInt())));
        }
        
        if (expectedValueType == ExpectedValueType.COMBINED_VALUES) {
            expected.put(bulkIngestKey, combineValues(values.iterator()));
        } else if (expectedValueType == ExpectedValueType.ALL_VALUES) {
            expected.putAll(bulkIngestKey, values);
        }
        
        reducer.doReduce(bulkIngestKey, values, context);
    }
    
    public static Value combineValues(Iterator<Value> iter) {
        StringBuilder combinedValues = new StringBuilder();
        iter.forEachRemaining(value -> combinedValues.append(value.toString()));
        Value value = new Value(new Text(combinedValues.toString()));
        return value;
    }
    
    public static class testCombiner extends Combiner {
        public testCombiner() {}
        
        @Override
        public void init(SortedKeyValueIterator<Key,Value> source, Map<String,String> options, IteratorEnvironment env) throws IOException {
            
        }
        
        @Override
        public Value reduce(Key key, Iterator<Value> iter) {
            return combineValues(iter);
        }
        
    }
    
}
