/**
 * 
 */
package org.queryall.api.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openrdf.model.URI;
import org.queryall.api.services.QueryAllEnum;

/**
 * Provider implementations register themselves with this enumeration when they are loaded.
 * 
 * NOTE: This is not an enumeration as java does not support extensible enumerations, but it should
 * act like one for all purposes other than java's underpowered switch case statement.
 * 
 */
public class ProviderEnum extends QueryAllEnum
{
    protected static final Collection<ProviderEnum> ALL_PROVIDERS = new ArrayList<ProviderEnum>(5);
    
    public static Collection<ProviderEnum> byTypeUris(final List<URI> nextProviderUris)
    {
        final List<ProviderEnum> results = new ArrayList<ProviderEnum>(ProviderEnum.ALL_PROVIDERS.size());
        
        for(final ProviderEnum nextProviderEnum : ProviderEnum.ALL_PROVIDERS)
        {
            if(nextProviderEnum.getTypeURIs().equals(nextProviderUris))
            {
                results.add(nextProviderEnum);
            }
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
            QueryAllEnum.log.error("Cannot register this provider again name=" + nextProvider.getName());
        }
        else
        {
            ProviderEnum.ALL_PROVIDERS.add(nextProvider);
        }
    }
    
    public static ProviderEnum register(final String name, final List<URI> typeURIs)
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
    public ProviderEnum(final String nextName, final List<URI> nextTypeURIs)
    {
        super(nextName, nextTypeURIs);
    }
}
