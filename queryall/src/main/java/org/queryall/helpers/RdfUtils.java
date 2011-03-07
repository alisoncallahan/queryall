
package org.queryall.helpers;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Map;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.BooleanLiteralImpl;
import org.openrdf.model.impl.IntegerLiteralImpl;
import org.openrdf.model.impl.CalendarLiteralImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.Rio;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.sail.memory.model.BooleanMemLiteral;
import org.openrdf.sail.memory.model.IntegerMemLiteral;
import org.openrdf.sail.memory.model.CalendarMemLiteral;

import org.queryall.*;
import org.queryall.impl.*;
import org.queryall.queryutils.*;


/**
 * A utility class that is used by multiple different Bio2RDF classes
 * @author peter
 * @version $Id: $
 */
public class RdfUtils
{
    static final Logger log = Logger.getLogger(RdfUtils.class
            .getName());
    static final boolean _TRACE = RdfUtils.log.isTraceEnabled();
    static final boolean _DEBUG = RdfUtils.log.isDebugEnabled();
    static final boolean _INFO = RdfUtils.log.isInfoEnabled();
    
    private static Repository myRepository = null;
    static ValueFactory myValueFactory = null;
    
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final String TIME_ZOME = "UTC";
    
    static
    {
        if((RdfUtils.myRepository == null)
                || (RdfUtils.myValueFactory == null))
        {
            try
            {
                RdfUtils.myRepository = new SailRepository(new MemoryStore());
                RdfUtils.myRepository.initialize();
                
                RdfUtils.myValueFactory = RdfUtils.myRepository
                        .getValueFactory();
            }
            catch (final RepositoryException re)
            {
                RdfUtils.log
                        .fatal("Utilities: Repository failed to initialise!!!! Bad stuff may happen now!!!");
                throw new RuntimeException(re);
            }
        }
        
    }
    
    public static void copyAllStatementsToRepository(Repository destination, Repository source)
    {
        RepositoryConnection mySourceConnection = null;
        RepositoryConnection myDestinationConnection = null;
        
        try
        {
            mySourceConnection = source.getConnection();
            if(_DEBUG)
            {
                log.debug("RdfUtils.copyAllStatementsToRepository: mySourceConnection.size()="+mySourceConnection.size());
            }
            myDestinationConnection = destination.getConnection();
            myDestinationConnection.add(mySourceConnection.getStatements(null, null, null, true));
            
            myDestinationConnection.commit();
            if(_DEBUG)
            {
                log.debug("RdfUtils.copyAllStatementsToRepository: myDestinationConnection.size()="+myDestinationConnection.size());
            }
        }
        catch(Exception ex)
        {
            log.error("RdfUtils.copyAllStatementsToRepository", ex);
        }
        finally
        {
            if(mySourceConnection != null)
            {
                try
                {
                    mySourceConnection.close();
                }
                catch(Exception ex)
                {
                    log.error("mySourceConnection",ex);
                }
            }
            if(myDestinationConnection != null)
            {
                try
                {
                    myDestinationConnection.close();
                }
                catch(Exception ex)
                {
                    log.error("myDestinationConnection",ex);
                }
            }
        }

    }
    
    public static Collection<QueryType> fetchQueryTypeByKey(String hostToUse, URI nextQueryKey, int modelVersion) throws InterruptedException
    {
        QueryBundle nextQueryBundle = new QueryBundle();
        
        Provider dummyProvider = new ProviderImpl();
        
        Collection<String> endpointUrls = new HashSet<String>();
        
        // if(nextQueryKey.startsWith(Settings.getSettings().getDefaultHostAddress()))
        // {
            String namespaceAndIdentifier = nextQueryKey.stringValue().substring(Settings.getSettings().getDefaultHostAddress().length());
            
            List<String> nsAndIdList = StringUtils.getNamespaceAndIdentifier(namespaceAndIdentifier);
            
            if(nsAndIdList.size() == 2)
            {
                endpointUrls.add(hostToUse+new QueryTypeImpl().getDefaultNamespace()+Settings.getSettings().getStringPropertyFromConfig("separator")+StringUtils.percentEncode(nsAndIdList.get(1)));
                nextQueryBundle.queryEndpoint = hostToUse+new QueryTypeImpl().getDefaultNamespace()+Settings.getSettings().getStringPropertyFromConfig("separator")+StringUtils.percentEncode(nsAndIdList.get(1));
            }
        // }
        // else
        // {
            // dummyProvider.endpointUrls.add(hostToUse+RdfUtils.percentEncode(nextQueryKey.substring(nextQueryKey.indexOf(new QueryTypeImpl().getDefaultNamespace()))));		
            // nextQueryBundle.queryEndpoint = hostToUse+RdfUtils.percentEncode(nextQueryKey.substring(nextQueryKey.indexOf(new QueryTypeImpl().getDefaultNamespace())));
        // }
        
        dummyProvider.setEndpointUrls(endpointUrls);
        dummyProvider.setEndpointMethod(ProviderImpl.getProviderHttpGetUrl());
        dummyProvider.setKey(hostToUse+Settings.getSettings().getNamespaceForProvider()+Settings.getSettings().getStringPropertyFromConfig("separator")+StringUtils.percentEncode(namespaceAndIdentifier));
        dummyProvider.setIsDefaultSource(true);
        
        nextQueryBundle.setProvider(dummyProvider);
        
        QueryType dummyQuery = new QueryTypeImpl();
        
        dummyQuery.setKey(hostToUse+Settings.getSettings().getNamespaceForQueryType()+Settings.getSettings().getStringPropertyFromConfig("separator")+StringUtils.percentEncode(namespaceAndIdentifier));
        dummyQuery.setTitle("$$__queryfetch__$$");
        dummyQuery.setIncludeDefaults(true);
        
        nextQueryBundle.setQueryType(dummyQuery);
        
        Collection<QueryBundle> queryBundles = new HashSet<QueryBundle>();
        
        queryBundles.add(nextQueryBundle);
        
        return getQueryTypesForQueryBundles(queryBundles, modelVersion);
    }
    
