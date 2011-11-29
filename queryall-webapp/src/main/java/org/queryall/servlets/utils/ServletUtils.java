/**
 * 
 */
package org.queryall.servlets.utils;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.app.VelocityEngine;
import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;
import org.queryall.api.base.QueryAllConfiguration;
import org.queryall.api.namespace.NamespaceEntry;
import org.queryall.api.profile.Profile;
import org.queryall.api.querytype.OutputQueryType;
import org.queryall.api.querytype.QueryType;
import org.queryall.api.rdfrule.NormalisationRuleSchema;
import org.queryall.api.utils.Constants;
import org.queryall.api.utils.SortOrder;
import org.queryall.blacklist.BlacklistController;
import org.queryall.exception.QueryAllException;
import org.queryall.exception.UnnormalisableRuleException;
import org.queryall.query.QueryBundle;
import org.queryall.query.QueryCreator;
import org.queryall.query.QueryDebug;
import org.queryall.query.RdfFetchController;
import org.queryall.query.RdfFetcherQueryRunnable;
import org.queryall.query.Settings;
import org.queryall.servlets.GeneralServlet;
import org.queryall.servlets.html.HtmlPageRenderer;
import org.queryall.servlets.queryparsers.DefaultQueryOptions;
import org.queryall.utils.RdfUtils;
import org.queryall.utils.RuleUtils;
import org.queryall.utils.StringUtils;

/**
 * @author Peter Ansell p_ansell@yahoo.com
 * 
 */
public class ServletUtils
{
    
    /**
     * @param response
     * @param localSettings
     * @param requestQueryOptions
     * @param contextPath
     * @param requestedContentType
     * @return
     */
    public static boolean checkExplicitRedirect(final HttpServletResponse response,
            final QueryAllConfiguration localSettings, final DefaultQueryOptions requestQueryOptions,
            final String contextPath, final String requestedContentType)
    {
        if(!requestQueryOptions.containsExplicitFormat())
        {
            if(localSettings.getBooleanProperty("alwaysRedirectToExplicitFormatUrl", false))
            {
                final int redirectCode = localSettings.getIntProperty("redirectToExplicitFormatHttpCode", 303);
                
                final StringBuilder redirectString = new StringBuilder();
                final boolean ignoreContextPath = false;
                
                ServletUtils.getRedirectString(redirectString, localSettings, requestQueryOptions,
                        requestedContentType, ignoreContextPath, contextPath);
                
                if(GeneralServlet._INFO)
                {
                    GeneralServlet.log.info("Sending redirect using redirectCode=" + redirectCode
                            + " to redirectString=" + redirectString.toString());
                }
                if(GeneralServlet._DEBUG)
                {
                    GeneralServlet.log.debug("contextPath=" + contextPath);
                }
                response.setStatus(redirectCode);
                // Cannot use response.sendRedirect as it will change the status to 302, which may
                // not be desired
                response.setHeader("Location", redirectString.toString());
                return true;
            }
        }
        return false;
    }
    
    /**
     * Encapsulates the call to the pool normalisation method
     * 
     * @param localSettings
     * @param includedProfiles
     * @param fetchController
     * @param myRepository
     *            The repository containing the unnormalised statements
     * @return The repository containing the normalised statements
     * @throws QueryAllException
     */
    public static Repository doPoolNormalisation(final QueryAllConfiguration localSettings,
            final List<Profile> includedProfiles, final RdfFetchController fetchController,
            final Repository myRepository) throws QueryAllException
    {
        try
        {
            return (Repository)RuleUtils.normaliseByStage(
                    NormalisationRuleSchema.getRdfruleStageAfterResultsToPool(),
                    myRepository,
                    RuleUtils.getSortedRulesForProviders(fetchController.getAllUsedProviders(),
                            localSettings.getAllNormalisationRules(), SortOrder.HIGHEST_ORDER_FIRST), includedProfiles,
                    localSettings.getBooleanProperty("recogniseImplicitRdfRuleInclusions", true),
                    localSettings.getBooleanProperty("includeNonProfileMatchedRdfRules", true));
        }
        catch(final UnnormalisableRuleException e)
        {
            GeneralServlet.log.error("Found unnormalisable rule exception while normalising the pool", e);
            throw new QueryAllException("Found unnormalisable rule exception while normalising the pool", e);
        }
        catch(final QueryAllException e)
        {
            GeneralServlet.log.error("Found queryall checked exception while normalising the pool", e);
            throw e;
        }
    }
    
