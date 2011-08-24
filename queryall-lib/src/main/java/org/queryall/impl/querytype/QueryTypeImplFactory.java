/**
 * 
 */
package org.queryall.impl.querytype;

import org.queryall.api.querytype.QueryTypeEnum;
import org.queryall.api.querytype.QueryTypeFactory;
import org.queryall.api.querytype.QueryTypeParser;

/**
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class QueryTypeImplFactory implements QueryTypeFactory
{
    /**
     * Returns the enumeration from the enumeration that matches this factory
     */
    @Override
    public QueryTypeEnum getEnum()
    {
        return QueryTypeEnum.valueOf(QueryTypeImpl.class.getName());
    }
    
    /**
     * Returns the parser from the enumeration that matches this factory
     */
    @Override
    public QueryTypeParser getParser()
    {
        return new QueryTypeImplParser();
    }
    
}