    public static Collection<QueryType> fetchQueryTypeByKey(
        URI nextQueryKey, boolean useSparqlGraph, 
        String sparqlGraphUri, String sparqlEndpointUrl, int modelVersion)
    {
        String constructQueryString = RdfUtils.getConstructQueryForKey(
            nextQueryKey, useSparqlGraph, sparqlGraphUri);
        
        QueryBundle nextQueryBundle = new QueryBundle();
        
        Provider dummyProvider = new ProviderImpl();
        
        Collection<String> endpointUrls = new HashSet<String>();
        
        endpointUrls.add(sparqlEndpointUrl);
        
        dummyProvider.setEndpointUrls(endpointUrls);
        
        nextQueryBundle.setQueryEndpoint(sparqlEndpointUrl);
        
        dummyProvider.setEndpointMethod(ProviderImpl.getProviderHttpPostSparql());
        dummyProvider.setKey(Settings.getSettings().getDefaultHostAddress()+Settings.getSettings().getNamespaceForProvider()+Settings.getSettings().getStringPropertyFromConfig("separator")+StringUtils.percentEncode(nextQueryKey.stringValue()));
        dummyProvider.setIsDefaultSource(true);
        
        nextQueryBundle.originalProvider = dummyProvider;
        
        
        QueryType dummyQuery = new QueryTypeImpl();
        
        dummyQuery.setKey(Settings.getSettings().getDefaultHostAddress()+Settings.getSettings().getNamespaceForQueryType()+Settings.getSettings().getStringPropertyFromConfig("separator")+StringUtils.percentEncode(nextQueryKey.stringValue()));
        dummyQuery.setTitle("$$__queryfetch__$$");
        dummyQuery.setIncludeDefaults(true);
        
        nextQueryBundle.setQueryType(dummyQuery);
        
        nextQueryBundle.setQuery(constructQueryString);
        
        Collection<QueryBundle> queryBundles = new HashSet<QueryBundle>();
        
        queryBundles.add(nextQueryBundle);
        
        return getQueryTypesForQueryBundles(queryBundles, modelVersion);
    }
    
    public static String findWriterFormat(String requestedContentType, String preferredDisplayContentType, String fallback)
    {
        if(requestedContentType.equals("text/html"))
        {
            return requestedContentType;
        }
        
        // even if they request a random format, we need to make sure that Rio has a writer compatible with it, otherwise we revert to one of the defaults as a failsafe mechanism
        RDFFormat writerFormat = Rio.getWriterFormatForMIMEType(requestedContentType);
        
        if(writerFormat != null)
        {
            return requestedContentType;
        }
        else
        {
            writerFormat = Rio.getWriterFormatForMIMEType(preferredDisplayContentType);
            
            if(writerFormat != null)
            {
                return preferredDisplayContentType;
            }
            else
            {
                return fallback;
            }
        }
    }
    
    public static HttpUrlQueryRunnable generateHttpUrlSparqlDeleteThread(
        BaseQueryAllInterface rdfObject, 
        boolean useSparqlGraph, 
        String sparqlGraphUri,
        String sparqlEndpointMethod,
        String sparqlEndpointUrl,
        String acceptHeader, 
        String expectedReturnFormat) 
    throws OpenRDFException
    {
        String sparqlInsertQuery = getSparulQueryForObject(rdfObject, false, true, useSparqlGraph, sparqlGraphUri);
        
        return generateHttpUrlSparqlThread(
         sparqlInsertQuery,
         sparqlEndpointMethod,
         sparqlEndpointUrl,
         acceptHeader, 
         expectedReturnFormat);
    }
    
    public static HttpUrlQueryRunnable generateHttpUrlSparqlInsertThread(
        BaseQueryAllInterface rdfObject, 
        boolean isDelete,
        boolean useSparqlGraph, 
        String sparqlGraphUri,
        String sparqlEndpointMethod,
        String sparqlEndpointUrl,
        String acceptHeader, 
        String expectedReturnFormat) 
    throws OpenRDFException
    {
        String sparqlInsertQuery = getSparulQueryForObject(rdfObject, true, isDelete, useSparqlGraph, sparqlGraphUri);
        
        return generateHttpUrlSparqlThread(
         sparqlInsertQuery,
         sparqlEndpointMethod,
         sparqlEndpointUrl,
         acceptHeader, 
         expectedReturnFormat);
    }
    
    public static HttpUrlQueryRunnable generateHttpUrlSparqlThread(
        String sparqlQuery,
        String sparqlEndpointMethod,
        String sparqlEndpointUrl,
        String acceptHeader, 
        String expectedReturnFormat) 
    {
        return new HttpUrlQueryRunnable(
                sparqlEndpointMethod,
                sparqlEndpointUrl, sparqlQuery,
                acceptHeader, expectedReturnFormat);
    }
    
    /**
     * @param nextRepository
     * @return
     * @throws OpenRDFException
     */
    public static List<Statement> getAllStatementsFromRepository(
            Repository nextRepository) throws OpenRDFException
    {
        final List<Statement> results = new ArrayList<Statement>();
        
        final RepositoryConnection con = nextRepository.getConnection();
        
        try
        {
            final String queryString = "CONSTRUCT { ?subject ?predicate ?object . } WHERE { ?subject ?predicate ?object . } ORDER BY ?subject ?predicate ?object";
            final GraphQuery tupleQuery = con.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
            final GraphQueryResult queryResult = tupleQuery.evaluate();
            
            try
            {
                while(queryResult.hasNext())
                {
                    final Statement nextStatement = queryResult.next();
                    
                    if(RdfUtils._DEBUG)
                    {
                        RdfUtils.log
                                .debug("RdfUtils.getAllStatementsFromRepository: found statement: nextStatement="
                                        + nextStatement);
                    }
                    
                    results.add(nextStatement);
                }
            }
            catch (final OpenRDFException ordfe)
            {
                RdfUtils.log
                        .error("RdfUtils.getAllStatementsFromRepository: inner caught exception "
                                + ordfe);
                
                throw ordfe;
            }
            finally
            {
                queryResult.close();
            }
        }
        catch (final OpenRDFException ordfe)
        {
            RdfUtils.log
                    .error("RdfUtils.getAllStatementsFromRepository: outer caught exception "
                            + ordfe);
            
            throw ordfe;
        }
        finally
        {
            con.close();
        }
        
        return results;
    }
    
    /**
     * @param nextValue
     * @return
     */
    public static boolean getBooleanFromValue(Value nextValue)
    {
        boolean result;
        
        try
        {
            result = ((BooleanLiteralImpl) nextValue).booleanValue();
        }
        catch (final ClassCastException cce)
        {
            try
            {
                result = ((BooleanMemLiteral) nextValue).booleanValue();
            }
            catch (final ClassCastException cce2)
            {
                // HACK for a Virtuoso bug where booleans are transformed into 0 and 1 and typed as integers instead of booleans
                if(nextValue instanceof org.openrdf.sail.memory.model.IntegerMemLiteral)
                {
                    int tempValue = ((IntegerMemLiteral) nextValue).intValue();
                    if(tempValue == 0)
                    {
                        return false;
                    }
                    
                    if(tempValue == 1)
                    {
                        return true;
                    }
                }

                
                if(RdfUtils._DEBUG)
                {
                    RdfUtils.log
                            .debug("RdfUtils.getBooleanFromValue: nextValue was not a typed boolean literal. Trying to parse it as a string... type="
                                    + nextValue.getClass().getName());
                }
                
                result = Boolean.parseBoolean(nextValue.toString());
            }
        }
        
        return result;
    }
    
