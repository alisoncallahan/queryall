package org.queryall.impl;

import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Literal;
import org.openrdf.model.ValueFactory;

import org.openrdf.OpenRDFException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.sail.memory.model.MemValueFactory;

import java.util.Collection;
import java.util.HashSet;

import org.queryall.Template;
import org.queryall.helpers.*;

import org.apache.log4j.Logger;

public class TemplateImpl extends Template
{
    private static final Logger log = Logger.getLogger(Template.class.getName());
    private static final boolean _TRACE = log.isTraceEnabled();
    private static final boolean _DEBUG = log.isDebugEnabled();
    @SuppressWarnings("unused")
	private static final boolean _INFO = log.isInfoEnabled();
    
    private static final String defaultNamespace = Settings.DEFAULT_RDF_TEMPLATE_NAMESPACE;
    
    private Collection<Statement> unrecognisedStatements = new HashSet<Statement>();
    
    private URI key;
    // Many different template types might match for this one... This should match the initial part of the template that is relevant to the required parameter
    // Some examples of this are:
    // key=http://bio2rdf.org/template:xmlEncoded_input_NN matchRegex="xmlEncoded_(input_\d+)" matches xmlEncoded_input_1
    // it is a native function with reference http://bio2rdf.org/nativetemplate:xmlEncoded
    // and then attempts to match its referenced templates using the (.+) group 
    // the last referenced template is applied to the inputs first, before moving back through the chain 
    // to eventually get to templates which are referenced directly by QueryTypes or other elements
    private String matchRegex = "";
    // if this template is marked as a native function, the implementation should recognise the native function URI and treat it accordingly
    // native functions may have referenced templates, which may or may not be native functions
    // An example of native functions are the input_NN set which are recognised and replaced
    // Other native functions also include the xmlEncoded_, privateuppercase_, privatelowercase_, urlEncoded_ ntriplesEncoded_, endpointSpecific_
    private boolean isNativeFunction = false;
    // if isNativeFunction is true, then the following will contain the URI matching the implemented function
    private String nativeFunctionUri = "";
    private URI curationStatus = ProjectImpl.projectNotCuratedUri;
    
    // each of the following templates must be applied to this template before returning, additionally, 
    // any parameters that are not declared here which are found in the contextual parameter list must also be included
    private Collection<URI> referencedTemplates = new HashSet<URI>();
    
    // The actual template string for this part is included here
    // It may be empty in native implementations, but otherwise it should contain references to the string which contains refenced template prefixes
    private String templateString = "";
    
    // This is the content type of this template
    // if it is a plain string then use text/plain, 
    // even though this has been erroneously overloaded to mean NTriples in SemWeb circles
    // for SPARQL queries use application/sparql-query
    // for SPARQL results templates use application/sparql-results+xml and application/sparql-results+json, although these will not generate results that can be included in RDF documents
    // Also this to indicate whether the template is a basic application/rdf+xml 
    // or text/rdf+n3 template that may be used for output or insertion into an endpoint as necessary
    private String contentType = "";
    
    private int order = 100;
    
    public static URI templateTypeUri = null;
    
    public static URI templateContentTypeSparqlQuery = null;
    public static URI templateContentTypeSparqlResultsXml = null;
    public static URI templateContentTypeSparqlResultsJson = null;
    public static URI templateContentTypeRdfXml = null;
    public static URI templateContentTypeN3 = null;
    public static URI templateContentTypePlainText = null;
    public static URI templateContentType = null;
    public static URI templateReferencedTemplate = null;
    public static URI templateMatchRegex = null;
    public static URI templateIsNativeFunction = null;
    public static URI templateNativeFunctionUri = null;
    public static URI templateTemplateString = null;
    public static URI templateOrder = null;
    
    public static String templateNamespace;
    
