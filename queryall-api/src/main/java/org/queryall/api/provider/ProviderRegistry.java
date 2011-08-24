/**
 * 
 */
package org.queryall.api.provider;

import org.queryall.api.services.AbstractServiceLoader;

/**
 * Dynamically loads and keeps a track of the different Provider's that are available.
 * 
 * Uses ProviderEnum objects as keys, as defined in ProviderFactory
 * 
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class ProviderRegistry extends AbstractServiceLoader<ProviderEnum, ProviderFactory>
{
    private static ProviderRegistry defaultRegistry;
    
    // RDFParserRegistry.getInstance();
    //
    public static synchronized ProviderRegistry getInstance()
    {
        if(ProviderRegistry.defaultRegistry == null)
        {
            ProviderRegistry.defaultRegistry = new ProviderRegistry();
        }
        
        return ProviderRegistry.defaultRegistry;
        
    }
    
    public ProviderRegistry()
    {
        super(ProviderFactory.class);
    }
    
    @Override
    protected ProviderEnum getKey(final ProviderFactory factory)
    {
        return factory.getEnum();
    }
    
}