    public static String getConstructQueryByType(BaseQueryAllInterface nextObject, int offset, int limit, boolean useSparqlGraph, String sparqlGraphUri)
    {
        return getConstructQueryByType(nextObject.getElementType(), offset, limit, useSparqlGraph, sparqlGraphUri);
    }
    
    
    public static String getConstructQueryByType(String nextType, int offset, int limit, boolean useSparqlGraph, String sparqlGraphUri)
    {
        StringBuilder result = new StringBuilder();
        
        result.append("CONSTRUCT { ?s a <"+nextType+"> . ");
        
        int counter = 0;
        
        for(String nextTitleUri : Settings.getSettings().getStringCollectionPropertiesFromConfig("titleProperties"))
        {
            result.append(" ?s <"+nextTitleUri+"> ?o"+counter+" . ");
            
            counter++;
        }
        
        result.append(" } WHERE { ");
        
        if(useSparqlGraph)
        {
            result.append(" GRAPH <" + sparqlGraphUri + "> { ");
        }
        
        result.append(" ?s a <"+nextType+"> . ");
        
        counter = 0;
        
        for(String nextTitleUri : Settings.getSettings().getStringCollectionPropertiesFromConfig("titleProperties"))
        {
            result.append("OPTIONAL{ ?s <"+nextTitleUri+"> ?o"+counter+" . }");
            
            counter++;
        }
        
        if(useSparqlGraph)
        {
            result.append(" } ");
        }
        
        result.append(" } ");
        
        return result.toString();
    }
    
    public static String getConstructQueryForKey(URI nextKey, boolean useSparqlGraph, String sparqlGraphUri)
    {
        StringBuilder result = new StringBuilder();
        
        result.append("CONSTRUCT { <"+nextKey.stringValue()+"> ?p ?o . } WHERE { ");
        
        if(useSparqlGraph)
        {
            result.append(" GRAPH <" + sparqlGraphUri + "> { ");
        }
        
        result.append(" <"+nextKey.stringValue()+"> ?p ?o . ");
        
        if(useSparqlGraph)
        {
            result.append(" } ");
        }
        
        result.append(" } ");
        
        return result.toString();
    }
    
    public static String getConstructQueryForObject(BaseQueryAllInterface nextObject, boolean useSparqlGraph, String sparqlGraphUri)
    {
        return getConstructQueryForKey(nextObject.getKey(), useSparqlGraph, sparqlGraphUri);
    }
    
    /**
     * @param nextValue
     * @return
     */
    public static Date getDateTimeFromValue(Value nextValue) throws java.text.ParseException
    {
        Date result;
        
        try
        {
            result = ((CalendarLiteralImpl)nextValue).calendarValue().toGregorianCalendar().getTime();
        }
        catch (final ClassCastException cce)
        {
            try
            {
                result = ((CalendarMemLiteral) nextValue).calendarValue().toGregorianCalendar().getTime();
            }
            catch (final ClassCastException cce2)
            {
                if(RdfUtils._DEBUG)
                {
                    RdfUtils.log
                            .debug("RdfUtils.getDateTimeFromValue: nextValue was not a typed date time literal. Trying to parse it as a string... type="+ nextValue.getClass().getName());
                }
                try
                {
                    result = RdfUtils.ISO8601UTC().parse(nextValue.toString());
                }
                catch(java.text.ParseException pe)
                {
                    log.error("Could not parse date: nextValue.toString="+nextValue.toString());
                    throw pe;
                }
            }
        }
        
        return result;
    }
    
    /**
     * @param nextRepository
     * @param predicateUris
     * @return
     * @throws OpenRDFException
     */
    public static Collection<String> getDistinctObjectUrisFromRepository(
            Repository nextRepository)//, Collection<String> predicateUris)
            throws OpenRDFException
    {
        final Collection<String> results = new HashSet<String>();
        
        if(RdfUtils._DEBUG)
        {
            RdfUtils.log
                    .debug("RdfUtils.getDistinctObjectsFromRepository: entering method");
            RdfUtils.log.debug(nextRepository);
            // RdfUtils.log.debug(predicateUris);
        }
        
        final RepositoryConnection con = nextRepository.getConnection();
        
        // try
        // {
            // 
            // final ValueFactory f = nextRepository.getValueFactory();
            
            // for(final String nextInputPredicate : predicateUris)
            // {
                // if((nextInputPredicate == null)
                        // || nextInputPredicate.trim().equals(""))
                // {
                    // if(RdfUtils._DEBUG)
                    // {
                        // RdfUtils.log
                                // .debug("RdfUtils.getDistinctObjectsFromRepository: nextInputPredicate was null or empty");
                    // }
                    // 
                    // continue;
                // }
                // 
                try
                {
                    // final URI nextInputPredicateUri = f
                            // .createURI(nextInputPredicate);
                    
                    final String queryString = "SELECT DISTINCT ?object WHERE { ?subject ?predicate ?object . FILTER(isURI(?object)) }";
                    final TupleQuery tupleQuery = con.prepareTupleQuery(
                            QueryLanguage.SPARQL, queryString);
                    final TupleQueryResult queryResult = tupleQuery.evaluate();
                    
                    try
                    {
                        while(queryResult.hasNext())
                        {
                            final BindingSet bindingSet = queryResult.next();
                            final Value valueOfObject = bindingSet
                                    .getValue("object");
                            
                            if(RdfUtils._DEBUG)
                            {
                                RdfUtils.log
                                        .debug("Utilities: found object: valueOfObject="
                                                + valueOfObject);
                            }
                            
                            results.add(getUTF8StringValueFromSesameValue(valueOfObject));
                        }
                    }
                    finally
                    {
                        queryResult.close();
                    }
                }
                catch (final OpenRDFException ordfe)
                {
                    RdfUtils.log
                            .error("RdfUtils.getDistinctObjectsFromRepository: RDF exception",ordfe);
                }
                catch (final Exception ex)
                {
                    RdfUtils.log
                            .error("RdfUtils.getDistinctObjectsFromRepository: general exception",ex);
                }
            // }
        // }
        
        // catch(OpenRDFException ordfe)
        // {
        // log.error("RdfUtils.getDistinctObjectsFromRepository: error found");
        // throw ordfe;
        // }
        // finally
        // {
            // if(con != null)
            // {
                // con.close();
            // }
        // }
        
        return results;
    }