    static
    {
        ValueFactory f = new MemValueFactory();
        
        templateNamespace = Settings.DEFAULT_ONTOLOGYTERMURI_PREFIX
                         +Settings.DEFAULT_RDF_TEMPLATE_NAMESPACE
                         +Settings.DEFAULT_ONTOLOGYTERMURI_SUFFIX;
                         
        templateTypeUri = f.createURI(templateNamespace+"Template");
        templateContentTypeSparqlQuery = f.createURI(templateNamespace+"ContentTypeSparqlQuery");
        templateContentTypeSparqlResultsXml = f.createURI(templateNamespace+"ContentTypeSparqlResultsXml");
        templateContentTypeSparqlResultsJson = f.createURI(templateNamespace+"ContentTypeSparqlResultsJson");
        templateContentTypeRdfXml = f.createURI(templateNamespace+"ContentTypeRdfXml");
        templateContentTypeN3 = f.createURI(templateNamespace+"ContentTypeN3");
        templateContentTypePlainText = f.createURI(templateNamespace+"ContentTypePlainText");
        templateContentType = f.createURI(templateNamespace+"contentType");
        templateReferencedTemplate = f.createURI(templateNamespace+"referencedTemplate");
        
        templateMatchRegex = f.createURI(templateNamespace+"matchRegex");
        templateIsNativeFunction = f.createURI(templateNamespace+"isNativeFunction");
        templateNativeFunctionUri = f.createURI(templateNamespace+"nativeFunctionUri");
        templateTemplateString = f.createURI(templateNamespace+"templateString");
        templateContentType = f.createURI(templateNamespace+"contentType");
        templateOrder = f.createURI(templateNamespace+"order");
    }
    
    
    // keyToUse is the URI of the next instance that can be found in myRepository
    // returns null if the URI is not in the repository or the information is not enough to create a minimal query configuration
    public static Template fromRdf(Collection<Statement> inputStatements, URI keyToUse, int modelVersion) throws OpenRDFException
    {
        Template result = new TemplateImpl();
        
        boolean resultIsValid = false;
        
        for(Statement nextStatement : inputStatements)
        {
            if(_DEBUG)
            {
                log.debug("Template: nextStatement: "+nextStatement.toString());
            }
            
            if(nextStatement.getPredicate().equals(RDF.TYPE) && nextStatement.getObject().equals(templateTypeUri))
            {
                if(_TRACE)
                {
                    log.trace("Template: found valid type predicate for URI: "+keyToUse);
                }
                
                resultIsValid = true;
                result.setKey(keyToUse);
            }
            else if(nextStatement.getPredicate().equals(ProjectImpl.projectCurationStatusUri))
            {
                result.setCurationStatus((URI)nextStatement.getObject());
            }
            else
            {
                result.addUnrecognisedStatement(nextStatement);
            }
        }
        
        if(_DEBUG)
        {
            log.debug("Template.fromRdf: would have returned... keyToUse="+keyToUse+" result="+result.toString());
        }
        
        
        if(resultIsValid)
        {
            return result;
        }
        else
        {
            throw new RuntimeException("Template.fromRdf: result was not valid keyToUse="+keyToUse);
        }
    }
    

    public boolean toRdf(Repository myRepository, URI keyToUse, int modelVersion) throws OpenRDFException
    {
        RepositoryConnection con = myRepository.getConnection();
        
        ValueFactory f = myRepository.getValueFactory();
        
        try
        {
            // create some resources and literals to make statements out of
            URI templateInstanceUri = keyToUse;
            
            Literal matchRegexLiteral = f.createLiteral(matchRegex);
            Literal isNativeFunctionLiteral = f.createLiteral(isNativeFunction);
            Literal templateStringLiteral = f.createLiteral(templateString);
            URI contentTypeLiteral = f.createURI(contentType);
            Literal orderLiteral = f.createLiteral(order);
            
            URI nativeFunctionUriLiteral = null;
            
            if(nativeFunctionUri!= null && !nativeFunctionUri.trim().equals(""))
            {
                nativeFunctionUriLiteral = f.createURI(nativeFunctionUri);
            }
            
            URI curationStatusLiteral = null;
            
            if(curationStatus == null)
            {
                curationStatusLiteral = ProjectImpl.projectNotCuratedUri;
            }
            else
            {
                curationStatusLiteral = curationStatus;
            }
            
            con.setAutoCommit(false);
            
            con.add(templateInstanceUri, RDF.TYPE, templateTypeUri, templateInstanceUri);
            con.add(templateInstanceUri, ProjectImpl.projectCurationStatusUri, curationStatusLiteral, templateInstanceUri);
            con.add(templateInstanceUri, templateMatchRegex, matchRegexLiteral, templateInstanceUri);
            con.add(templateInstanceUri, templateIsNativeFunction, isNativeFunctionLiteral, templateInstanceUri);
            
            if(nativeFunctionUri!= null && !nativeFunctionUri.trim().equals(""))
            {
                con.add(templateInstanceUri, templateNativeFunctionUri, nativeFunctionUriLiteral, templateInstanceUri);
            }
            
            con.add(templateInstanceUri, templateTemplateString, templateStringLiteral, templateInstanceUri);
            con.add(templateInstanceUri, templateContentType, contentTypeLiteral, templateInstanceUri);
            con.add(templateInstanceUri, templateOrder, orderLiteral, templateInstanceUri);
            
            if(referencedTemplates != null)
            {
                for(URI nextReferencedTemplate : referencedTemplates)
                {
                    con.add(templateInstanceUri, templateReferencedTemplate, nextReferencedTemplate, templateInstanceUri);
                }
            }
            
            if(unrecognisedStatements != null)
            {
                for(Statement nextUnrecognisedStatement : unrecognisedStatements)
                {
                    con.add(nextUnrecognisedStatement);
                }
            }
            
            // If everything went as planned, we can commit the result
            con.commit();
            
            return true;
        }
        catch (RepositoryException re)
        {
            // Something went wrong during the transaction, so we roll it back
            
            if(con != null)
                con.rollback();
                
            log.error("RepositoryException: "+re.getMessage());
        }
        finally
        {
            if(con != null)
                con.close();
        }
        
        return false;
    }
    