    /**
     * @param localSettings
     * @param localBlacklistController
     * @param queryString
     * @param requesterIpAddress
     * @param multiProviderQueryBundles
     * @param nextTotalTime
     */
    public static void doQueryDebug(final QueryAllConfiguration localSettings,
            final BlacklistController localBlacklistController, final String queryString,
            final String requesterIpAddress, final Collection<QueryBundle> multiProviderQueryBundles,
            final long nextTotalTime)
    {
        QueryDebug nextQueryDebug;
        nextQueryDebug = new QueryDebug();
        nextQueryDebug.setClientIPAddress(requesterIpAddress);
        
        nextQueryDebug.setTotalTimeMilliseconds(nextTotalTime);
        nextQueryDebug.setQueryString(queryString);
        
        final Collection<URI> queryTitles = new HashSet<URI>();
        
        for(final QueryBundle nextInitialQueryBundle : multiProviderQueryBundles)
        {
            queryTitles.add(nextInitialQueryBundle.getQueryType().getKey());
        }
        
        nextQueryDebug.setMatchingQueryTitles(queryTitles);
        
        localBlacklistController.accumulateQueryDebug(nextQueryDebug, localSettings,
                localSettings.getLongProperty("blacklistResetPeriodMilliseconds", 120000L),
                localSettings.getBooleanProperty("blacklistResetClientBlacklistWithEndpoints", true),
                localSettings.getBooleanProperty("automaticallyBlacklistClients", false),
                localSettings.getIntProperty("blacklistMinimumQueriesBeforeBlacklistRules", 200),
                localSettings.getIntProperty("blacklistClientMaxQueriesPerPeriod", 400));
    }
    