    /**
     * @param nextRepository
     * @param predicateUris
     * @return
     * @throws OpenRDFException
     */
    public static Collection<String> getDistinctSubjectsFromRepository(
            Repository nextRepository)//, Collection<String> predicateUris)
            throws OpenRDFException
    {
        final Collection<String> results = new HashSet<String>();
        
        if(RdfUtils._DEBUG)
        {
            RdfUtils.log
                    .debug("RdfUtils.getDistinctSubjectsFromRepository: entering method");
            RdfUtils.log.debug(nextRepository);
            // RdfUtils.log.debug(predicateUris);
        }
        
        final RepositoryConnection con = nextRepository.getConnection();
        
        // try
        // {
            // 
            // final ValueFactory f = nextRepository.getValueFactory();
            
            // for(final String nextInputPredicate : predicateUris)
            // {
                // if((nextInputPredicate == null)
                        // || nextInputPredicate.trim().equals(""))
                // {
                    // if(RdfUtils._DEBUG)
                    // {
                        // RdfUtils.log
                                // .debug("RdfUtils.getDistinctSubjectsFromRepository: nextInputPredicate was null or empty");
                    // }
                    // 
                    // continue;
                // }
                // 
                try
                {
                    // final URI nextInputPredicateUri = f
                            // .createURI(nextInputPredicate);
                    
                    final String queryString = "SELECT DISTINCT ?subject WHERE { ?subject ?predicate ?object . }";
                    final TupleQuery tupleQuery = con.prepareTupleQuery(
                            QueryLanguage.SPARQL, queryString);
                    final TupleQueryResult queryResult = tupleQuery.evaluate();
                    
                    try
                    {
                        while(queryResult.hasNext())
                        {
                            final BindingSet bindingSet = queryResult.next();
                            final Value valueOfSubject = bindingSet
                                    .getValue("subject");
                            
                            if(RdfUtils._DEBUG)
                            {
                                RdfUtils.log
                                        .debug("Utilities: found subject: valueOfSubject="
                                                + valueOfSubject);
                            }
                            
                            results.add(getUTF8StringValueFromSesameValue(valueOfSubject));
                        }
                    }
                    finally
                    {
                        queryResult.close();
                    }
                }
                catch (final OpenRDFException ordfe)
                {
                    RdfUtils.log
                            .error("RdfUtils.getDistinctSubjectsFromRepository: RDF exception",ordfe);
                }
                catch (final Exception ex)
                {
                    RdfUtils.log
                            .error("RdfUtils.getDistinctSubjectsFromRepository: general exception",ex);
                }
            // }
        // }
        
        // catch(OpenRDFException ordfe)
        // {
        // log.error("RdfUtils.getDistinctSubjectsFromRepository: error found");
        // throw ordfe;
        // }
        // finally
        // {
            // if(con != null)
            // {
                // con.close();
            // }
        // }
        
        return results;
    }
    
    /**
     * @param nextValue
     * @return
     */
    public static int getIntegerFromValue(Value nextValue)
    {
        int result = 0;
        
        try
        {
            result = ((IntegerLiteralImpl) nextValue).intValue();
        }
        catch (final ClassCastException cce)
        {
            try
            {
                result = ((IntegerMemLiteral) nextValue).intValue();
            }
            catch (final ClassCastException cce2)
            {
                if(RdfUtils._DEBUG)
                {
                    RdfUtils.log
                            .debug("RdfUtils.getIntegerFromValue: nextValue was not a typed integer literal. Trying to parse it as a string... type="
                                    + nextValue.getClass().getName());
                }
                
                result = Integer.parseInt(nextValue.toString());
            }
        }
        
        return result;
    }
    
    /**
     * @param nextValue
     * @return
     */
    public static long getLongFromValue(Value nextValue)
    {
        long result = 0L;
        
        try
        {
            result = ((IntegerMemLiteral) nextValue).longValue();
        }
        catch (final ClassCastException cce)
        {
            RdfUtils.log.error("RdfUtils.getLongFromValue: nextValue was not a long numeric literal. Trying to parse it as a string... type="+ nextValue.getClass().getName());
            try
            {
                result = Long.parseLong(nextValue.stringValue());
            }
            catch(NumberFormatException nfe)
            {
                RdfUtils.log.error("RdfUtils.getLongFromValue: nextValue was not a long numeric literal. Trying to parse it as a string... type="+ nextValue.getClass().getName());
            }
        }
        
        return result;
    }
    
    /**
     * @param nextRepository
     * @param predicateUris
     * @return
     * @throws OpenRDFException
     */
    public static Collection<String> getObjectUrisFromRepositoryByPredicateUris(
            Repository nextRepository, Collection<String> predicateUris)
            throws OpenRDFException
    {
        final Collection<String> results = new HashSet<String>();
        
        if(RdfUtils._DEBUG)
        {
            RdfUtils.log
                    .debug("RdfUtils.getObjectUrisFromRepositoryByPredicateUris: entering method");
            RdfUtils.log.debug(nextRepository);
            RdfUtils.log.debug(predicateUris);
        }
        
        final RepositoryConnection con = nextRepository.getConnection();
        
        try
        {
            
            final ValueFactory f = nextRepository.getValueFactory();
            
            for(final String nextInputPredicate : predicateUris)
            {
                if((nextInputPredicate == null)
                        || nextInputPredicate.trim().equals(""))
                {
                    if(RdfUtils._DEBUG)
                    {
                        RdfUtils.log
                                .debug("RdfUtils.getObjectUrisFromRepositoryByPredicateUris: nextInputPredicate was null or empty");
                    }
                    
                    continue;
                }
                
                try
                {
                    final URI nextInputPredicateUri = f
                            .createURI(nextInputPredicate);
                    
                    final String queryString = "SELECT DISTINCT ?object WHERE { ?subject <"
                            + nextInputPredicateUri.stringValue()
                            + "> ?object . FILTER(isURI(?object)) }";
                    final TupleQuery tupleQuery = con.prepareTupleQuery(
                            QueryLanguage.SPARQL, queryString);
                    final TupleQueryResult queryResult = tupleQuery.evaluate();
                    
                    try
                    {
                        while(queryResult.hasNext())
                        {
                            final BindingSet bindingSet = queryResult.next();
                            final Value valueOfObject = bindingSet
                                    .getValue("object");
                            
                            if(RdfUtils._DEBUG)
                            {
                                RdfUtils.log
                                        .debug("Utilities: found object: valueOfObject="
                                                + valueOfObject);
                            }
                            
                            results.add(getUTF8StringValueFromSesameValue(valueOfObject));
                        }
                    }
                    finally
                    {
                        queryResult.close();
                    }
                }
                catch (final OpenRDFException ordfe)
                {
                    RdfUtils.log
                            .error("RdfUtils.getObjectUrisFromRepositoryByPredicateUris: RDF exception found for nextInputPredicate="
                                    + nextInputPredicate);
                }
                catch (final Exception ex)
                {
                    RdfUtils.log
                            .error("RdfUtils.getObjectUrisFromRepositoryByPredicateUris: general exception found for nextInputPredicate="
                                    + nextInputPredicate);
                }
            }
        }
        
        // catch(OpenRDFException ordfe)
        // {
        // log.error("RdfUtils.getObjectUrisFromRepositoryByPredicateUris: error found");
        // throw ordfe;
        // }
        finally
        {
            if(con != null)
            {
                con.close();
            }
        }
        
        return results;
    }
    
