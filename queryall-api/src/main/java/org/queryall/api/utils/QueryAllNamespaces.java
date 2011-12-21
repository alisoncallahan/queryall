/**
 * 
 */
package org.queryall.api.utils;

/**
 * Generates the namespace ontology URIs based on calls to PropertyUtils
 * 
 * @author Peter Ansell p_ansell@yahoo.com
 */
public enum QueryAllNamespaces
{
    NAMESPACEENTRY("queryall.namespace.NamespaceEntries", "ns", "Namespace Entries"),
    
    PROFILE("queryall.namespace.Profiles", "profile", "Profiles"),
    
    PROJECT("queryall.namespace.Projects", "project", "Projects"),
    
    PROVENANCE("queryall.namespace.ProvenanceRecords", "provenance", "Provenance Records"),
    
    PROVIDER("queryall.namespace.Providers", "provider", "Providers"),
    
    QUERY("queryall.namespace.QueryTypes", "query", "Query Types"),
    
    QUERYBUNDLE("queryall.namespace.QueryBundles", "querybundle", "Query Bundles"),
    
    RDFRULE("queryall.namespace.NormalisationRules", "rdfrule", "RDF Normalisation Rules"),
    
    RULETEST("queryall.namespace.RuleTests", "ruletest", "Rule Tests"),
    
    STATISTICS("queryall.namespace.Statistics", "statistics", "Statistics"),
    
    TEMPLATES("queryall.namespace.Templates", "template", "Templates"),
    
    WEBAPPCONFIG("queryall.namespace.WebApplicationConfiguration", "webapp_configuration",
            "Web Application Configurations");
    
    /**
     * Returns the prefix that will be used to create base URIs for each namespace.
     * 
     * If the queryall.ontologyPrefix property is not set in the System Properties or in the
     * queryall.properties file, then it defaults to http://purl.org/queryall/
     * 
     * @return The prefix used to create base URIs for each namespace.
     */
    public static String getPrefix()
    {
        return QueryAllNamespaces.prefix;
    }
    
    /**
     * Returns the suffix that will be used to create base URIs for each namespace.
     * 
     * If the queryall.ontologySuffix property is not set in the System Properties or in the
     * queryall.properties file, then it defaults to : (ie., the colon character)
     * 
     * @return The suffix used to create base URIs for each namespace.
     */
    public static String getSuffix()
    {
        return QueryAllNamespaces.suffix;
    }
    
    /**
     * 
     * @param nextUri
     *            The URI that is going to be used to identify the relevant namespace
     * @return An instance of QueryAllNamespace where nextURI.startsWith(namespace.getBaseURI()) or
     *         null if there are no matches.
     * @throws NullPointerException
     *             if nextUri is null
     */
    public static QueryAllNamespaces matchBaseUri(final String nextUri)
    {
        if(nextUri == null)
        {
            throw new NullPointerException("Cannot match a null URI");
        }
        
        for(final QueryAllNamespaces nextNamespace : QueryAllNamespaces.values())
        {
            if(nextUri.startsWith(nextNamespace.getBaseURI()))
            {
                return nextNamespace;
            }
        }
        
        return null;
    }
    
    private String defaultValue;
    private String description;
    private String namespace;
    private static String prefix = PropertyUtils.getSystemOrPropertyString("queryall.ontologyPrefix",
            "http://purl.org/queryall/");
    private static String suffix = PropertyUtils.getSystemOrPropertyString("queryall.ontologySuffix", ":");
    
    private String baseUri;
    
    QueryAllNamespaces(final String nextKey, final String defaultValue, final String nextDescription)
    {
        this.defaultValue = defaultValue;
        this.namespace = PropertyUtils.getSystemOrPropertyString(nextKey, defaultValue);
        this.description = nextDescription;
    }
    
    /**
     * 
     * @return The base URI for all properties and classes defined by the QueryAll API in this
     *         namespace.
     */
    public String getBaseURI()
    {
        if(this.baseUri == null)
        {
            this.baseUri = QueryAllNamespaces.prefix + this.namespace + QueryAllNamespaces.suffix;
        }
        
        return this.baseUri;
    }
    
    /**
     * 
     * @return The default namespace identifier, which may be different to that returned by
     *         getNamespace() if it is overriden in a system property or a properties file.
     */
    public String getDefaultValue()
    {
        return this.defaultValue;
    }
    
    /**
     * 
     * @return The human readable description for the namespace.
     */
    public String getDescription()
    {
        return this.description;
    }
    
    /**
     * 
     * @return The namespace that, when prefixed with the property "queryall.ontologyPrefix" and
     *         suffixed with the property "queryall.ontologySuffix", makes up the base URI for this
     *         namespace.
     */
    public String getNamespace()
    {
        return this.namespace;
    }
}
