package datawave.webservice.common.result;

import datawave.services.common.cache.TableCacheDescription;
import datawave.webservice.HtmlProvider;
import datawave.webservice.result.BaseResponse;

import jakarta.xml.bind.annotation.XmlAccessOrder;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorOrder;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.LinkedList;
import java.util.List;

@XmlRootElement(name = "AccumuloTableCacheStatus")
@XmlAccessorType(XmlAccessType.NONE)
@XmlAccessorOrder(XmlAccessOrder.ALPHABETICAL)
public class AccumuloTableCacheStatus extends BaseResponse implements HtmlProvider {
    
    private static final long serialVersionUID = 1L;
    private static final String TITLE = "Accumulo Table Cache Status", EMPTY = "";
    
    @XmlElementWrapper(name = "TableCaches")
    @XmlElement(name = "TableCache")
    private List<TableCacheDescription> caches = new LinkedList<>();
    
    @Override
    public String getTitle() {
        return TITLE;
    }
    
    @Override
    public String getHeadContent() {
        return TITLE;
    }
    
    @Override
    public String getPageHeader() {
        return EMPTY;
    }
    
    @Override
    public String getMainContent() {
        StringBuilder builder = new StringBuilder();
        
        builder.append("<style>\n");
        builder.append("table td, table th {");
        builder.append("border-colapse: collapse;\n");
        builder.append("border: 1px black solid;\n");
        builder.append("empty-cells: hide;\n");
        builder.append("}\n");
        builder.append("</style>\n");
        
        builder.append("<h2>").append("Table Caches").append("</h2>");
        builder.append("<br/>");
        builder.append("<table>");
        builder.append("<tr><th>Table Name</th><th>Connection Pool</th><th>Authorizations</th><th>Reload Interval (ms)</th><th>Max Rows</th><th>Last Refresh</th><th>Refreshing Now</th></tr>");
        for (TableCacheDescription cache : caches) {
            builder.append("<tr>");
            builder.append("<td>").append(cache.getTableName()).append("</td>");
            builder.append("<td>").append(cache.getConnectionPoolName()).append("</td>");
            builder.append("<td>").append(cache.getAuthorizations()).append("</td>");
            builder.append("<td>").append(cache.getReloadInterval()).append("</td>");
            builder.append("<td>").append(cache.getMaxRows()).append("</td>");
            builder.append("<td>").append(cache.getLastRefresh()).append("</td>");
            builder.append("<td>").append(cache.getCurrentlyRefreshing()).append("</td>");
            builder.append("</tr>");
        }
        builder.append("</table>");
        return builder.toString();
    }
    
    public List<TableCacheDescription> getCaches() {
        return caches;
    }
}