    public static Collection<QueryType> getQueryTypesForQueryBundles(Collection<QueryBundle> queryBundles, int modelVersion)
    {
        RdfFetchController fetchController = new RdfFetchController(queryBundles);
        
        try
        {
            fetchController.fetchRdfForQueries();
        }
        catch(InterruptedException ie)
        {
            log.fatal("QueryType: interrupted exception",ie);
            // throw ie;
        }
        
        Collection<RdfFetcherQueryRunnable> rdfResults = fetchController.successfulResults;
        
        Repository myRepository = null;
        RepositoryConnection myRepositoryConnection = null;
        try
        {
            myRepository = new SailRepository(new MemoryStore());
            myRepository.initialize();
            myRepositoryConnection = myRepository.getConnection();
            
            for(RdfFetcherQueryRunnable nextResult : rdfResults)
            {
                try
                {
                    RDFFormat nextReaderFormat = RDFFormat.forMIMEType(nextResult.returnedMIMEType);
                    
                    if(log.isDebugEnabled())
                    {
                        log.debug("QueryType.fetchQueryTypeByKey: nextReaderFormat for returnedContentType="+nextResult.returnedContentType+" nextReaderFormat="+nextReaderFormat);
                    }
                    
                    if(nextReaderFormat == null)
                    {
                        nextReaderFormat = Rio.getParserFormatForMIMEType(Settings.getSettings().getStringPropertyFromConfig("assumedRequestContentType"));
                        
                        if(nextReaderFormat == null)
                        {
                            log.error("QueryType.fetchQueryTypeByKey: Not attempting to parse result because Settings.getStringPropertyFromConfig(\"assumedRequestContentType\") isn't supported by Rio and the returned content type wasn't either nextResult.returnedMIMEType="+nextResult.returnedMIMEType+" Settings.getStringPropertyFromConfig(\"assumedRequestContentType\")="+Settings.getSettings().getStringPropertyFromConfig("assumedRequestContentType"));
                            continue;
                        }
                        else
                        {
                            log.warn("QueryType.fetchQueryTypeByKey: readerFormat NOT matched for returnedMIMEType="+nextResult.returnedMIMEType+" using configured preferred content type as fallback Settings.getStringPropertyFromConfig(\"assumedRequestContentType\")="+Settings.getSettings().getStringPropertyFromConfig("assumedRequestContentType"));
                        }
                    }
                    else if(log.isDebugEnabled())
                    {
                        log.debug("QueryType.fetchQueryTypeByKey: readerFormat matched for returnedMIMEType="+nextResult.returnedMIMEType);
                    }
                    
                    if(nextResult.normalisedResult.length() > 0)
                    {
                        myRepositoryConnection.add(new java.io.StringReader(nextResult.normalisedResult), Settings.getSettings().getDefaultHostAddress(), nextReaderFormat);
                    }
                }
                catch(org.openrdf.rio.RDFParseException rdfpe)
                {
                    log.error("QueryType.fetchQueryTypeByKey: RDFParseException",rdfpe);
                }
                catch(org.openrdf.repository.RepositoryException re)
                {
                    log.error("QueryType.fetchQueryTypeByKey: RepositoryException inner",re);
                }
                catch(java.io.IOException ioe)
                {
                    log.error("QueryType.fetchQueryTypeByKey: IOException",ioe);
                }
            } // end for(RdfFetcherQueryRunnable nextResult : rdfResults)
        }
        catch(org.openrdf.repository.RepositoryException re)
        {
            log.error("QueryType.fetchQueryTypeByKey: RepositoryException outer",re);
        }
        finally
        {
            try
            {
                if(myRepositoryConnection != null)
                    myRepositoryConnection.close();
            }
            catch(org.openrdf.repository.RepositoryException re2)
            {
                log.fatal("QueryType.fetchQueryTypeByKey: failed to close repository connection", re2);
            }
        }
        
        Map<URI, QueryType> results = null;
        
        try
        {
            results = QueryTypeImpl.getCustomQueriesFromRepository(myRepository, modelVersion);
        }
        catch(org.openrdf.repository.RepositoryException re)
        {
            log.fatal("QueryType.fetchQueryTypeByKey: failed to get records due to a repository exception", re);
        }
        
        return results.values();
    }
    
    /**
     * @return a SPARQL Update language query that will either insert or delete triples about rdfObject
     * @throws OpenRDFException
     */
    public static String getSparulQueryForObject(
        BaseQueryAllInterface rdfObject, 
        boolean isInsert,
        boolean isDelete,
        boolean useSparqlGraph, 
        String sparqlGraphUri)
    throws OpenRDFException
    {
        final Repository myRepository = new SailRepository(new MemoryStore());
        myRepository.initialize();
        
        // FIXME: fix this reliance on the Settings class
        final boolean rdfOkay = rdfObject.toRdf(myRepository, rdfObject.getKey(), Settings.CONFIG_API_VERSION);
        
        if(!rdfOkay && isInsert)
        {
            if(_DEBUG)
            {
                log.debug("RdfUtils.getSparulQueryForObject: could not convert to RDF");
            }
            
            return "";
        }
        
        final RDFFormat writerFormat = Rio.getWriterFormatForMIMEType("text/plain");
        
        final StringWriter insertTriples = new StringWriter();
        
        if(isInsert)
        {
            RdfUtils.toWriter(myRepository, insertTriples, writerFormat);
            
            log.debug("RdfUtils.getSparulQueryForObject: insertTriples.toString()="+insertTriples.toString());
        }
        else if(_DEBUG)
        {
            log.debug("RdfUtils.getSparulQueryForObject: isInsert was false");
        }
        
        // NOTE: this looks messy because it is. 
        // SPARUL doesn't play nicely if you don't know whether the delete will delete any triples,
        // and empty blocks are mandatory for the MODIFY statement if they are not applicable
        // The define sql:log-enable is a Virtuoso hack to enable SPARUL to work with more than one thread at once
        String sparqlInsertQuery = "define sql:log-enable 2 MODIFY ";
        
        if(useSparqlGraph)
        {
            sparqlInsertQuery += " GRAPH <"
                    + sparqlGraphUri + "> ";
        }
        
        if(isDelete)
        {
            sparqlInsertQuery += " DELETE { <" + rdfObject.getKey() + "> ?p ?o . } ";
        }
        else
        {
            sparqlInsertQuery += " DELETE { } ";
        }
        
        // NOTE: insertTriples will be an empty string if isInsert is false
        sparqlInsertQuery += " INSERT { " + insertTriples.toString() + " } ";
        
        if(isDelete)
        {
            sparqlInsertQuery += " WHERE { <" + rdfObject.getKey() + "> ?p ?o . } ";
        }
        
        if(_DEBUG)
        {
            log.debug("RdfUtils.getInsertQueryForObject: sparqlInsertQuery="
                    + sparqlInsertQuery);
        }
        
        return sparqlInsertQuery;
    }
    