    /**
     * @param localSettings
     * @param queryString
     * @param requestedContentType
     * @param includedProfiles
     * @param fetchController
     * @param multiProviderQueryBundles
     * @param debugStrings
     * @param myRepository
     * @param myRepositoryConnection
     * @throws InterruptedException
     * @throws IOException
     * @throws RepositoryException
     * @throws OpenRDFException
     * @throws QueryAllException
     * @throws UnnormalisableRuleException
     */
    public static void doQueryNotPretend(final QueryAllConfiguration localSettings, final String queryString,
            final String requestedContentType, final List<Profile> includedProfiles,
            final RdfFetchController fetchController, final Collection<QueryBundle> multiProviderQueryBundles,
            final Collection<String> debugStrings, final Repository myRepository) throws InterruptedException,
        IOException, RepositoryException, OpenRDFException, UnnormalisableRuleException, QueryAllException
    {
        
        RepositoryConnection myRepositoryConnection = null;
        
        try
        {
            myRepositoryConnection = myRepository.getConnection();
            
            // Attempt to fetch information as needed
            fetchController.fetchRdfForQueries();
            
            if(GeneralServlet._INFO)
            {
                if(requestedContentType.equals(Constants.APPLICATION_RDF_XML)
                        || requestedContentType.equals(Constants.TEXT_HTML))
                {
                    debugStrings.add("<!-- result units=" + fetchController.getResults().size() + " -->\n");
                }
                else if(requestedContentType.equals(Constants.TEXT_RDF_N3))
                {
                    debugStrings.add("# result units=" + fetchController.getResults().size() + " \n");
                }
            }
            
            for(final RdfFetcherQueryRunnable nextResult : fetchController.getResults())
            {
                if(GeneralServlet._INFO)
                {
                    if(requestedContentType.equals(Constants.APPLICATION_RDF_XML)
                            || requestedContentType.equals(Constants.TEXT_HTML))
                    {
                        debugStrings.add("<!-- "
                                + StringUtils.xmlEncodeString(nextResult.getResultDebugString()).replace("--", "- -")
                                + "-->");
                    }
                    else if(requestedContentType.equals(Constants.TEXT_RDF_N3))
                    {
                        debugStrings.add("# " + nextResult.getResultDebugString().replace("\n", "").replace("\r", "")
                                + ")");
                    }
                }
                
                if(GeneralServlet._TRACE)
                {
                    GeneralServlet.log.trace("GeneralServlet: normalised result string : "
                            + nextResult.getNormalisedResult());
                }
                
                Repository tempRepository = new SailRepository(new MemoryStore());
                tempRepository.initialize();
                
                RdfUtils.insertResultIntoRepository(nextResult, tempRepository, localSettings);
                
                tempRepository =
                        (Repository)RuleUtils.normaliseByStage(NormalisationRuleSchema
                                .getRdfruleStageAfterResultsImport(), tempRepository, RuleUtils.getSortedRulesByUris(
                                localSettings.getAllNormalisationRules(), nextResult.getOriginalQueryBundle()
                                        .getProvider().getNormalisationUris(), SortOrder.HIGHEST_ORDER_FIRST),
                                includedProfiles, localSettings.getBooleanProperty(
                                        "recogniseImplicitRdfRuleInclusions", true), localSettings.getBooleanProperty(
                                        "includeNonProfileMatchedRdfRules", true));
                
                if(GeneralServlet._DEBUG)
                {
                    final RepositoryConnection tempRepositoryConnection = tempRepository.getConnection();
                    
                    GeneralServlet.log.debug("GeneralServlet: getAllStatementsFromRepository(tempRepository).size()="
                            + RdfUtils.getAllStatementsFromRepository(tempRepository).size());
                    GeneralServlet.log.debug("GeneralServlet: tempRepositoryConnection.size()="
                            + tempRepositoryConnection.size());
                }
                
                RdfUtils.copyAllStatementsToRepository(myRepository, tempRepository);
            }
            
            for(final QueryBundle nextPotentialQueryBundle : multiProviderQueryBundles)
            {
                String nextStaticString = nextPotentialQueryBundle.getStaticRdfXmlString();
                
                if(GeneralServlet._TRACE)
                {
                    GeneralServlet.log
                            .trace("GeneralServlet: Adding static RDF/XML string nextPotentialQueryBundle.getQueryType().getKey()="
                                    + nextPotentialQueryBundle.getQueryType().getKey()
                                    + " nextStaticString="
                                    + nextStaticString);
                }
                
                nextStaticString =
                        "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\">"
                                + nextStaticString + "</rdf:RDF>";
                
                try
                {
                    myRepositoryConnection.add(new java.io.StringReader(nextStaticString),
                            localSettings.getDefaultHostAddress() + queryString, RDFFormat.RDFXML,
                            nextPotentialQueryBundle.getOriginalProvider().getKey());
                }
                catch(final org.openrdf.rio.RDFParseException rdfpe)
                {
                    GeneralServlet.log.error("GeneralServlet: RDFParseException: static RDF " + rdfpe.getMessage());
                    GeneralServlet.log.error("GeneralServlet: nextStaticString=" + nextStaticString);
                }
            }
        }
        finally
        {
            if(myRepositoryConnection != null)
            {
                myRepositoryConnection.close();
            }
        }
    }
    
    /**
     * @param response
     * @param localSettings
     * @param queryString
     * @param responseCode
     * @param pageOffset
     * @param requestedContentType
     * @param multiProviderQueryBundles
     * @param myRepository
     * @throws IOException
     * @throws OpenRDFException
     */
    public static void doQueryPretend(final QueryAllConfiguration localSettings, final String queryString,
            final int responseCode, final int pageOffset, final String requestedContentType,
            final Collection<QueryBundle> multiProviderQueryBundles, final Repository myRepository) throws IOException,
        OpenRDFException
    {
        for(final QueryBundle nextScheduledQueryBundle : multiProviderQueryBundles)
        {
            nextScheduledQueryBundle.toRdf(
                    myRepository,
                    StringUtils.createURI(StringUtils.percentEncode(queryString)
                            + localSettings.getSeparator()
                            + "pageoffset"
                            + pageOffset
                            + localSettings.getSeparator()
                            + StringUtils.percentEncode(nextScheduledQueryBundle.getOriginalProvider().getKey()
                                    .stringValue().toLowerCase())
                            + localSettings.getSeparator()
                            + StringUtils.percentEncode(nextScheduledQueryBundle.getQueryType().getKey().stringValue()
                                    .toLowerCase()) + localSettings.getSeparator()),
                    // + StringUtils.percentEncode(nextScheduledQueryBundle.getQueryEndpoint())),
                    Settings.CONFIG_API_VERSION);
        }
        
        if(GeneralServlet._TRACE)
        {
            GeneralServlet.log.trace("GeneralServlet: Finished with pretend query bundle rdf generation");
        }
    }
    
