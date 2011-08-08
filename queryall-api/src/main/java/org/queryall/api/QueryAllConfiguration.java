package org.queryall.api;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

import org.openrdf.model.Statement;
import org.openrdf.model.URI;

/**
 * @author Peter Ansell p_ansell@yahoo.com
 */
public interface QueryAllConfiguration
{
    void addNamespaceEntry(NamespaceEntry nextNamespaceEntry);
    
    void addNormalisationRule(NormalisationRule nextNormalisationRule);
    
    void addProfile(Profile nextProfile);
    
    void addProvider(Provider nextProvider);
    
    void addQueryType(QueryType nextQueryType);
    
    void addRuleTest(RuleTest nextRuleTest);
    
    Map<URI, NamespaceEntry> getAllNamespaceEntries();
    
    Map<URI, NormalisationRule> getAllNormalisationRules();
    
    Map<URI, Profile> getAllProfiles();
    
    Map<URI, Provider> getAllProviders();
    
    Map<URI, QueryType> getAllQueryTypes();
    
    Map<URI, RuleTest> getAllRuleTests();
    
    boolean getBooleanProperty(String propertyKey, boolean defaultValue);
    
    String getDefaultHostAddress();
    
    float getFloatProperty(String key, float defaultValue);
    
    int getIntProperty(String key, int defaultValue);
    
    long getLongProperty(String key, long defaultValue);
    
    Map<String, Collection<URI>> getNamespacePrefixesToUris();
    
    Pattern getPlainNamespaceAndIdentifierPattern();
    
    Pattern getPlainNamespacePattern();
    
    Collection<Statement> getStatementProperties(String string);
    
    Collection<String> getStringProperties(String string);
    
    String getStringProperty(String key, String defaultValue);
    
    Pattern getTagPattern();
    
    Collection<URI> getURIProperties(String string);
    
    URI getURIProperty(String key, URI defaultValue);
    
}