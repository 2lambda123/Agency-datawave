package datawave.microservice.query.executor;

import datawave.microservice.common.storage.QueryCheckpoint;
import datawave.microservice.common.storage.QueryKey;
import datawave.microservice.common.storage.QueryQueueManager;
import datawave.microservice.common.storage.QueryStatus;
import datawave.microservice.common.storage.QueryStorageCache;
import datawave.microservice.common.storage.QueryStorageLock;
import datawave.microservice.common.storage.QueryTask;
import datawave.microservice.common.storage.QueryTaskNotification;
import datawave.microservice.common.storage.Result;
import datawave.microservice.common.storage.TaskKey;
import datawave.microservice.common.storage.TaskLockException;
import datawave.microservice.common.storage.TaskStates;
import datawave.microservice.common.storage.remote.QueryTaskNotificationHandler;
import datawave.microservice.query.configuration.GenericQueryConfiguration;
import datawave.microservice.query.executor.config.ExecutorProperties;
import datawave.microservice.query.logic.CheckpointableQueryLogic;
import datawave.microservice.query.logic.QueryLogic;
import datawave.microservice.query.logic.QueryLogicFactory;
import datawave.webservice.query.Query;
import datawave.webservice.query.QueryImpl;
import org.apache.accumulo.core.client.Connector;
import org.apache.commons.collections4.iterators.TransformIterator;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.UUID;

/**
 * This class holds the business logic for handling a task notification
 *
 * TODO: Query Metrics
 **/
public class QueryExecutor implements QueryTaskNotificationHandler {
    private static final Logger log = Logger.getLogger(QueryExecutor.class);
    
    private final Connector connector;
    private final QueryStorageCache cache;
    private final QueryQueueManager queues;
    private final QueryLogicFactory queryLogicFactory;
    private final ExecutorProperties executorProperties;
    
    public QueryExecutor(ExecutorProperties executorProperties, Connector connector, QueryStorageCache cache, QueryQueueManager queues,
                    QueryLogicFactory queryLogicFactory) {
        this.executorProperties = executorProperties;
        this.cache = cache;
        this.queues = queues;
        this.connector = connector;
        this.queryLogicFactory = queryLogicFactory;
    }
    
    @Override
    public void handleQueryTaskNotification(QueryTaskNotification taskNotification) {
        boolean gotLock = false;
        boolean taskComplete = false;
        boolean taskFailed = false;
        TaskKey taskKey = taskNotification.getTaskKey();
        UUID queryId = taskKey.getQueryId();
        try {
            // pull the task out of the cache, locking it in the process
            QueryTask task = cache.getTask(taskKey, executorProperties.getLockWaitTimeMillis(), executorProperties.getLockLeaseTimeMillis());
            if (task != null) {
                // check the states to see if we can run this now
                gotLock = cache.updateTaskState(taskKey, TaskStates.TASK_STATE.RUNNING);
                
                // only proceed if we got the lock
                if (gotLock) {
                    // pull the query from the cache
                    QueryStatus queryStatus = cache.getQueryStatus(queryId);
                    QueryLogic<?> queryLogic;
                    switch (task.getAction()) {
                        case PLAN:
                            queryLogic = queryLogicFactory.getQueryLogic(queryStatus.getQuery().getQueryLogicName());
                            // by default we will expand the fields but not the values.
                            boolean expandFields = true;
                            boolean expandValues = false;
                            Query query = queryStatus.getQuery();
                            for (QueryImpl.Parameter p : query.getParameters()) {
                                if (p.getParameterName().equals(QueryTask.EXPAND_FIELDS)) {
                                    expandFields = Boolean.valueOf(p.getParameterValue());
                                } else if (p.getParameterName().equals(QueryTask.EXPAND_VALUES)) {
                                    expandValues = Boolean.valueOf(p.getParameterValue());
                                }
                            }
                            String plan = queryLogic.getPlan(connector, queryStatus.getQuery(), queryStatus.getCalculatedAuthorizations(), expandFields,
                                            expandValues);
                            updatePlan(queryId, plan);
                            break;
                        case CREATE:
                        case DEFINE:
                        case NEXT:
                            queryLogic = queryLogicFactory.getQueryLogic(queryStatus.getQuery().getQueryLogicName());
                            GenericQueryConfiguration config = queryLogic.initialize(connector, queryStatus.getQuery(),
                                            queryStatus.getCalculatedAuthorizations());
                            
                            // update the query status plan
                            if (task.getAction() != QueryTask.QUERY_ACTION.NEXT) {
                                updatePlan(queryId, config.getQueryString());
                            }
                            
                            if (queryLogic instanceof CheckpointableQueryLogic && ((CheckpointableQueryLogic) queryLogic).isCheckpointable()) {
                                CheckpointableQueryLogic cpQueryLogic = (CheckpointableQueryLogic) queryLogic;
                                cpQueryLogic.setupQuery(connector, task.getQueryCheckpoint());
                                
                                if (task.getAction() == QueryTask.QUERY_ACTION.NEXT) {
                                    taskComplete = pullResults(taskKey, queryLogic, queryStatus.getQuery(), false);
                                    if (!taskComplete) {
                                        checkpoint(taskKey.getQueryKey(), cpQueryLogic);
                                        taskComplete = true;
                                    }
                                } else {
                                    checkpoint(taskKey.getQueryKey(), cpQueryLogic);
                                    taskComplete = true;
                                }
                            } else {
                                queryLogic.setupQuery(config);
                                taskComplete = pullResults(taskKey, queryLogic, queryStatus.getQuery(), true);
                                if (!taskComplete) {
                                    throw new IllegalStateException("Expected to have exhausted results.  Something went wrong here");
                                }
                            }
                            break;
                        case CLOSE:
                            taskComplete = true;
                            break;
                        case TEST:
                            // we can ignore this one
                        default:
                            throw new IllegalStateException("Unknown task action: " + task.getAction() + " for " + taskKey);
                    }
                }
            }
        } catch (TaskLockException tle) {
            // somebody is already processing this one
        } catch (Exception e) {
            taskFailed = true;
            QueryStatus status = cache.getQueryStatus(taskKey.getQueryId());
            status.setFailure(e);
            status.setQueryState(QueryStatus.QUERY_STATE.FAILED);
        } finally {
            if (gotLock) {
                if (taskComplete) {
                    cache.updateTaskState(taskKey, TaskStates.TASK_STATE.COMPLETED);
                    try {
                        cache.deleteTask(taskKey);
                    } catch (IOException e) {
                        log.error("We may be leaving an orphaned task: " + taskKey, e);
                    }
                } else if (taskFailed) {
                    cache.updateTaskState(taskKey, TaskStates.TASK_STATE.FAILED);
                    cache.getTaskLock(taskKey).unlock();
                } else {
                    cache.updateTaskState(taskKey, TaskStates.TASK_STATE.READY);
                    cache.getTaskLock(taskKey).unlock();
                }
            } else {
                cache.post(taskNotification);
            }
        }
    }
    
