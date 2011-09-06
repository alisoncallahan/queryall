/**
 * 
 */
package org.queryall.api.querytype;

import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.queryall.api.utils.Constants;
import org.queryall.api.utils.QueryAllNamespaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class RdfXmlOutputQueryTypeSchema
{
    private static final Logger log = LoggerFactory.getLogger(RdfXmlOutputQueryTypeSchema.class);
    @SuppressWarnings("unused")
    private static final boolean _TRACE = RdfXmlOutputQueryTypeSchema.log.isTraceEnabled();
    @SuppressWarnings("unused")
    private static final boolean _DEBUG = RdfXmlOutputQueryTypeSchema.log.isDebugEnabled();
    @SuppressWarnings("unused")
    private static final boolean _INFO = RdfXmlOutputQueryTypeSchema.log.isInfoEnabled();
    
    private static URI rdfXmlOutputQueryTypeUri;
    private static URI queryOutputRdfXmlString;
    
    static
    {
        final ValueFactory f = Constants.valueFactory;
        
        final String baseUri = QueryAllNamespaces.QUERY.getBaseURI();
        
        RdfXmlOutputQueryTypeSchema.setRdfXmlOutputQueryTypeUri(f.createURI(baseUri, "RdfXmlOutputQuery"));
        RdfXmlOutputQueryTypeSchema.setQueryOutputRdfXmlString(f.createURI(baseUri, "outputRdfXmlString"));
    }
    
    /**
     * @return the queryOutputRdfXmlString
     */
    public static URI getQueryOutputRdfXmlString()
    {
        return RdfXmlOutputQueryTypeSchema.queryOutputRdfXmlString;
    }
    
    /**
     * @return the queryTypeUri
     */
    public static URI getRdfXmlOutputQueryTypeUri()
    {
        return RdfXmlOutputQueryTypeSchema.rdfXmlOutputQueryTypeUri;
    }
    
    public static boolean schemaToRdf(final Repository myRepository, final URI keyToUse, final int modelVersion)
        throws OpenRDFException
    {
        final RepositoryConnection con = myRepository.getConnection();
        
        final ValueFactory f = Constants.valueFactory;
        
        try
        {
            final URI contextKeyUri = keyToUse;
            con.setAutoCommit(false);
            
            con.add(RdfXmlOutputQueryTypeSchema.getRdfXmlOutputQueryTypeUri(), RDF.TYPE, OWL.CLASS, contextKeyUri);
            
            con.add(RdfXmlOutputQueryTypeSchema.getQueryOutputRdfXmlString(), RDF.TYPE, OWL.DATATYPEPROPERTY,
                    contextKeyUri);
            con.add(RdfXmlOutputQueryTypeSchema.getQueryOutputRdfXmlString(), RDFS.RANGE, RDFS.LITERAL, contextKeyUri);
            con.add(RdfXmlOutputQueryTypeSchema.getQueryOutputRdfXmlString(), RDFS.DOMAIN,
                    SparqlProcessorQueryTypeSchema.getSparqlProcessorQueryTypeUri(), contextKeyUri);
            con.add(RdfXmlOutputQueryTypeSchema.getQueryOutputRdfXmlString(), RDFS.LABEL, f.createLiteral("."),
                    contextKeyUri);
            
            // If everything went as planned, we can commit the result
            con.commit();
            
            return true;
        }
        catch(final RepositoryException re)
        {
            // Something went wrong during the transaction, so we roll it back
            
            if(con != null)
            {
                con.rollback();
            }
            
            RdfXmlOutputQueryTypeSchema.log.error("RepositoryException: " + re.getMessage());
        }
        finally
        {
            if(con != null)
            {
                con.close();
            }
        }
        
        return false;
    }
    
    /**
     * @param queryOutputRdfXmlString
     *            the queryOutputRdfXmlString to set
     */
    public static void setQueryOutputRdfXmlString(final URI queryOutputRdfXmlString)
    {
        RdfXmlOutputQueryTypeSchema.queryOutputRdfXmlString = queryOutputRdfXmlString;
    }
    
    /**
     * @param queryTypeUri
     *            the queryTypeUri to set
     */
    public static void setRdfXmlOutputQueryTypeUri(final URI queryTypeUri)
    {
        RdfXmlOutputQueryTypeSchema.rdfXmlOutputQueryTypeUri = queryTypeUri;
    }
    
}