    public static Collection<Statement> getStatementsFromRepositoryByPredicateUris(
            Repository nextRepository, Collection<URI> predicateUris)
            throws OpenRDFException
    {
        final Collection<Statement> results = new HashSet<Statement>();
        if(RdfUtils._DEBUG)
        {
            RdfUtils.log
                    .debug("RdfUtils.getStatementsFromRepositoryByPredicateUris: entering method");
            RdfUtils.log.debug(nextRepository);
            RdfUtils.log.debug(predicateUris);
        }
        
        final RepositoryConnection con = nextRepository.getConnection();
        
        try
        {
            for(final URI nextInputPredicateUri : predicateUris)
            {
                try
                {
                    final String queryString = "CONSTRUCT { ?subject <"
                            + nextInputPredicateUri.stringValue()
                            + "> ?object . } WHERE { ?subject <"
                            + nextInputPredicateUri.stringValue()
                            + "> ?object . }";
                    final GraphQuery tupleQuery = con.prepareGraphQuery(
                            QueryLanguage.SPARQL, queryString);
                    final GraphQueryResult queryResult = tupleQuery.evaluate();
                    
                    try
                    {
                        while(queryResult.hasNext())
                        {
                            final Statement bindingSet = queryResult.next();
                            // final Value valueOfObject = bindingSet.getValue("object");
                            
                            // if(RdfUtils._DEBUG)
                            // {
                                // RdfUtils.log
                                        // .debug("Utilities: found object: valueOfObject="
                                                // + valueOfObject);
                            // }
                            
                            results.add(bindingSet);
                            // results.add(getUTF8StringValueFromSesameValue(valueOfObject));
                        }
                    }
                    finally
                    {
                        queryResult.close();
                    }
                }
                catch (final OpenRDFException ordfe)
                {
                    RdfUtils.log
                            .error("RdfUtils.getValuesFromRepositoryByPredicateUris: RDF exception found for nextInputPredicate="
                                    + nextInputPredicateUri.stringValue());
                }
                catch (final Exception ex)
                {
                    RdfUtils.log
                            .error("RdfUtils.getValuesFromRepositoryByPredicateUris: general exception found for nextInputPredicate="
                                    + nextInputPredicateUri.stringValue());
                }
            }
        }
        
        // catch(OpenRDFException ordfe)
        // {
        // log.error("RdfUtils.getValuesFromRepositoryByPredicateUris: error found");
        // throw ordfe;
        // }
        finally
        {
            if(con != null)
            {
                con.close();
            }
        }
        
        return results;
    }
    
    public static Collection<Statement> getStatementsFromRepositoryByPredicateUrisAndSubject(
            Repository nextRepository, Collection<URI> predicateUris, URI subjectUri)
            throws OpenRDFException
    {
        
        Collection<Statement> results = new HashSet<Statement>();
        
        Collection<Statement> tempResults = getStatementsFromRepositoryByPredicateUris(nextRepository, predicateUris);
        
        for(Statement nextTempResults : tempResults)
        {
            if(nextTempResults.getSubject().equals(subjectUri))
            {
                results.add(nextTempResults);
            }
        }
        
        return results;
    }
    
    
    
    public static Collection<Statement> getStatementsFromRepositoryByPredicateUrisAndSubject(
            Repository nextRepository, URI predicateUri, URI subjectUri)
            throws OpenRDFException
    {
        Collection<URI> predicateUris = new HashSet<URI>();
        predicateUris.add(predicateUri);
        
        return getStatementsFromRepositoryByPredicateUrisAndSubject(nextRepository, predicateUris, subjectUri);
    }
    
    public static String getUTF8StringValueFromSesameValue(Value nextValue)
    {
        try
        {
            return new String(nextValue.stringValue().getBytes(), "utf-8");
        }
        catch(java.io.UnsupportedEncodingException uee)
        {
            throw new RuntimeException("Utilities: UTF-8 is not supported by this java vm!!!", uee);
        }
    }
    
    /**
     * @param nextRepository
     * @param predicateUris
     * @return
     * @throws OpenRDFException
     */
    public static Collection<Value> getValuesFromRepositoryByPredicateUris(
            Repository nextRepository, Collection<URI> predicateUris)
            throws OpenRDFException
    {
        final Collection<Value> results = new HashSet<Value>();
        
        Collection<Statement> relevantStatements = getStatementsFromRepositoryByPredicateUris(nextRepository, predicateUris);

        for(Statement nextStatement : relevantStatements)
        {
            results.add(nextStatement.getObject());
        }
        
        return results;
        
    }
    
    public static Collection<Value> getValuesFromRepositoryByPredicateUrisAndSubject(
            Repository nextRepository, Collection<URI> predicateUris, URI subjectUri)
            throws OpenRDFException
    {
        final Collection<Value> results = new HashSet<Value>();
        
        if(RdfUtils._DEBUG)
        {
            RdfUtils.log
                    .debug("RdfUtils.getValuesFromRepositoryByPredicateUrisAndSubject: entering method");
            RdfUtils.log.debug(nextRepository);
            RdfUtils.log.debug(predicateUris);
        }
        
        final RepositoryConnection con = nextRepository.getConnection();
        
        try
        {
            for(final URI nextInputPredicateUri : predicateUris)
            {
                try
                {
                    final String queryString = "CONSTRUCT { <"+subjectUri.stringValue() +"> <"
                            + nextInputPredicateUri.stringValue()
                            + "> ?object } WHERE { <"+subjectUri.stringValue() +"> <"
                            + nextInputPredicateUri.stringValue()
                            + "> ?object . }";
                    final GraphQuery tupleQuery = con.prepareGraphQuery(
                            QueryLanguage.SPARQL, queryString);
                    final GraphQueryResult queryResult = tupleQuery.evaluate();

                    if(_DEBUG)
                        RdfUtils.log.debug("w: queryString="+queryString);                   

                    try
                    {
                        while(queryResult.hasNext())
                        {
                            final Statement nextStatement = queryResult.next();
                            
                            if(_DEBUG)
                                RdfUtils.log.debug("RdfUtils.getValuesFromRepositoryByPredicateUrisAndSubject: nextStatement="+nextStatement);                   

                            results.add(nextStatement.getObject());
                            
                            // if(RdfUtils._DEBUG)
                            // {
                                // RdfUtils.log
                                        // .debug("Utilities: found object: valueOfObject="
                                                // + valueOfObject);
                            // }
                            
                            // results.add(new MemStatement(subjectUri, nextInputPredicateUri, valueOfObject, null, false, 0));
                            // results.add(getUTF8StringValueFromSesameValue(valueOfObject));
                        }
                    }
                    finally
                    {
                        queryResult.close();
                    }
                }
                catch (final OpenRDFException ordfe)
                {
                    RdfUtils.log
                            .error("RdfUtils.getValuesFromRepositoryByPredicateUrisAndSubject: RDF exception found for nextInputPredicate="
                                    + nextInputPredicateUri.stringValue()+" ordfe.class"+ordfe.getClass().getName(), ordfe);
                }
                catch (final Exception ex)
                {
                    RdfUtils.log
                            .error("RdfUtils.getValuesFromRepositoryByPredicateUrisAndSubject: general exception found for nextInputPredicate="
                                    + nextInputPredicateUri.stringValue(), ex);
                }
            }
        }
        
        // catch(OpenRDFException ordfe)
        // {
        // log.error("RdfUtils.getValuesFromRepositoryByPredicateUris: error found");
        // throw ordfe;
        // }
        finally
        {
            if(con != null)
            {
                con.close();
            }
        }
        // log.info("RdfUtils.getValuesFromRepositoryByPredicateUrisAndSubject: results.size()="+results.size());
        return results;
        
    }
    
