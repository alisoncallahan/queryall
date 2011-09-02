/**
 * 
 */
package org.queryall.api.querytype;

import org.queryall.api.services.AbstractServiceLoader;

/**
 * Dynamically loads and keeps a track of the different QueryType's that are available.
 * 
 * Uses QueryTypeEnum objects as keys, as defined in QueryTypeFactory
 * 
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class QueryTypeRegistry extends AbstractServiceLoader<QueryTypeEnum, QueryTypeFactory>
{
    private static QueryTypeRegistry defaultRegistry;
    
    public static synchronized QueryTypeRegistry getInstance()
    {
        if(QueryTypeRegistry.defaultRegistry == null)
        {
            QueryTypeRegistry.defaultRegistry = new QueryTypeRegistry();
        }
        
        return QueryTypeRegistry.defaultRegistry;
        
    }
    
    public QueryTypeRegistry()
    {
        super(QueryTypeFactory.class);
    }
    
    @Override
    protected QueryTypeEnum getKey(final QueryTypeFactory factory)
    {
        return factory.getEnum();
    }
    
}