    /**
     * @param localSettings
     * @param realHostName
     * @param queryString
     * @param pageOffset
     * @param requestedContentType
     * @param includedProfiles
     * @param fetchController
     * @param debugStrings
     * @param myRepository
     * @throws IOException
     * @throws RepositoryException
     * @throws QueryAllException
     */
    public static void doQueryUnknown(final QueryAllConfiguration localSettings, final String realHostName,
            final Map<String, String> queryParameters, final int pageOffset, final String requestedContentType,
            final List<Profile> includedProfiles, final RdfFetchController fetchController,
            final Collection<String> debugStrings, final Repository myRepository) throws IOException,
        RepositoryException, QueryAllException
    {
        RepositoryConnection myRepositoryConnection = null;
        
        final boolean convertAlternateToPreferredPrefix =
                localSettings.getBooleanProperty("convertAlternateNamespacePrefixesToPreferred", false);
        
        try
        {
            myRepositoryConnection = myRepository.getConnection();
            
            final Collection<String> currentStaticStrings = new HashSet<String>();
            
            Collection<URI> staticQueryTypesForUnknown = new ArrayList<URI>(1);
            
            // TODO: attempt to generate a non-empty namespaceEntryMap in this case??
            if(fetchController.anyNamespaceNotRecognised())
            {
                staticQueryTypesForUnknown = localSettings.getURIProperties("unknownNamespaceStaticAdditions");
            }
            else
            {
                staticQueryTypesForUnknown = localSettings.getURIProperties("unknownQueryStaticAdditions");
            }
            
            for(final URI nextStaticQueryTypeForUnknown : staticQueryTypesForUnknown)
            {
                if(GeneralServlet._DEBUG)
                {
                    GeneralServlet.log.debug("GeneralServlet: nextStaticQueryTypeForUnknown="
                            + nextStaticQueryTypeForUnknown);
                }
                
                final QueryType nextIncludeType = localSettings.getAllQueryTypes().get(nextStaticQueryTypeForUnknown);
                
                // If we didn't understand the query
                final Map<String, Collection<NamespaceEntry>> emptyNamespaceEntryMap = Collections.emptyMap();
                
                if(nextIncludeType instanceof OutputQueryType)
                {
                    final Map<String, String> attributeList =
                            QueryCreator.getAttributeListFor(nextIncludeType, null, queryParameters,
                                    localSettings.getStringProperty("hostName", "bio2rdf.org"), realHostName,
                                    pageOffset, localSettings);
                    
                    // This is a last ditch solution to giving some meaningful feedback, as we
                    // assume that the unknown query type will handle the input, so we pass it in as
                    // both parameters
                    String nextBackupString =
                            QueryCreator.createStaticRdfXmlString(nextIncludeType, (OutputQueryType)nextIncludeType,
                                    null, attributeList, emptyNamespaceEntryMap, includedProfiles,
                                    localSettings.getBooleanProperty("recogniseImplicitRdfRuleInclusions", true),
                                    localSettings.getBooleanProperty("includeNonProfileMatchedRdfRules", true),
                                    convertAlternateToPreferredPrefix, localSettings)
                                    + "\n";
                    
                    nextBackupString =
                            "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\">"
                                    + nextBackupString + "</rdf:RDF>";
                    
                    try
                    {
                        myRepositoryConnection.add(new java.io.StringReader(nextBackupString),
                                localSettings.getDefaultHostAddress() + queryParameters.get(Constants.QUERY),
                                RDFFormat.RDFXML, nextIncludeType.getKey());
                    }
                    catch(final org.openrdf.rio.RDFParseException rdfpe)
                    {
                        GeneralServlet.log.error("GeneralServlet: RDFParseException: static RDF " + rdfpe.getMessage());
                        GeneralServlet.log.error("GeneralServlet: nextBackupString=" + nextBackupString);
                    }
                }
                else
                {
                    GeneralServlet.log
                            .warn("Attempted to include a query type that was not parsed as an output query type key="
                                    + nextIncludeType.getKey() + " types=" + nextIncludeType.getElementTypes());
                }
            }
            
            if(currentStaticStrings.size() == 0)
            {
                GeneralServlet.log.error("Could not find anything at all to match at query level queryString="
                        + queryParameters.get(Constants.QUERY));
                
                if(requestedContentType.equals("application/rdf+xml") || requestedContentType.equals("text/html"))
                {
                    debugStrings.add("<!-- Could not find anything at all to match at query level -->");
                }
                else if(requestedContentType.equals("text/rdf+n3"))
                {
                    debugStrings.add("# Could not find anything at all to match at query level");
                }
            }
            
            if(GeneralServlet._TRACE)
            {
                GeneralServlet.log.trace("GeneralServlet: ending !fetchController.queryKnown() section");
            }
        }
        finally
        {
            if(myRepositoryConnection != null)
            {
                myRepositoryConnection.close();
            }
        }
    }
    