    /**
     * @param nextRepository
     * @param predicateUris
     * @return
     * @throws OpenRDFException
     */
    public static Collection<Value> getValuesFromRepositoryByPredicateUrisAndSubject(
            Repository nextRepository, URI predicateUri, URI subjectUri)
            throws OpenRDFException
    {
        Collection<URI> predicateUris = new HashSet<URI>();
        
        predicateUris.add(predicateUri);
        
        return getValuesFromRepositoryByPredicateUrisAndSubject(nextRepository, predicateUris, subjectUri);
    }
    
    public static void insertResultIntoRepository(RdfFetcherQueryRunnable nextResult, Repository myRepository) throws RepositoryException, java.io.IOException
    {
        if(_DEBUG)
        {
            log.debug("Utilities: nextResult.toString()="+nextResult.toString());
        }
        
        RepositoryConnection myRepositoryConnection = null;
        
        try
        {
            myRepositoryConnection = myRepository.getConnection();
            
            RDFFormat nextReaderFormat = RDFFormat.forMIMEType(nextResult.returnedMIMEType);
            
            if(_DEBUG)
            {
                log.debug("Utilities: nextReaderFormat for returnedContentType="+nextResult.returnedContentType+" nextReaderFormat="+nextReaderFormat);
            }
            
            if(nextReaderFormat == null)
            {
                nextReaderFormat = Rio.getParserFormatForMIMEType(Settings.getSettings().getStringPropertyFromConfig("assumedRequestContentType"));
                
                if(nextReaderFormat == null)
                {
                    log.error("Utilities: Not attempting to parse result because Settings.getStringPropertyFromConfig(\"assumedRequestContentType\") isn't supported by Rio and the returned content type wasn't either nextResult.returnedMIMEType="+nextResult.returnedMIMEType+" Settings.getStringPropertyFromConfig(\"assumedRequestContentType\")="+Settings.getSettings().getStringPropertyFromConfig("assumedRequestContentType"));
                    //throw new RuntimeException("Utilities: Not attempting to parse because there are no content types to use for interpretation");
                }
                else if(nextResult.wasSuccessful)
                {
                    log.warn("Utilities: readerFormat NOT matched for returnedMIMEType="+nextResult.returnedMIMEType+" using configured preferred content type as fallback Settings.getStringPropertyFromConfig(\"assumedRequestContentType\")="+Settings.getSettings().getStringPropertyFromConfig("assumedRequestContentType"));
                }
            }
            else if(_DEBUG)
            {
                log.debug("Utilities: readerFormat matched for returnedMIMEType="+nextResult.returnedMIMEType);
            }
            
            if(_DEBUG)
            {
                log.debug("Utilities: nextResult.normalisedResult.length()="+nextResult.normalisedResult.length());
            }
            
            if(_TRACE)
            {
                log.trace("Utilities: nextResult.normalisedResult="+nextResult.normalisedResult);
            }
            
            if(nextReaderFormat != null && nextResult.normalisedResult.length() > 0)
            {
                myRepositoryConnection.add(new java.io.StringReader(nextResult.normalisedResult), Settings.getSettings().getDefaultHostAddress(), nextReaderFormat);
                
                myRepositoryConnection.commit();
            }
            
            if(_DEBUG)
            {
                log.debug("RdfUtils.insertResultIntoRepository: myRepositoryConnection.size()="+myRepositoryConnection.size());
            }
        }
        catch(org.openrdf.rio.RDFParseException rdfpe)
        {
            log.error("Utilities: RDFParseException result: nextResult.endpointUrl="+nextResult.endpointUrl+" message="+rdfpe.getMessage());
            
            if(_TRACE)
            {
                log.debug("Utilities: RDFParseException result: normalisedResult="+nextResult.normalisedResult);
            }
        }
        finally
        {
            if(myRepositoryConnection != null)
            {
                try
                {
                    myRepositoryConnection.close();
                }
                catch(Exception ex)
                {
                    log.error("Utilities: finally section, caught exception",ex);
                }
            }
        }
    }
    
    public static void insertResultsIntoRepository(Collection<RdfFetcherQueryRunnable> results, Repository myRepository) throws RepositoryException, java.io.IOException
    {
        for(RdfFetcherQueryRunnable nextResult : results)
		{
		    insertResultIntoRepository(nextResult, myRepository);
		}
	}
    
    // Why can't these objects be thread safe???????
    // TODO: find a thread-safe date formatting library and use it instead
    public static SimpleDateFormat ISO8601UTC()
    {
        final SimpleDateFormat result = new SimpleDateFormat(DATE_FORMAT);
        result.setTimeZone(TimeZone.getTimeZone(TIME_ZOME));
        return result;
    }
    
    public static void retrieveUrls(Collection<String> retrievalUrls, String defaultResultFormat, Repository myRepository) throws RepositoryException, java.io.IOException , InterruptedException
    {
        retrieveUrls(retrievalUrls, defaultResultFormat, myRepository, true);
    }
	
