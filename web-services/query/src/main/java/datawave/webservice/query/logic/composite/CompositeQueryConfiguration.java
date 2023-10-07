package datawave.webservice.query.logic.composite;

import datawave.webservice.query.Query;
import datawave.webservice.query.QueryImpl;
import datawave.webservice.query.configuration.GenericQueryConfiguration;

import java.io.Serializable;

public class CompositeQueryConfiguration extends GenericQueryConfiguration implements Serializable {
    
    private Query query = null;
    
    // Specifies whether all queries must succeed initialization
    private boolean allMustInitialize = false;
    
    // Specifies whether queries are run sequentially. We stop after the first query that returns any results.
    private boolean sequentialExecution = false;
    
    public CompositeQueryConfiguration() {
        super();
        query = new QueryImpl();
    }
    
    /**
     * Performs a deep copy of the provided CompositeQueryConfiguration into a new instance
     *
     * @param other
     *            - another CompositeQueryConfiguration instance
     */
    public CompositeQueryConfiguration(CompositeQueryConfiguration other) {
        
        // GenericQueryConfiguration copy first
        super(other);
    }
    
    /**
     * Factory method that instantiates an fresh CompositeQueryConfiguration
     *
     * @return - a clean CompositeQueryConfiguration
     */
    public static CompositeQueryConfiguration create() {
        return new CompositeQueryConfiguration();
    }
    
    /**
     * Factory method that returns a deep copy of the provided CompositeQueryConfiguration
     *
     * @param other
     *            - another instance of a CompositeQueryConfiguration
     * @return - copy of provided CompositeQueryConfiguration
     */
    public static CompositeQueryConfiguration create(CompositeQueryConfiguration other) {
        return new CompositeQueryConfiguration(other);
    }
    
    /**
     * Factory method that creates a CompositeQueryConfiguration deep copy from a CompositeQueryLogic
     *
     * @param compositeQueryLogic
     *            - a configured CompositeQueryLogic
     * @return - a CompositeQueryConfiguration
     */
    public static CompositeQueryConfiguration create(CompositeQueryLogic compositeQueryLogic) {
        
        CompositeQueryConfiguration config = create(compositeQueryLogic.getConfig());
        
        return config;
    }
    
    /**
     * Factory method that creates a CompositeQueryConfiguration from a CompositeQueryLogic and a Query
     *
     * @param compositeQueryLogic
     *            - a configured CompositeQueryLogic
     * @param query
     *            - a configured Query object
     * @return - a CompositeQueryConfiguration
     */
    public static CompositeQueryConfiguration create(CompositeQueryLogic compositeQueryLogic, Query query) {
        CompositeQueryConfiguration config = create(compositeQueryLogic);
        config.setQuery(query);
        return config;
    }
    
    public Query getQuery() {
        return query;
    }
    
    public void setQuery(Query query) {
        this.query = query;
    }
    
    public boolean isAllMustInitialize() {
        return allMustInitialize;
    }
    
    public void setAllMustInitialize(boolean allMustInitialize) {
        this.allMustInitialize = allMustInitialize;
    }
    
    public boolean isSequentialExecution() {
        return sequentialExecution;
    }
    
    public void setSequentialExecution(boolean sequentialExecution) {
        this.sequentialExecution = sequentialExecution;
    }
}