    /**
     * @param redirectString
     *            The StringBuilder that will have the redirect String appended to it
     * @param localSettings
     *            The Settings object
     * @param requestQueryOptions
     *            The query options object
     * @param requestedContentType
     *            The requested content type
     * @param ignoreContextPath
     *            Whether we should ignore the context path or not
     * @param contextPath
     *            The context path from the request
     */
    public static void getRedirectString(final StringBuilder redirectString, final QueryAllConfiguration localSettings,
            final DefaultQueryOptions requestQueryOptions, final String requestedContentType,
            boolean ignoreContextPath, final String contextPath)
    {
        if(localSettings.getBooleanProperty("useHardcodedRequestHostname", false))
        {
            redirectString.append(localSettings.getStringProperty("hardcodedRequestHostname", ""));
        }
        
        if(localSettings.getBooleanProperty("useHardcodedRequestContext", false))
        {
            redirectString.append(localSettings.getStringProperty("hardcodedRequestContext", ""));
            ignoreContextPath = true;
        }
        
        if(!ignoreContextPath)
        {
            if(contextPath.equals(""))
            {
                redirectString.append("/");
            }
            else
            {
                redirectString.append(contextPath);
                redirectString.append("/");
            }
            
        }
        
        if(requestedContentType.equals(Constants.TEXT_HTML))
        {
            redirectString.append(localSettings.getStringProperty("htmlUrlPrefix", "page/"));
            
            if(requestQueryOptions.isQueryPlanRequest())
            {
                redirectString.append(localSettings.getStringProperty("queryplanUrlPrefix", "queryplan/"));
            }
            
            if(requestQueryOptions.containsExplicitPageOffsetValue())
            {
                redirectString.append(localSettings.getStringProperty("pageoffsetUrlOpeningPrefix", "pageoffset"));
                redirectString.append(requestQueryOptions.getPageOffset());
                redirectString.append(localSettings.getStringProperty("pageoffsetUrlClosingPrefix", "/"));
            }
            
            redirectString.append(requestQueryOptions.getParsedRequest());
            
            if(requestQueryOptions.containsExplicitPageOffsetValue())
            {
                redirectString.append(localSettings.getStringProperty("pageoffsetUrlSuffix", ""));
            }
            
            if(requestQueryOptions.isQueryPlanRequest())
            {
                redirectString.append(localSettings.getStringProperty("queryplanUrlSuffix", ""));
            }
            
            redirectString.append(localSettings.getStringProperty("htmlUrlSuffix", ""));
        }
        else if(requestedContentType.equals(Constants.TEXT_RDF_N3))
        {
            redirectString.append(localSettings.getStringProperty("n3UrlPrefix", "n3/"));
            
            if(requestQueryOptions.isQueryPlanRequest())
            {
                redirectString.append(localSettings.getStringProperty("queryplanUrlPrefix", "queryplan/"));
            }
            
            if(requestQueryOptions.containsExplicitPageOffsetValue())
            {
                redirectString.append(localSettings.getStringProperty("pageoffsetUrlOpeningPrefix", "pageoffset"));
                redirectString.append(requestQueryOptions.getPageOffset());
                redirectString.append(localSettings.getStringProperty("pageoffsetUrlClosingPrefix", "/"));
            }
            
            redirectString.append(requestQueryOptions.getParsedRequest());
            
            if(requestQueryOptions.containsExplicitPageOffsetValue())
            {
                redirectString.append(localSettings.getStringProperty("pageoffsetUrlSuffix", ""));
            }
            
            if(requestQueryOptions.isQueryPlanRequest())
            {
                redirectString.append(localSettings.getStringProperty("queryplanUrlSuffix", ""));
            }
            
            redirectString.append(localSettings.getStringProperty("n3UrlSuffix", ""));
        }
        else if(requestedContentType.equals(Constants.APPLICATION_RDF_XML))
        {
            redirectString.append(localSettings.getStringProperty("rdfXmlUrlPrefix", "rdfxml/"));
            
            if(requestQueryOptions.isQueryPlanRequest())
            {
                redirectString.append(localSettings.getStringProperty("queryplanUrlPrefix", "queryplan/"));
            }
            
            if(requestQueryOptions.containsExplicitPageOffsetValue())
            {
                redirectString.append(localSettings.getStringProperty("pageoffsetUrlOpeningPrefix", "pageoffset"));
                redirectString.append(requestQueryOptions.getPageOffset());
                redirectString.append(localSettings.getStringProperty("pageoffsetUrlClosingPrefix", "/"));
            }
            
            redirectString.append(requestQueryOptions.getParsedRequest());
            
            if(requestQueryOptions.containsExplicitPageOffsetValue())
            {
                redirectString.append(localSettings.getStringProperty("pageoffsetUrlSuffix", ""));
            }
            
            if(requestQueryOptions.isQueryPlanRequest())
            {
                redirectString.append(localSettings.getStringProperty("queryplanUrlSuffix", ""));
            }
            
            redirectString.append(localSettings.getStringProperty("rdfXmlUrlSuffix", ""));
        }
        
        else if(requestedContentType.equals(Constants.APPLICATION_JSON))
        {
            redirectString.append(localSettings.getStringProperty("jsonUrlPrefix", "json/"));
            
            if(requestQueryOptions.isQueryPlanRequest())
            {
                redirectString.append(localSettings.getStringProperty("queryplanUrlPrefix", "queryplan/"));
            }
            
            if(requestQueryOptions.containsExplicitPageOffsetValue())
            {
                redirectString.append(localSettings.getStringProperty("pageoffsetUrlOpeningPrefix", "pageoffset"));
                redirectString.append(requestQueryOptions.getPageOffset());
                redirectString.append(localSettings.getStringProperty("pageoffsetUrlClosingPrefix", "/"));
            }
            
            redirectString.append(requestQueryOptions.getParsedRequest());
            
            if(requestQueryOptions.containsExplicitPageOffsetValue())
            {
                redirectString.append(localSettings.getStringProperty("pageoffsetUrlSuffix", ""));
            }
            
            if(requestQueryOptions.isQueryPlanRequest())
            {
                redirectString.append(localSettings.getStringProperty("queryplanUrlSuffix", ""));
            }
            
            redirectString.append(localSettings.getStringProperty("jsonUrlSuffix", ""));
        }
        else
        {
            throw new IllegalArgumentException(
                    "GeneralServlet.getRedirectString: did not recognise requestedContentType=" + requestedContentType);
        }
    }
    
