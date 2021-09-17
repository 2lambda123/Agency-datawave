package datawave.query.transformer;

import datawave.query.model.QueryModel;
import datawave.services.query.logic.QueryLogic;
import datawave.services.query.logic.QueryLogicTransformer;
import datawave.webservice.query.Query;
import datawave.webservice.query.cachedresults.CacheableQueryRow;
import datawave.webservice.query.exception.QueryException;
import org.apache.commons.collections4.Transformer;

import java.util.List;

public interface EventQueryTransformerInterface<Q> extends QueryLogicTransformer {
    
    void initialize(String tableName, Query settings);
    
    void initialize(QueryLogic<Q> logic, Query settings);
    
    Object transform(Object input);
    
    List<CacheableQueryRow> writeToCache(Object o) throws QueryException;
    
    List<Object> readFromCache(List<CacheableQueryRow> cacheableQueryRowList);
    
    Transformer getEventQueryDataDecoratorTransformer();
    
    void setEventQueryDataDecoratorTransformer(Transformer eventQueryDataDecoratorTransformer);
    
    List<String> getContentFieldNames();
    
    void setContentFieldNames(List<String> contentFieldNames);
    
    QueryModel getQm();
    
    void setQm(QueryModel qm);
    
}
