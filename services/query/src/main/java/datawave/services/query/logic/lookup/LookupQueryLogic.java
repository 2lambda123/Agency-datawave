package datawave.services.query.logic.lookup;

import datawave.audit.SelectorExtractor;
import datawave.services.common.connection.AccumuloConnectionFactory;
import datawave.services.query.configuration.GenericQueryConfiguration;
import datawave.services.query.logic.BaseQueryLogic;
import datawave.services.query.logic.QueryLogicTransformer;
import datawave.webservice.common.audit.Auditor;
import datawave.webservice.query.Query;
import datawave.webservice.query.exception.QueryException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.commons.collections4.iterators.TransformIterator;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.util.MultiValueMap;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

public abstract class LookupQueryLogic<T> extends BaseQueryLogic<T> {
    public static final String LOOKUP_KEY_VALUE_DELIMITER = ":";
    
    // The underlying query logic to use for the lookup
    private final BaseQueryLogic<T> delegateQueryLogic;
    
    public LookupQueryLogic(BaseQueryLogic<T> delegateQueryLogic) {
        this.delegateQueryLogic = delegateQueryLogic;
    }
    
    @SuppressWarnings("unchecked")
    public LookupQueryLogic(LookupQueryLogic<T> other) throws CloneNotSupportedException {
        this((BaseQueryLogic<T>) other.delegateQueryLogic.clone());
    }
    
    public abstract String createQueryFromLookupTerms(MultiValueMap<String,String> lookupTerms);
    
    public abstract boolean isEventLookupRequired(MultiValueMap<String,String> lookupTerms);
    
    public abstract Set<String> getContentLookupTerms(MultiValueMap<String,String> lookupTerms);
    
    @Override
    public GenericQueryConfiguration initialize(Connector connection, Query settings, Set<Authorizations> runtimeQueryAuthorizations) throws Exception {
        return delegateQueryLogic.initialize(connection, settings, runtimeQueryAuthorizations);
    }
    
    @Override
    public void setupQuery(GenericQueryConfiguration configuration) throws Exception {
        delegateQueryLogic.setupQuery(configuration);
    }
    
    @Override
    public GenericQueryConfiguration getConfig() {
        if (delegateQueryLogic != null) {
            return delegateQueryLogic.getConfig();
        } else {
            return super.getConfig();
        }
    }
    
    @Override
    public String getPlan(Connector connection, Query settings, Set<Authorizations> runtimeQueryAuthorizations, boolean expandFields, boolean expandValues)
                    throws Exception {
        return delegateQueryLogic.getPlan(connection, settings, runtimeQueryAuthorizations, expandFields, expandValues);
    }
    
    @Override
    public Set<String> getRequiredRoles() {
        return delegateQueryLogic.getRequiredRoles();
    }
    
    @Override
    public void setRequiredRoles(Set<String> requiredRoles) {
        delegateQueryLogic.setRequiredRoles(requiredRoles);
    }
    
    @Override
    public String getTableName() {
        return delegateQueryLogic.getTableName();
    }
    
    @Override
    public long getMaxResults() {
        return delegateQueryLogic.getMaxResults();
    }
    
    @Override
    public int getMaxConcurrentTasks() {
        return delegateQueryLogic.getMaxConcurrentTasks();
    }
    
    @Override
    @Deprecated
    public long getMaxRowsToScan() {
        return delegateQueryLogic.getMaxRowsToScan();
    }
    
    @Override
    public long getMaxWork() {
        return delegateQueryLogic.getMaxWork();
    }
    
    @Override
    public void setTableName(String tableName) {
        delegateQueryLogic.setTableName(tableName);
    }
    
    @Override
    public void setMaxResults(long maxResults) {
        delegateQueryLogic.setMaxResults(maxResults);
    }
    
    @Override
    public void setMaxConcurrentTasks(int maxConcurrentTasks) {
        delegateQueryLogic.setMaxConcurrentTasks(maxConcurrentTasks);
    }
    
    @Override
    @Deprecated
    public void setMaxRowsToScan(long maxRowsToScan) {
        delegateQueryLogic.setMaxRowsToScan(maxRowsToScan);
    }
    
    @Override
    public void setMaxWork(long maxWork) {
        delegateQueryLogic.setMaxWork(maxWork);
    }
    
    @Override
    public int getMaxPageSize() {
        return delegateQueryLogic.getMaxPageSize();
    }
    
    @Override
    public void setMaxPageSize(int maxPageSize) {
        delegateQueryLogic.setMaxPageSize(maxPageSize);
    }
    
    @Override
    public long getPageByteTrigger() {
        return delegateQueryLogic.getPageByteTrigger();
    }
    
    @Override
    public void setPageByteTrigger(long pageByteTrigger) {
        delegateQueryLogic.setPageByteTrigger(pageByteTrigger);
    }
    
    @Override
    public int getBaseIteratorPriority() {
        return delegateQueryLogic.getBaseIteratorPriority();
    }
    
