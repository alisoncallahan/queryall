/**
 * 
 */
package org.queryall.helpers;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.openrdf.model.URI;
import org.queryall.api.Profile;
import org.queryall.api.Provider;
import org.queryall.api.QueryType;
import org.queryall.impl.QueryTypeImpl;
import org.queryall.queryutils.RdfFetchController;

/**
 * @author Peter Ansell p_ansell@yahoo.com
 */
public final class ProviderUtils
{

	/**
	 * 
	 */
	public ProviderUtils()
	{
		// TODO Auto-generated constructor stub
	}

	public static Map<URI, Provider> getProvidersForQueryType(Map<URI, Provider> allProviders, URI nextQueryType)
	{
	    final Map<URI, Provider> results = new TreeMap<URI, Provider>();
	    
	    for(final Provider nextProvider : allProviders.values())
	    {
	        if(nextProvider.containsQueryTypeUri(nextQueryType))
	        {
	            results.put(nextProvider.getKey(), nextProvider);
	        }
	    }

	    if(Settings._DEBUG)
	    {
	        Settings.log.debug("Settings.getProvidersForQueryType: Found "
	                + results.size() + " providers for customService="
	                + nextQueryType.stringValue());
	    }
	    if(Settings._TRACE)
	    {
	        for(final Provider nextResult : results.values())
	        {
	            Settings.log
	                    .trace("Settings.getProvidersForQueryType: nextResult="
	                            + nextResult.toString());
	        }
	    }
	    return results;
	}

	public static Map<URI, Provider> getProvidersForNamespaceUris(Map<URI, Provider> allProviders,
	        Collection<Collection<URI>> namespaceUris, URI namespaceMatchMethod)
	{
	    if((namespaceUris == null) || (namespaceUris.size() == 0))
	    {
	        if(Settings._DEBUG)
	        {
	            Settings.log
	                    .debug("Settings.getProvidersForNamespaceUris: namespaceUris was either null or empty");
	        }
	        return Collections.emptyMap();
	    }
	    if(Settings._TRACE)
	    {
	        Settings.log
	                .trace("Settings.getProvidersForNamespaceUris: namespaceUris="
	                        + namespaceUris);
	    }
	    final Map<URI, Provider> results = new TreeMap<URI, Provider>();
	
	    for(final Provider nextProvider : allProviders.values())
	    {
	        boolean anyFound = false;
	        boolean allFound = true;
	        if(Settings._TRACE)
	        {
	            Settings.log
	                    .trace("Settings.getProvidersForNamespaceUris: nextProvider.getKey()="
	                            + nextProvider.getKey().stringValue());
	        }
	        
	        
	        for(final Collection<URI> nextNamespaceUriList : namespaceUris)
	        {
	            if(nextNamespaceUriList == null)
	            {
	                if(Settings._DEBUG)
	                {
	                    Settings.log
	                            .debug("Settings.getProvidersForNamespaceUris: nextNamespaceUriList was null");
	                }
	                continue;
	            }
	            if(Settings._TRACE)
	            {
	                Settings.log
	                        .trace("Settings.getProvidersForNamespaceUris: nextNamespaceUriList="
	                                + nextNamespaceUriList);
	            }
	            boolean somethingFound = false;
	            for(final URI nextNamespaceUri : nextNamespaceUriList)
	            {
	                if(Settings._TRACE)
	                {
	                    Settings.log
	                            .trace("Settings.getProvidersForNamespaceUris: nextNamespaceUri="
	                                    + nextNamespaceUri);
	                }
	                if(nextProvider.containsNamespaceUri(nextNamespaceUri))
	                {
	                    somethingFound = true;
	                    break;
	                }
	            }
	            if(somethingFound)
	            {
	                anyFound = true;
	            }
	            else
	            {
	                allFound = false;
	            }
	        }
	        if(anyFound
	                && namespaceMatchMethod.equals(QueryTypeImpl.getNamespaceMatchAnyUri()))
	        {
	            results.put(nextProvider.getKey(), nextProvider);
	        }
	        else if(allFound
	                && namespaceMatchMethod.equals(QueryTypeImpl.getNamespaceMatchAllUri()))
	        {
	            results.put(nextProvider.getKey(), nextProvider);
	        }
	    }
	    return results;
	}

	public static Collection<Provider> getProvidersForQueryNonNamespaceSpecific(Map<URI, Provider> allProviders, 
	        QueryType nextQueryType, List<Profile> sortedIncludedProfiles, boolean recogniseImplicitProviderInclusions, boolean includeNonProfileMatchedProviders)
	{
	    Collection<Provider> results = new LinkedList<Provider>();
	    
	    // if we aren't specific to namespace we simply find all providers for this type of custom query
	    Map<URI, Provider> relevantProviders = getProvidersForQueryType(allProviders, nextQueryType.getKey() );
	    
	    for( Provider nextAllProvider : relevantProviders.values() )
	    {
	        if( RdfFetchController._DEBUG )
	        {
	            RdfFetchController.log.debug( "RdfFetchController.getProvidersForQueryNonNamespaceSpecific: !nextQueryType.isNamespaceSpecific nextAllProvider="+nextAllProvider.toString() );
	        }
	        
	        if( nextAllProvider.isUsedWithProfileList( sortedIncludedProfiles, recogniseImplicitProviderInclusions, includeNonProfileMatchedProviders ) )
	        {
	            if( RdfFetchController._DEBUG )
	            {
	                RdfFetchController.log.debug( "RdfFetchController.getProvidersForQueryNonNamespaceSpecific: profileList suitable for nextAllProvider.getKey()="+nextAllProvider.getKey() );
	            }
	            
	            results.add( nextAllProvider );
	        }
	    }
	    
	    return results;
	}

