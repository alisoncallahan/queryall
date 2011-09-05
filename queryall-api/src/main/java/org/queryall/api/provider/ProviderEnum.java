/**
 * 
 */
package org.queryall.api.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.openrdf.model.URI;
import org.queryall.api.services.QueryAllEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider implementations register themselves with this enumeration when they are loaded.
 * 
 * NOTE: This is not an enumeration as java does not support extensible enumerations, but it should
 * act like one for all purposes other than java's underpowered switch case statement.
 * 
 */
public class ProviderEnum extends QueryAllEnum
{
    private static final Logger log = LoggerFactory.getLogger(ProviderEnum.class);
    @SuppressWarnings("unused")
    private static final boolean _TRACE = ProviderEnum.log.isTraceEnabled();
    private static final boolean _DEBUG = ProviderEnum.log.isDebugEnabled();
    @SuppressWarnings("unused")
    private static final boolean _INFO = ProviderEnum.log.isInfoEnabled();
    
    protected static final Collection<ProviderEnum> ALL_PROVIDERS = new ArrayList<ProviderEnum>(5);
    
    public static Collection<ProviderEnum> byTypeUris(final Set<URI> nextProviderUris)
    {
        if(nextProviderUris.size() == 0)
        {
            if(ProviderEnum._DEBUG)
            {
                ProviderEnum.log.debug("found an empty URI set for nextProviderUris=" + nextProviderUris);
            }
            return Collections.emptyList();
        }
        else
        {
            if(ProviderEnum._DEBUG)
            {
                ProviderEnum.log.debug("found a URI set for nextProviderUris.size()=" + nextProviderUris.size());
                ProviderEnum.log.debug("ProviderEnum.ALL_PROVIDERS.size()=" + ProviderEnum.ALL_PROVIDERS.size());
            }
        }
        
        final List<ProviderEnum> results = new ArrayList<ProviderEnum>(ProviderEnum.ALL_PROVIDERS.size());
        
        for(final ProviderEnum nextProviderEnum : ProviderEnum.ALL_PROVIDERS)
        {
            boolean matching = (nextProviderEnum.getTypeURIs().size() == nextProviderUris.size());
            
            for(final URI nextURI : nextProviderEnum.getTypeURIs())
            {
                if(!nextProviderUris.contains(nextURI))
                {
                    if(ProviderEnum._DEBUG)
                    {
                        ProviderEnum.log.debug("found an empty URI set for nextURI=" + nextURI.stringValue());
                    }
                    
                    matching = false;
                }
            }
            
            if(matching)
            {
                if(ProviderEnum._DEBUG)
                {
                    ProviderEnum.log.debug("found an matching URI set for nextProviderUris=" + nextProviderUris);
                }
                results.add(nextProviderEnum);
            }
        }
        
        if(ProviderEnum._DEBUG)
        {
            ProviderEnum.log.debug("returning results.size()=" + results.size() + " for nextProviderUris="
                    + nextProviderUris);
        }
        
        return results;
    }
    
    /**
     * Registers the specified provider.
     */
    public static void register(final ProviderEnum nextProvider)
    {
        if(ProviderEnum.valueOf(nextProvider.getName()) != null)
        {
            if(ProviderEnum._DEBUG)
            {
                ProviderEnum.log.debug("Cannot register this provider again name=" + nextProvider.getName());
            }
        }
        else
        {
            ProviderEnum.ALL_PROVIDERS.add(nextProvider);
        }
    }
    
    public static ProviderEnum register(final String name, final Set<URI> typeURIs)
    {
        final ProviderEnum newProviderEnum = new ProviderEnum(name, typeURIs);
        ProviderEnum.register(newProviderEnum);
        return newProviderEnum;
    }
    
    public static ProviderEnum valueOf(final String string)
    {
        for(final ProviderEnum nextProviderEnum : ProviderEnum.ALL_PROVIDERS)
        {
            if(nextProviderEnum.getName().equals(string))
            {
                return nextProviderEnum;
            }
        }
        
        return null;
    }
    
    /**
     * Returns all known/registered providers.
     */
    public static Collection<ProviderEnum> values()
    {
        return Collections.unmodifiableCollection(ProviderEnum.ALL_PROVIDERS);
    }
    
    /**
     * Create a new Provider enum using the given name, which must be unique
     * 
     * @param nextName
     * @param nextTypeURIs
     */
    public ProviderEnum(final String nextName, final Set<URI> nextTypeURIs)
    {
        super(nextName, nextTypeURIs);
        ProviderEnum.ALL_PROVIDERS.add(this);
    }
}