    /**
     * Encapsulates the basic logging details for a single request
     * 
     * @param request
     * @param requestQueryOptions
     * @param useDefaultProviders
     * @param serverName
     * @param queryString
     * @param requesterIpAddress
     * @param locale
     * @param characterEncoding
     * @param isPretendQuery
     * @param pageOffset
     * @param originalRequestedContentType
     * @param requestedContentType
     */
    public static void logRequestDetails(final HttpServletRequest request,
            final DefaultQueryOptions requestQueryOptions, final boolean useDefaultProviders, final String serverName,
            final String queryString, final String requesterIpAddress, final String locale,
            final String characterEncoding, final boolean isPretendQuery, final int pageOffset,
            final String originalRequestedContentType, final String requestedContentType)
    {
        if(GeneralServlet._INFO)
        {
            GeneralServlet.log.info("GeneralServlet: query started on " + serverName + " requesterIpAddress="
                    + requesterIpAddress + " queryString=" + queryString + " explicitPageOffset="
                    + requestQueryOptions.containsExplicitPageOffsetValue() + " pageOffset=" + pageOffset
                    + " isPretendQuery=" + isPretendQuery + " useDefaultProviders=" + useDefaultProviders);
            GeneralServlet.log.info("GeneralServlet: requestedContentType=" + requestedContentType + " acceptHeader="
                    + request.getHeader("Accept") + " userAgent=" + request.getHeader("User-Agent"));
            GeneralServlet.log.info("GeneralServlet: locale=" + locale + " characterEncoding=" + characterEncoding);
            
            if(!originalRequestedContentType.equals(requestedContentType))
            {
                GeneralServlet.log
                        .info("GeneralServlet: originalRequestedContentType was overwritten originalRequestedContentType="
                                + originalRequestedContentType + " requestedContentType=" + requestedContentType);
            }
        }
    }
    