    public static boolean schemaToRdf(Repository myRepository, String keyToUse, int modelVersion) throws OpenRDFException
    {
        RepositoryConnection con = myRepository.getConnection();
        
        ValueFactory f = myRepository.getValueFactory();
        
        try
        {
            URI contextKeyUri = f.createURI(keyToUse);
            URI dcFormatUri = f.createURI(Settings.DC_NAMESPACE+"format");
            
            con.setAutoCommit(false);
            
            con.add(templateTypeUri, RDF.TYPE, OWL.CLASS, contextKeyUri);
            con.add(templateContentTypeSparqlQuery, dcFormatUri, f.createLiteral("application/sparql-query"), contextKeyUri);
            con.add(templateContentTypeSparqlResultsXml, dcFormatUri, f.createLiteral("application/sparql-results+xml"), contextKeyUri);
            con.add(templateContentTypeSparqlResultsJson, dcFormatUri, f.createLiteral("application/sparql-results+json"), contextKeyUri);
            con.add(templateContentTypeRdfXml, dcFormatUri, f.createLiteral("application/rdf+xml"), contextKeyUri);
            con.add(templateContentTypeN3, dcFormatUri, f.createLiteral("text/rdf+n3"), contextKeyUri);
            con.add(templateContentTypePlainText, dcFormatUri, f.createLiteral("text/plain"), contextKeyUri);
            
            con.add(templateContentType, RDF.TYPE, OWL.DATATYPEPROPERTY, contextKeyUri);
            
            con.add(templateReferencedTemplate, RDF.TYPE, OWL.OBJECTPROPERTY, contextKeyUri);
            
            // If everything went as planned, we can commit the result
            con.commit();
            
            return true;
        }
        catch (RepositoryException re)
        {
            // Something went wrong during the transaction, so we roll it back
            
            if(con != null)
                con.rollback();
                
            log.error("RepositoryException: "+re.getMessage());
        }
        finally
        {
            if(con != null)
                con.close();
        }
        
        return false;
    }
    

    public String toHtmlFormBody()
    {
        StringBuilder sb = new StringBuilder();
        
        @SuppressWarnings("unused")
        String prefix = "template_";
        
        return sb.toString();
    }
    

    public String toHtml()
    {
        StringBuilder sb = new StringBuilder();
        
        @SuppressWarnings("unused")
        String prefix = "template_";
        
        return sb.toString();
    }
    
    /**
     * @return the key
     */

    public URI getKey()
    {
        return key;
    }

    /**
     * @param key the key to set
     */

    public void setKey(String nextKey)
    {
        this.setKey(Utilities.createURI(nextKey));
    }

    public void setKey(URI nextKey)
    {
        this.key = nextKey;
    }    
    /**
     * @return the namespace used to represent objects of this type by default
     */

    public String getDefaultNamespace()
    {
        return defaultNamespace;
    }
    
    /**
     * @return the URI used for the rdf Type of these elements
     */

    public String getElementType()
    {
        return templateTypeUri.stringValue();
    }
    
    public String getTemplateString()
    {
        return templateString;
    }
    
    public void setTemplateString(String templateString)
    {
        this.templateString = templateString;
    }
    
    public String getContentType()
    {
        return contentType;
    }
    
    public void setContentType(String contentType)
    {
        this.contentType = contentType;
    }
    
    public String getMatchRegex()
    {
        return matchRegex;
    }
    
    public void setMatchRegex(String matchRegex)
    {
        this.matchRegex = matchRegex;
    }
    
    
    public Collection<URI> getReferencedTemplates()
    {
        return referencedTemplates;
    }
    
    public void setReferencedTemplates(Collection<URI> referencedTemplates)
    {
        this.referencedTemplates = referencedTemplates;
    }
    
    public String getNativeFunctionUri()
    {
        return nativeFunctionUri;
    }
    
    public boolean isNativeFunction()
    {
        return isNativeFunction;
    }
    
    
    
    public void setCurationStatus(URI curationStatus)
    {
        this.curationStatus = curationStatus;
    }
    
    public URI getCurationStatus()
    {
        return curationStatus;
    }
    
    public void addUnrecognisedStatement(Statement unrecognisedStatement)
    {
        unrecognisedStatements.add(unrecognisedStatement);
    }

    public Collection<Statement> getUnrecognisedStatements()
    {
        return unrecognisedStatements;
    }

    public int compareTo(Template otherTemplate)
    {
        @SuppressWarnings("unused")
        final int BEFORE = -1;
        final int EQUAL = 0;
        @SuppressWarnings("unused")
        final int AFTER = 1;
    
        if ( this == otherTemplate ) 
            return EQUAL;

        return this.getKey().stringValue().compareTo(otherTemplate.getKey().stringValue());
    }


}