    private boolean shouldGenerateMoreResults(boolean exhaust, TaskKey taskKey, int maxPageSize) {
        int queueSize = queues.getQueueSize(taskKey.getQueryId());
        return (queueSize < (2.5 * maxPageSize));
    }
    
    private void updatePlan(UUID queryId, String plan) {
        QueryStorageLock lock = cache.getQueryStatusLock(queryId);
        lock.lock();
        try {
            QueryStatus queryStatus = cache.getQueryStatus(queryId);
            queryStatus.setPlan(plan);
            cache.updateQueryStatus(queryStatus);
        } finally {
            lock.unlock();
        }
    }
    
    private long incrementNumResultsGenerated(TaskKey taskKey) {
        QueryStorageLock lock = cache.getQueryStatusLock(taskKey.getQueryId());
        lock.lock();
        try {
            QueryStatus status = cache.getQueryStatus(taskKey.getQueryId());
            if (status != null) {
                long numGenerated = status.getNumResultsGenerated() + 1;
                status.setNumResultsGenerated(numGenerated);
                cache.updateQueryStatus(status);
                return numGenerated;
            } else {
                return Integer.MAX_VALUE;
            }
        } finally {
            lock.unlock();
        }
    }
    
    private QueryStatus.QUERY_STATE getQueryState(TaskKey taskKey) {
        QueryStatus status = cache.getQueryStatus(taskKey.getQueryId());
        if (status != null) {
            return status.getQueryState();
        } else {
            return QueryStatus.QUERY_STATE.CLOSED;
        }
    }
    
    private boolean pullResults(TaskKey taskKey, QueryLogic queryLogic, Query settings, boolean exhaustIterator) throws Exception {
        TransformIterator iter = queryLogic.getTransformIterator(settings);
        int pageSize = settings.getPagesize();
        if (queryLogic.getMaxPageSize() != 0) {
            pageSize = Math.min(pageSize, queryLogic.getMaxPageSize());
        }
        boolean running = shouldGenerateMoreResults(exhaustIterator, taskKey, pageSize);
        while (running && iter.hasNext()) {
            QueryStatus.QUERY_STATE queryState = getQueryState(taskKey);
            // if we are canceled, then break out
            if (queryState == QueryStatus.QUERY_STATE.CANCELED || queryState == QueryStatus.QUERY_STATE.CLOSED) {
                log.info("Query has been cancelled, aborting query.next call");
                // TODO this.getMetric().setLifecycle(QueryMetric.Lifecycle.CANCELLED);
                break;
            }
            
            Object result = iter.next();
            queues.sendMessage(taskKey.getQueryId(), new Result(UUID.randomUUID().toString(), new Object[] {result}));
            
            // regardless whether the transform iterator returned a result, it may have updated the metrics (next/seek calls etc.)
            // TODO
            // if (iter.getTransformer() instanceof WritesQueryMetrics) {
            // ((WritesQueryMetrics) iter.getTransformer()).writeQueryMetrics(this.getMetric());
            // }
            
            running = shouldGenerateMoreResults(exhaustIterator, taskKey, pageSize);
        }
        return !iter.hasNext();
    }
    
    /**
     * Checkpoint a query logic
     * 
     * @param queryKey
     * @param cpQueryLogic
     * @throws IOException
     */
    private void checkpoint(QueryKey queryKey, CheckpointableQueryLogic cpQueryLogic) throws IOException {
        for (QueryCheckpoint cp : cpQueryLogic.checkpoint(queryKey)) {
            cache.checkpointTask(new TaskKey(UUID.randomUUID(), queryKey), cp);
        }
    }
    
}