package datawave.microservice.query.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.zaxxer.sparsebits.SparseBitSet;
import datawave.microservice.query.remote.QueryRequest;
import datawave.query.config.ShardQueryConfiguration;
import datawave.services.query.logic.QueryCheckpoint;
import datawave.services.query.logic.QueryKey;
import datawave.webservice.query.QueryImpl;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class QueryTaskCheckpointTest {
    @Test
    public void testQueryKey() {
        String queryId = UUID.randomUUID().toString();
        String queryPool = "default";
        String queryLogic = "EventQuery";
        QueryKey key = new QueryKey(queryPool, queryId, queryLogic);
        assertEquals(queryId, key.getQueryId());
        assertEquals(queryPool, key.getQueryPool());
        
        String queryId2 = queryId;
        String queryPool2 = "default";
        String queryLogic2 = "EventQuery";
        QueryKey key2 = new QueryKey(queryPool2, queryId2, queryLogic2);
        assertEquals(key, key2);
        assertEquals(key.hashCode(), key2.hashCode());
        assertEquals(key.toKey(), key2.toKey());
        
        assertTrue(key.toKey().contains(queryId.toString()));
        assertTrue(key.toKey().contains(queryPool.toString()));
        assertTrue(key.toKey().contains(queryLogic));
        
        String otherId = UUID.randomUUID().toString();
        QueryKey otherKey = new QueryKey(queryPool, otherId, queryLogic);
        assertNotEquals(key, otherKey);
        assertNotEquals(key.toKey(), otherKey.toKey());
        
        String otherPool = "other";
        otherKey = new QueryKey(otherPool, queryId, queryLogic);
        assertNotEquals(key, otherKey);
        assertNotEquals(key.toKey(), otherKey.toKey());
        
        String otherLogic = "EdgeQuery";
        otherKey = new QueryKey(queryPool, queryId, otherLogic);
        assertNotEquals(key, otherKey);
        assertNotEquals(key.toKey(), otherKey.toKey());
    }
    
    @Test
    public void getTaskKey() {
        String queryId = UUID.randomUUID().toString();
        String queryPool = "default";
        String queryLogic = "EventQuery";
        int taskId = new Random().nextInt(Integer.MAX_VALUE);
        TaskKey key = new TaskKey(taskId, queryPool, queryId, queryLogic);
        assertEquals(queryId, key.getQueryId());
        assertEquals(queryPool, key.getQueryPool());
        assertEquals(taskId, key.getTaskId());
        
        String queryId2 = queryId;
        String queryPool2 = "default";
        int taskId2 = taskId;
        String queryLogic2 = "EventQuery";
        TaskKey key2 = new TaskKey(taskId2, queryPool2, queryId2, queryLogic2);
        assertEquals(key, key2);
        assertEquals(key.hashCode(), key2.hashCode());
        assertEquals(key.toKey(), key2.toKey());
        
        assertTrue(key.toKey().contains(Integer.toString(taskId)));
        assertTrue(key.toKey().contains(queryId));
        assertTrue(key.toKey().contains(queryPool));
        
        String otherQqueryId = UUID.randomUUID().toString();
        int otherId = new Random().nextInt(Integer.MAX_VALUE);
        String otherPool = "other";
        String otherLogic = "EdgeQuery";
        TaskKey otherKey = new TaskKey(otherId, queryPool, queryId, queryLogic);
        assertNotEquals(key, otherKey);
        assertNotEquals(key.toKey(), otherKey.toKey());
        otherKey = new TaskKey(taskId, otherPool, queryId, queryLogic);
        assertNotEquals(key, otherKey);
        assertNotEquals(key.toKey(), otherKey.toKey());
        otherKey = new TaskKey(taskId + 1, queryPool, queryId, queryLogic);
        assertNotEquals(key, otherKey);
        assertNotEquals(key.toKey(), otherKey.toKey());
        otherKey = new TaskKey(taskId, queryPool, otherQqueryId, queryLogic);
        assertNotEquals(key, otherKey);
        assertNotEquals(key.toKey(), otherKey.toKey());
        otherKey = new TaskKey(taskId, queryPool, queryId, otherLogic);
        assertNotEquals(key, otherKey);
        assertNotEquals(key.toKey(), otherKey.toKey());
    }
    
    @Test
    public void testCheckpoint() {
        String uuid = UUID.randomUUID().toString();
        String queryPool = "default";
        String queryLogic = "EventQuery";
        QueryImpl query = new QueryImpl();
        query.setQueryName("foo");
        query.setQuery("foo == bar");
        ShardQueryConfiguration config = new ShardQueryConfiguration();
        config.setQuery(query);
        QueryCheckpoint qcp = new QueryCheckpoint(queryPool, uuid, queryLogic, config);
        
        assertEquals(queryPool, qcp.getQueryKey().getQueryPool());
        assertEquals(config, qcp.getConfig());
        assertEquals(uuid, qcp.getQueryKey().getQueryId());
        
        String uuid2 = uuid;
        String queryPool2 = "default";
        String queryLogic2 = "EventQuery";
        QueryImpl query2 = new QueryImpl();
        query2.setQueryName("foo");
        query2.setQuery("foo == bar");
        ShardQueryConfiguration config2 = new ShardQueryConfiguration();
        config2.setQuery(query2);
        assertEquals(config, config2);
        QueryCheckpoint qcp2 = new QueryCheckpoint(queryPool2, uuid2, queryLogic2, config2);
        
        assertEquals(qcp, qcp2);
        assertEquals(qcp.hashCode(), qcp2.hashCode());
        
        String otherId = UUID.randomUUID().toString();
        String otherPool = "other";
        String otherLogic = "EdgeQuery";
        QueryImpl otherQuery = new QueryImpl();
        otherQuery.setQueryName("bar");
        ShardQueryConfiguration otherConfig = new ShardQueryConfiguration();
        otherConfig.setQuery(otherQuery);
        QueryCheckpoint otherCp = new QueryCheckpoint(otherPool, uuid, queryLogic, config);
        assertNotEquals(otherCp, qcp);
        otherCp = new QueryCheckpoint(queryPool, otherId, queryLogic, config);
        assertNotEquals(otherCp, qcp);
        otherCp = new QueryCheckpoint(queryPool, uuid, otherLogic, config);
        assertNotEquals(otherCp, qcp);
        otherCp = new QueryCheckpoint(queryPool, uuid, queryLogic, otherConfig);
        assertNotEquals(otherCp, qcp);
    }
    
    @Test
    public void testTask() {
        String uuid = UUID.randomUUID().toString();
        String queryPool = "default";
        String queryLogic = "EventQuery";
        QueryImpl query = new QueryImpl();
        query.setQueryName("foo");
        query.setQuery("foo == bar");
        ShardQueryConfiguration config = new ShardQueryConfiguration();
        config.setQuery(query);
        QueryCheckpoint qcp = new QueryCheckpoint(queryPool, uuid, queryLogic, config);
        QueryTask task = new QueryTask(0, QueryRequest.Method.CREATE, qcp);
        
        assertEquals(QueryRequest.Method.CREATE, task.getAction());
        assertEquals(qcp, task.getQueryCheckpoint());
        
        String uuid2 = uuid;
        String queryPool2 = "default";
        String queryLogic2 = "EventQuery";
        QueryImpl query2 = new QueryImpl();
        query2.setQueryName("foo");
        query2.setQuery("foo == bar");
        ShardQueryConfiguration config2 = new ShardQueryConfiguration();
        config2.setQuery(query2);
        QueryCheckpoint qcp2 = new QueryCheckpoint(queryPool2, uuid2, queryLogic2, config2);
        assertEquals(qcp, qcp2);
        QueryTask task2 = new QueryTask(task.getTaskKey().getTaskId(), QueryRequest.Method.CREATE, qcp2);
        
        assertEquals(task, task2);
        assertEquals(task.hashCode(), task2.hashCode());
        assertEquals(task.getTaskKey(), task2.getTaskKey());
        
        String otherId = UUID.randomUUID().toString();
        QueryCheckpoint otherCp = new QueryCheckpoint(queryPool, otherId, queryLogic, config);
        QueryTask otherTask = new QueryTask(1, QueryRequest.Method.CREATE, qcp);
        assertNotEquals(otherTask, task);
        assertNotEquals(otherTask.getTaskKey(), task.getTaskKey());
        otherTask = new QueryTask(task.getTaskKey().getTaskId(), QueryRequest.Method.NEXT, qcp);
        assertNotEquals(otherTask, task);
        assertEquals(otherTask.getTaskKey(), task.getTaskKey());
        otherTask = new QueryTask(task.getTaskKey().getTaskId(), QueryRequest.Method.CREATE, otherCp);
        assertNotEquals(otherTask, task);
        assertNotEquals(otherTask.getTaskKey(), task.getTaskKey());
    }
    
    @Test
    public void testTaskDescription() throws JsonProcessingException {
        TaskKey key = new TaskKey(0, "default", UUID.randomUUID().toString(), "EventQuery");
        QueryImpl query = new QueryImpl();
        UUID queryId = UUID.randomUUID();
        query.setId(queryId);
        query.setQueryName("foo");
        query.setQuery("foo == bar");
        ShardQueryConfiguration config = new ShardQueryConfiguration();
        config.setQuery(query);
        TaskDescription desc = new TaskDescription(key, config);
        
        assertEquals(key, desc.getTaskKey());
        assertEquals(config, desc.getConfig());
        
        String json = new ObjectMapper().registerModule(new GuavaModule()).writeValueAsString(desc);
        TaskDescription desc2 = new ObjectMapper().registerModule(new GuavaModule()).readerFor(TaskDescription.class).readValue(json);
        assertEquals(desc, desc2);
        assertEquals(desc.hashCode(), desc2.hashCode());
        
        TaskKey key2 = new TaskKey(key.getTaskId(), key.getQueryPool(), key.getQueryId(), key.getQueryLogic());
        QueryImpl query2 = new QueryImpl();
        query2.setId(queryId);
        query2.setQueryName("foo");
        query2.setQuery("foo == bar");
        ShardQueryConfiguration config2 = new ShardQueryConfiguration();
        config2.setQuery(query2);
        desc2 = new TaskDescription(key2, config2);
        
        assertEquals(desc, desc2);
        assertEquals(desc.hashCode(), desc.hashCode());
        
        TaskKey otherKey = new TaskKey(key.getTaskId() + 1, key.getQueryPool(), key.getQueryId(), key.getQueryLogic());
        QueryImpl otherQuery = new QueryImpl();
        otherQuery.setQueryName("bar");
        otherQuery.setQuery("foo == bar");
        ShardQueryConfiguration otherConfig = new ShardQueryConfiguration();
        otherConfig.setQuery(otherQuery);
        TaskDescription otherDesc = new TaskDescription(otherKey, config);
        assertNotEquals(otherDesc, desc);
        otherDesc = new TaskDescription(key, otherConfig);
        assertNotEquals(otherDesc, desc);
    }
    
    @Test
    public void testQueryState() throws JsonProcessingException {
        String uuid = UUID.randomUUID().toString();
        String queryPool = "default";
        String queryLogic = "EventQuery";
        QueryStatus queryStatus = new QueryStatus(new QueryKey(queryPool, uuid, queryLogic));
        TaskStates tasks = new TaskStates(new QueryKey(queryPool, uuid, queryLogic), 10);
        Map<TaskStates.TASK_STATE,SparseBitSet> states = new HashMap<>();
        QueryRequest.Method action = QueryRequest.Method.CREATE;
        states.put(TaskStates.TASK_STATE.READY, new SparseBitSet());
        states.get(TaskStates.TASK_STATE.READY).set(0);
        states.get(TaskStates.TASK_STATE.READY).set(1);
        tasks.setTaskStates(states);
        QueryState state = new QueryState(queryStatus, tasks);
        
        assertEquals(uuid, state.getQueryStatus().getQueryKey().getQueryId());
        assertEquals(queryPool, state.getQueryStatus().getQueryKey().getQueryPool());
        assertEquals(queryLogic, state.getQueryStatus().getQueryKey().getQueryLogic());
        assertEquals(tasks, state.getTaskStates());
        
        String json = new ObjectMapper().writeValueAsString(state);
        QueryState state2 = new ObjectMapper().readerFor(QueryState.class).readValue(json);
        assertEquals(state, state2);
        assertEquals(state.hashCode(), state2.hashCode());
        
        String uuid2 = uuid;
        String queryPool2 = "default";
        String queryLogic2 = "EventQuery";
        QueryStatus queryStatus2 = new QueryStatus(new QueryKey(queryPool2, uuid2, queryLogic2));
        TaskStates tasks2 = new TaskStates(new QueryKey(queryPool, uuid, queryLogic), 10);
        tasks2.setTaskStates(new HashMap<>(states));
        state2 = new QueryState(queryStatus2, tasks2);
        
        assertEquals(state, state2);
        assertEquals(state.hashCode(), state2.hashCode());
        
        String otherId = UUID.randomUUID().toString();
        String otherPool = "other";
        String otherLogic = "EdgeQuery";
        QueryStatus otherProperties = new QueryStatus(new QueryKey(otherPool, otherId, otherLogic));
        TaskStates otherTasks = new TaskStates(new QueryKey(queryPool, uuid, queryLogic), 10);
        Map<TaskStates.TASK_STATE,SparseBitSet> otherStates = new HashMap<>();
        otherStates.put(TaskStates.TASK_STATE.READY, new SparseBitSet());
        otherStates.get(TaskStates.TASK_STATE.READY).set(2);
        otherStates.get(TaskStates.TASK_STATE.READY).set(3);
        otherTasks.setTaskStates(otherStates);
        QueryState otherState = new QueryState(otherProperties, tasks);
        assertNotEquals(otherState, state);
        otherState = new QueryState(queryStatus, otherTasks);
        assertNotEquals(otherState, state);
    }
}