    @Override
    public void setBaseIteratorPriority(int baseIteratorPriority) {
        delegateQueryLogic.setBaseIteratorPriority(baseIteratorPriority);
    }
    
    @Override
    public Iterator<T> iterator() {
        return delegateQueryLogic.iterator();
    }
    
    @Override
    public TransformIterator getTransformIterator(Query settings) {
        return delegateQueryLogic.getTransformIterator(settings);
    }
    
    @Override
    public boolean getBypassAccumulo() {
        return delegateQueryLogic.getBypassAccumulo();
    }
    
    @Override
    public void setBypassAccumulo(boolean bypassAccumulo) {
        delegateQueryLogic.setBypassAccumulo(bypassAccumulo);
    }
    
    @Override
    public void close() {
        delegateQueryLogic.close();
    }
    
    @Override
    public Auditor.AuditType getAuditType(Query query) {
        return delegateQueryLogic.getAuditType(query);
    }
    
    @Override
    public Auditor.AuditType getAuditType() {
        return delegateQueryLogic.getAuditType();
    }
    
    @Override
    @Required
    public void setAuditType(Auditor.AuditType auditType) {
        delegateQueryLogic.setAuditType(auditType);
    }
    
    @Override
    public boolean getCollectQueryMetrics() {
        return delegateQueryLogic.getCollectQueryMetrics();
    }
    
    @Override
    public void setCollectQueryMetrics(boolean collectQueryMetrics) {
        delegateQueryLogic.setCollectQueryMetrics(collectQueryMetrics);
    }
    
    @Override
    public String getConnPoolName() {
        return delegateQueryLogic.getConnPoolName();
    }
    
    @Override
    public void setConnPoolName(String connPoolName) {
        delegateQueryLogic.setConnPoolName(connPoolName);
    }
    
    @Override
    public boolean canRunQuery(Collection<String> userRoles) {
        return delegateQueryLogic.canRunQuery(userRoles);
    }
    
    @Override
    public List<String> getSelectors(Query settings) throws IllegalArgumentException {
        return delegateQueryLogic.getSelectors(settings);
    }
    
    @Override
    public void setSelectorExtractor(SelectorExtractor selectorExtractor) {
        delegateQueryLogic.setSelectorExtractor(selectorExtractor);
    }
    
    @Override
    public SelectorExtractor getSelectorExtractor() {
        return delegateQueryLogic.getSelectorExtractor();
    }
    
    @Override
    public Set<String> getAuthorizedDNs() {
        return delegateQueryLogic.getAuthorizedDNs();
    }
    
    @Override
    public void setAuthorizedDNs(Set<String> authorizedDNs) {
        delegateQueryLogic.setAuthorizedDNs(authorizedDNs);
    }
    
    @Override
    public void setDnResultLimits(Map<String,Long> dnResultLimits) {
        delegateQueryLogic.setDnResultLimits(dnResultLimits);
    }
    
    @Override
    public Map<String,Long> getDnResultLimits() {
        return delegateQueryLogic.getDnResultLimits();
    }
    
    @Override
    public AccumuloConnectionFactory.Priority getConnectionPriority() {
        return delegateQueryLogic.getConnectionPriority();
    }
    
    @Override
    public QueryLogicTransformer getTransformer(Query settings) {
        return delegateQueryLogic.getTransformer(settings);
    }
    
    @Override
    public String getResponseClass(Query query) throws QueryException {
        return delegateQueryLogic.getResponseClass(query);
    }
    
    @Override
    public Set<String> getOptionalQueryParameters() {
        return delegateQueryLogic.getOptionalQueryParameters();
    }
    
    @Override
    public Set<String> getRequiredQueryParameters() {
        return delegateQueryLogic.getRequiredQueryParameters();
    }
    
    @Override
    public Set<String> getExampleQueries() {
        return delegateQueryLogic.getExampleQueries();
    }
    
    @Override
    public boolean containsDNWithAccess(Collection<String> dns) {
        return delegateQueryLogic.containsDNWithAccess(dns);
    }
    
    @Override
    public long getResultLimit(Collection<String> dns) {
        return delegateQueryLogic.getResultLimit(dns);
    }
    
    @Override
    public void forEach(Consumer<? super T> action) {
        delegateQueryLogic.forEach(action);
    }
    
    @Override
    public Spliterator<T> spliterator() {
        return delegateQueryLogic.spliterator();
    }
    
    @Override
    public String getLogicName() {
        return delegateQueryLogic.getLogicName();
    }
    
    @Override
    public void setLogicName(String logicName) {
        delegateQueryLogic.setLogicName(logicName);
    }
    
    @Override
    public void setLogicDescription(String logicDescription) {
        delegateQueryLogic.setLogicDescription(logicDescription);
    }
    
    @Override
    public String getLogicDescription() {
        return delegateQueryLogic.getLogicDescription();
    }
    
    public BaseQueryLogic<T> getDelegateQueryLogic() {
        return delegateQueryLogic;
    }
}