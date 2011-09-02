package org.queryall.impl.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.queryall.api.provider.HttpProviderSchema;
import org.queryall.api.provider.HttpSparqlProvider;
import org.queryall.api.provider.ProviderSchema;
import org.queryall.api.provider.SparqlProviderSchema;

public class HttpSparqlProviderImpl extends HttpProviderImpl implements HttpSparqlProvider
{
    
    public static List<URI> httpAndSparqlTypes()
    {
        final List<URI> results = new ArrayList<URI>(3);
        
        results.add(ProviderSchema.getProviderTypeUri());
        results.add(HttpProviderSchema.getProviderHttpTypeUri());
        results.add(SparqlProviderSchema.getProviderSparqlTypeUri());
        
        return results;
    }
    
    public HttpSparqlProviderImpl()
    {
        super();
    }
    
    public HttpSparqlProviderImpl(final Collection<Statement> inputStatements, final URI keyToUse,
            final int modelVersion) throws OpenRDFException
    {
        super(inputStatements, keyToUse, modelVersion);
    }
    
}