    public static void retrieveUrls(Collection<String> retrievalUrls, String defaultResultFormat, Repository myRepository, boolean inParallel) throws RepositoryException, java.io.IOException , InterruptedException
    {
        Collection<RdfFetcherQueryRunnable> retrievalThreads = new HashSet<RdfFetcherQueryRunnable>();
        
        for(String nextLocation : retrievalUrls)
        {
            RdfFetcherQueryRunnable nextThread = new RdfFetcherUriQueryRunnable( nextLocation,
                         defaultResultFormat,
                         "",
                         "",
                         defaultResultFormat,
                         new QueryBundle());
            
            retrievalThreads.add(nextThread);
        }
        
        for(RdfFetcherQueryRunnable nextThread : retrievalThreads)
        {
            nextThread.start();
            
            if(!inParallel)
            {
                // TODO: make it possible for users to configure either serial or parallel querying
                try
                {
                    nextThread.join();
                }
                catch( InterruptedException ie )
                {
                    log.error( "RdfFetchController.fetchRdfForQuery: caught interrupted exception message="+ie.getMessage() );
                    throw ie;
                }
            }
        }
        
        if(inParallel)
        {
            for(RdfFetcherQueryRunnable nextThread : retrievalThreads)
            {
                try
                {
                    nextThread.join();
                }
                catch( InterruptedException ie )
                {
                    log.error( "RdfFetchController.fetchRdfForQuery: caught interrupted exception message="+ie.getMessage() );
                    throw ie;
                }
            }
        }
        
        insertResultsIntoRepository(retrievalThreads, myRepository);
        
    }
    
    public static void retrieveUrls(String retrievalUrl, String defaultResultFormat, Repository myRepository) throws RepositoryException, java.io.IOException , InterruptedException
    {
        Collection<String> retrievalList = new HashSet<String>();
        retrievalList.add(retrievalUrl);
        
        retrieveUrls(retrievalList, defaultResultFormat, myRepository, true);
    }
    
    /**
     * @param nextRepository
     * @param outputStream
     */
    public static void toOutputStream(Repository nextRepository,
            java.io.OutputStream outputStream)
    {
        RdfUtils.toOutputStream(nextRepository, outputStream, RDFFormat.RDFXML);
    }
    
    /**
     * @param nextRepository
     * @param outputStream
     * @param format
     */
    public static void toOutputStream(Repository nextRepository,
            java.io.OutputStream outputStream, RDFFormat format)
    {
        RepositoryConnection nextConnection = null;
        
        try
        {
            nextConnection = nextRepository.getConnection();
            
            nextConnection.export(Rio.createWriter(format, outputStream));
        }
        catch (final RepositoryException e)
        {
            RdfUtils.log.error(e.toString());
        }
        catch (final RDFHandlerException e)
        {
            RdfUtils.log.error(e.toString());
        }
        finally
        {
            try
            {
                if(nextConnection != null)
                {
                    nextConnection.close();
                }
            }
            catch(RepositoryException rex)
            {
                log.error("RdfUtils.toWriter: connection didn't close correctly",rex);
            }
        }
    }
    
    /**
     * @param nextConnection
     * @return
     */
    public static String toString(Repository nextRepository)
    {
        final java.io.StringWriter stBuff = new java.io.StringWriter();

        RepositoryConnection nextConnection = null;
        
        try
        {
            nextConnection = nextRepository.getConnection();
            
            nextConnection.export(Rio.createWriter(RDFFormat.RDFXML, stBuff));
        }
        catch (final RepositoryException e)
        {
            RdfUtils.log.error(e.toString());
        }
        catch (final RDFHandlerException e)
        {
            RdfUtils.log.error(e.toString());
        }
        finally
        {
            try
            {
                if(nextConnection != null)
                {
                    nextConnection.close();
                }
            }
            catch(RepositoryException rex)
            {
                log.error("RdfUtils.toWriter: connection didn't close correctly",rex);
            }
        }
            
        return stBuff.toString();
    }
    
    /**
     * @param nextRepository
     * @param nextWriter
     */
    public static void toWriter(Repository nextRepository,
            java.io.Writer nextWriter)
    {
        RdfUtils.toWriter(nextRepository, nextWriter, RDFFormat.RDFXML);
    }

    /**
     * @param nextRepository
     * @param nextWriter
     * @param format
     */
    public static void toWriter(Repository nextRepository, java.io.Writer nextWriter, RDFFormat format)
    {
        RepositoryConnection nextConnection = null;
        
        try
        {
            nextConnection = nextRepository.getConnection();
            
            nextConnection.export(Rio.createWriter(format, nextWriter));
        }
        catch (final RepositoryException e)
        {
            RdfUtils.log.error(e.toString());
        }
        catch (final RDFHandlerException e)
        {
            RdfUtils.log.error(e.toString());
        }
        finally
        {
            try
            {
                if(nextConnection != null)
                {
                    nextConnection.close();
                }
            }
            catch(RepositoryException rex)
            {
                log.error("RdfUtils.toWriter: connection didn't close correctly",rex);
            }
        }
    }
    
    // from http://java.sun.com/developer/technicalArticles/ThirdParty/WebCrawler/WebCrawler.java
    // License at http://developers.sun.com/license/berkeley_license.html
    @SuppressWarnings("unused")
    public boolean robotSafe(URL url) 
    {
        final String DISALLOW = "Disallow:";
        String strHost = url.getHost();
        
        // TODO: Implement me!!!
        return true;
        /*****
        // form URL of the robots.txt file
        String strRobot = "http://" + strHost + "/robots.txt";
        URL urlRobot;
        try 
        { 
            urlRobot = new URL(strRobot);
        } 
        catch (MalformedURLException e) 
        {
            // something weird is happening, so don't trust it
            return false;
        }
        
        String strCommands;
        
        try 
        {
            InputStream urlRobotStream = urlRobot.openStream();
            
            // read in entire file
            byte b[] = new byte[10000];
            int numRead = urlRobotStream.read(b);
            strCommands = new String(b, 0, numRead);
            while (numRead != -1) 
            {
                if (Thread.currentThread() != searchThread)
                    break;
                numRead = urlRobotStream.read(b);
                if (numRead != -1) 
                {
                    String newCommands = new String(b, 0, numRead);
                    strCommands += newCommands;
                }
            }
            urlRobotStream.close();
        } 
        catch (IOException e) 
        {
            // if there is no robots.txt file, it is OK to search
            return true;
        }
        
        // assume that this robots.txt refers to us and 
        // search for "Disallow:" commands.
        String strURL = url.getFile();
        int index = 0;
        while ((index = strCommands.indexOf(DISALLOW, index)) != -1) 
        {
            index += DISALLOW.length();
            String strPath = strCommands.substring(index);
            StringTokenizer st = new StringTokenizer(strPath);
            
            if (!st.hasMoreTokens())
            break;
            
            String strBadPath = st.nextToken();
            
            // if the URL starts with a disallowed path, it is not safe
            if (strURL.indexOf(strBadPath) == 0)
            return false;
        }
        
        return true;
        *****/
    }
    
}