    /**
     * @param out
     * @param request
     * @param localSettings
     * @param writerFormat
     * @param realHostName
     * @param queryString
     * @param pageOffset
     * @param requestedContentType
     * @param fetchController
     * @param debugStrings
     * @param convertedPool
     * @param contextPath
     * @throws IOException
     */
    public static void resultsToWriter(final VelocityEngine nextEngine, final Writer out,
            final QueryAllConfiguration localSettings, final RDFFormat writerFormat, final String realHostName,
            final String queryString, final int pageOffset, final String requestedContentType,
            final RdfFetchController fetchController, final Collection<String> debugStrings,
            final Repository convertedPool, final String contextPath) throws IOException
    {
        // Assume an average document may easily contain 2000 characters, to save on copies inside
        // the stringwriter
        // By default it starts with only 16 characters if we don't set a number here
        final java.io.StringWriter cleanOutput = new java.io.StringWriter(2000);
        
        // TODO: Make this process generic to allow output to arbitrary formats
        if(requestedContentType.equals(Constants.TEXT_HTML))
        {
            if(GeneralServlet._DEBUG)
            {
                GeneralServlet.log.debug("GeneralServlet: about to call html rendering method");
                GeneralServlet.log
                        .debug("GeneralServlet: fetchController.queryKnown()=" + fetchController.queryKnown());
            }
            
            try
            {
                HtmlPageRenderer.renderHtml(nextEngine, localSettings, fetchController, convertedPool, cleanOutput,
                        queryString, localSettings.getDefaultHostAddress() + queryString, realHostName, contextPath,
                        pageOffset, debugStrings);
            }
            catch(final OpenRDFException ordfe)
            {
                GeneralServlet.log.error("GeneralServlet: couldn't render HTML because of an RDF exception", ordfe);
            }
            catch(final Exception ex)
            {
                GeneralServlet.log.error("GeneralServlet: couldn't render HTML because of an unknown exception", ex);
            }
        }
        else
        {
            if(GeneralServlet._DEBUG)
            {
                GeneralServlet.log.debug("GeneralServlet: about to call rdf rendering method");
                GeneralServlet.log
                        .debug("GeneralServlet: fetchController.queryKnown()=" + fetchController.queryKnown());
            }
            
            RdfUtils.toWriter(convertedPool, cleanOutput, writerFormat);
        }
        
        if(requestedContentType.equals(Constants.APPLICATION_RDF_XML))
        {
            out.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
            
            for(final String nextDebugString : debugStrings)
            {
                out.write(nextDebugString + "\n");
            }
            final StringBuffer buffer = cleanOutput.getBuffer();
            
            // HACK to get around lack of interest in sesame for getting RDF/XML documents without
            // the XML PI
            // 38 is the length of the sesame RDF/XML PI, if it changes we will start to fail with
            // all RDF/XML results and we need to change the magic number here
            if(buffer.length() > 38)
            {
                for(int i = 38; i < cleanOutput.getBuffer().length(); i++)
                {
                    out.write(buffer.charAt(i));
                }
            }
        }
        else if(requestedContentType.equals(Constants.TEXT_RDF_N3))
        {
            for(final String nextDebugString : debugStrings)
            {
                out.write(nextDebugString + "\n");
            }
            
            final StringBuffer buffer = cleanOutput.getBuffer();
            for(int i = 0; i < cleanOutput.getBuffer().length(); i++)
            {
                out.write(buffer.charAt(i));
            }
        }
        else
        {
            final StringBuffer buffer = cleanOutput.getBuffer();
            for(int i = 0; i < cleanOutput.getBuffer().length(); i++)
            {
                out.write(buffer.charAt(i));
            }
        }
    }
    
    /**
     * Sends the basic headers for each request to the client, including the final response code and
     * the requested content type
     * 
     * @param response
     * @param responseCode
     * @param requestedContentType
     * @throws IOException
     */
    public static void sendBasicHeaders(final HttpServletResponse response, final int responseCode,
            final String requestedContentType) throws IOException
    {
        response.setContentType(requestedContentType + "; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(responseCode);
        response.setHeader("Vary", "Accept");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.flushBuffer();
    }
    
}