	public static Collection<Provider> getDefaultProviders(Map<URI, Provider> allProviders, QueryType queryType, List<Profile> profileList, boolean recogniseImplicitProviderInclusions, boolean includeNonProfileMatchedProviders)
	{
	    final Collection<Provider> results = new HashSet<Provider>();
	
	    // Return an empty collection if this query type does not include defaults
		if(queryType.getIncludeDefaults())
		{
	        for(final Provider nextProvider : allProviders.values())
	        {
	            if(nextProvider.getIsDefaultSource()
	                    && nextProvider.containsQueryTypeUri(queryType.getKey()))
	            {
	                    if( nextProvider.isUsedWithProfileList( profileList, recogniseImplicitProviderInclusions, includeNonProfileMatchedProviders ) )
	                    {
	                        if( RdfFetchController._DEBUG )
	                        {
	                            RdfFetchController.log.debug( "RdfFetchController.getProvidersForQueryNonNamespaceSpecific: profileList suitable for nextAllProvider.getKey()="+nextProvider.getKey());
	                        }
	                        
	                        results.add( nextProvider );
	                    }
	            }
	        }
		}
	    
	    return results;
	}

	public static Collection<Provider> getProvidersForQueryNamespaceSpecific(Map<URI, Provider> allProviders, List<Profile> sortedIncludedProfiles, QueryType nextQueryType, Map<String, Collection<URI>> namespacePrefixToUriMap, String queryString, boolean recogniseImplicitProviderInclusions, boolean includeNonProfileMatchedProviders)
	{
	    Collection<Provider> results = new LinkedList<Provider>();
	    
	    List<String> queryStringMatches = nextQueryType.matchesForQueryString( queryString );
	    
	    int queryStringMatchesSize = queryStringMatches.size();
	    
	    // Collection<String> nextQueryNamespacePrefixes = new HashSet<String>();
	    Collection<Collection<URI>> nextQueryNamespaceUris = new HashSet<Collection<URI>>();
	    
	    for( int nextNamespaceInputIndex : nextQueryType.getNamespaceInputIndexes() )
	    {
	        if( queryStringMatchesSize  >= nextNamespaceInputIndex && nextNamespaceInputIndex > 0 )
	        {
	            String nextTitle = queryStringMatches.get( nextNamespaceInputIndex-1 );
	            
	            Collection<URI> nextUriFromTitleNamespaceList = NamespaceUtils.getNamespaceUrisForTitle(namespacePrefixToUriMap, nextTitle );
	            
	            if( nextUriFromTitleNamespaceList != null )
	            {
	                nextQueryNamespaceUris.add( nextUriFromTitleNamespaceList );
	            }
	            else if(RdfFetchController._DEBUG)
	            {
	        		RdfFetchController.log.debug( "RdfFetchController.getProvidersForQueryNamespaceSpecific: did not find any namespace URIs for nextTitle="+nextTitle + " nextQueryType.getKey()="+nextQueryType.getKey());
	            }
	        }
	        else
	        {
	            RdfFetchController.log.error( "RdfFetchController.getProvidersForQueryNamespaceSpecific: Could not match the namespace because the input index was invalid nextNamespaceInputIndex="+nextNamespaceInputIndex+" queryStringMatches.size()="+queryStringMatches.size() );
	            
	            throw new RuntimeException( "Could not match the namespace because the input index was invalid nextNamespaceInputIndex="+nextNamespaceInputIndex+" queryStringMatches.size()="+queryStringMatches.size() );
	        }
	    }
	    
	    if( RdfFetchController._DEBUG )
	    {
	        // log.debug( "RdfFetchController.getProvidersForQueryNamespaceSpecific: nextQueryNamespacePrefixes="+nextQueryNamespacePrefixes );
	        RdfFetchController.log.debug( "RdfFetchController.getProvidersForQueryNamespaceSpecific: nextQueryNamespaceUris="+nextQueryNamespaceUris );
	    }
	    
	    if( nextQueryType.handlesNamespaceUris( nextQueryNamespaceUris ) )
	    {
	        if( RdfFetchController._DEBUG )
	        {
	            RdfFetchController.log.debug( "RdfFetchController.getProvidersForQueryNamespaceSpecific: confirmed to handle namespaces nextQueryType.getKey()="+nextQueryType.getKey() +" nextQueryNamespaceUris="+nextQueryNamespaceUris );
	        }
	        
	        Map<URI, Provider> namespaceSpecificProviders = Settings.getProvidersForQueryTypeForNamespaceUris(allProviders, nextQueryType.getKey(), nextQueryNamespaceUris, nextQueryType.getNamespaceMatchMethod() );
	        
	        for( Provider nextNamespaceSpecificProvider : namespaceSpecificProviders.values() )
	        {
	            if( RdfFetchController._TRACE )
	            {
	                RdfFetchController.log.trace( "RdfFetchController.getProvidersForQueryNamespaceSpecific: nextQueryType.isNamespaceSpecific nextNamespaceSpecificProvider="+nextNamespaceSpecificProvider.getKey() );
	            }
	            
	            if( nextNamespaceSpecificProvider.isUsedWithProfileList( sortedIncludedProfiles, recogniseImplicitProviderInclusions, includeNonProfileMatchedProviders ) )
	            {
	                if( RdfFetchController._DEBUG )
	                {
	                    RdfFetchController.log.debug( "RdfFetchController.getProvidersForQueryNamespaceSpecific: profileList suitable for nextNamespaceSpecificProvider.getKey()="+nextNamespaceSpecificProvider.getKey()+" queryString="+queryString );
	                }
	                
	                results.add( nextNamespaceSpecificProvider );
	            }
	        }
	    }
	    
	    return results;
	}

}
