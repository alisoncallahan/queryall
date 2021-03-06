/**
 * 
 */
package org.queryall.api.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;
import org.queryall.api.rdfrule.NormalisationRuleSchema;
import org.queryall.api.rdfrule.SpinConstraintRule;
import org.queryall.api.rdfrule.SpinNormalisationRule;
import org.queryall.exception.QueryAllException;
import org.queryall.impl.rdfrule.SpinUtils;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.shared.ReificationStyle;
import com.hp.hpl.jena.vocabulary.OWL2;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * Abstract unit test for SpinConstraintRule API.
 * 
 * @author Peter Ansell p_ansell@yahoo.com
 */
public abstract class AbstractSpinConstraintRuleTest extends AbstractSpinNormalisationRuleTest
{
    /**
     * 
     * @param sparqlPrefixes
     * @param constructGraphPattern
     * @param wherePattern
     * @return
     */
    private static String mergeQuery(final String sparqlPrefixes, final String wherePattern)
    {
        return new StringBuilder(sparqlPrefixes).append(" ASK WHERE { ").append(wherePattern).append(" }").toString();
    }
    
    private URI testMultipleWherePatternsSparqlNormalisationRuleUri;
    private URI testStageEmptyConstructQuerySetSparqlNormalisationRuleUri;
    private String testStartingUriAEOBase;
    private String testFinalUriAEOBase;
    
    private String testStartingUriPOBase;
    
    private String testFinalUriPOBase;
    private Repository testRepository;
    private RepositoryConnection testRepositoryConnection;
    private ValueFactory testValueFactory;
    
    private OntModel testOntologyModel;
    private List<Statement> testSesameStatements;
    
    @Override
    public final Set<URI> getExpectedValidStages()
    {
        final Set<URI> results = new HashSet<URI>();
        
        results.add(NormalisationRuleSchema.getRdfruleStageAfterQueryParsing());
        results.add(NormalisationRuleSchema.getRdfruleStageAfterResultsImport());
        results.add(NormalisationRuleSchema.getRdfruleStageAfterResultsToPool());
        
        return results;
    }
    
    /**
     * Create a new instance of the SpinConstraintRule implementation being tested.
     * 
     * @return a new instance of the implemented SpinConstraintRule
     */
    public abstract SpinConstraintRule getNewTestSpinConstraintRule();
    
    /**
     * Final method, so that implementing test cases must supply a SpinConstraintRule instead,
     * through getNewTestSpinConstraintRule.
     * 
     * @return A NormalisationRule that is also a SpinNormalisationRule
     */
    @Override
    public final SpinNormalisationRule getNewTestSpinNormalisationRule()
    {
        return this.getNewTestSpinConstraintRule();
    }
    
    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        
        this.testRepository = new SailRepository(new MemoryStore());
        this.testRepository.initialize();
        this.testRepositoryConnection = this.testRepository.getConnection();
        this.testValueFactory = new ValueFactoryImpl();
        
        this.testMultipleWherePatternsSparqlNormalisationRuleUri =
                this.testValueFactory.createURI("http://example.org/test/multipleWherePatternsSparqlNormalisationRule");
        this.testStageEmptyConstructQuerySetSparqlNormalisationRuleUri =
                this.testValueFactory
                        .createURI("http://example.org/test/emptyConstructQuerySetSparqlNormalisationRule");
        
        this.testStartingUriAEOBase = "http://purl.obolibrary.org/obo/AEO_";
        this.testFinalUriAEOBase = "http://bio2rdf.org/obo_aeo:";
        this.testStartingUriPOBase = "http://purl.obolibrary.org/obo/PO_";
        this.testFinalUriPOBase = "http://bio2rdf.org/obo_po:";
        
        final Model testModel = ModelFactory.createDefaultModel(ReificationStyle.Minimal);
        
        this.testOntologyModel = ModelFactory.createOntologyModel(SpinUtils.getOntModelSpec(), testModel);
        
        final List<com.hp.hpl.jena.rdf.model.Statement> jenaStatements =
                new ArrayList<com.hp.hpl.jena.rdf.model.Statement>(3);
        
        final com.hp.hpl.jena.rdf.model.Statement testJenaStatement1 =
                ResourceFactory.createStatement(ResourceFactory.createResource("http://my.example.org/test/uri/1"),
                        ResourceFactory.createProperty("http://other.example.org/test/property/a1"),
                        ResourceFactory.createTypedLiteral(42));
        final com.hp.hpl.jena.rdf.model.Statement testJenaStatement2 =
                ResourceFactory.createStatement(ResourceFactory.createResource("http://my.example.org/test/uri/1"),
                        RDF.type, ResourceFactory.createResource("http://my.example.org/test/uri/testType"));
        final com.hp.hpl.jena.rdf.model.Statement testJenaStatement3 =
                ResourceFactory.createStatement(
                        ResourceFactory.createResource("http://my.example.org/test/uri/testType"),
                        OWL2.equivalentClass,
                        ResourceFactory.createResource("http://vocab.org/test/equivalentToRuleType1"));
        
        jenaStatements.add(testJenaStatement1);
        jenaStatements.add(testJenaStatement2);
        jenaStatements.add(testJenaStatement3);
        
        this.testOntologyModel.add(jenaStatements);
        
        this.testRepository = new SailRepository(new MemoryStore());
        this.testRepository.initialize();
        
        final org.openrdf.model.Statement testSesameStatement1 =
                this.testValueFactory.createStatement(
                        this.testValueFactory.createURI("http://my.example.org/test/uri/1"),
                        this.testValueFactory.createURI("http://other.example.org/test/property/a1"),
                        this.testValueFactory.createLiteral(42));
        final org.openrdf.model.Statement testSesameStatement2 =
                this.testValueFactory.createStatement(
                        this.testValueFactory.createURI("http://my.example.org/test/uri/1"),
                        org.openrdf.model.vocabulary.RDF.TYPE,
                        this.testValueFactory.createURI("http://my.example.org/test/uri/testType"));
        final org.openrdf.model.Statement testSesameStatement3 =
                this.testValueFactory.createStatement(
                        this.testValueFactory.createURI("http://my.example.org/test/uri/testType"),
                        OWL.EQUIVALENTCLASS,
                        this.testValueFactory.createURI("http://vocab.org/test/equivalentToRuleType1"));
        
        this.testSesameStatements = new ArrayList<org.openrdf.model.Statement>(3);
        
        this.testSesameStatements.add(testSesameStatement1);
        this.testSesameStatements.add(testSesameStatement2);
        this.testSesameStatements.add(testSesameStatement3);
        
        this.testRepositoryConnection.add(this.testSesameStatements);
        this.testRepositoryConnection.commit();
    }
    
    /**
     * @throws java.lang.Exception
     */
    @Override
    @After
    public void tearDown() throws Exception
    {
        super.tearDown();
        
        if(this.testRepositoryConnection != null)
        {
            try
            {
                this.testRepositoryConnection.close();
            }
            catch(final RepositoryException ex)
            {
                ex.printStackTrace();
            }
            finally
            {
                this.testRepositoryConnection = null;
            }
        }
        
        this.testRepository = null;
        this.testValueFactory = null;
        
        this.testMultipleWherePatternsSparqlNormalisationRuleUri = null;
        
        this.testStartingUriAEOBase = null;
        this.testFinalUriAEOBase = null;
        this.testStartingUriPOBase = null;
        this.testFinalUriPOBase = null;
    }
    
    @Test
    public void testVerifySpinConstraintsByClasspathRef() throws OpenRDFException, QueryAllException
    {
        Assert.assertEquals(3, this.testRepositoryConnection.size());
        
        final SpinConstraintRule spinNormalisationRuleImpl = this.getNewTestSpinConstraintRule();
        spinNormalisationRuleImpl.setKey("http://test.queryall.org/spin/test/localimport/1");
        
        // spinNormalisationRuleImpl.setSpinModuleRegistry(testSpinModuleRegistry1);
        spinNormalisationRuleImpl.addLocalImport("/test/owlrl-all");
        
        final boolean results = spinNormalisationRuleImpl.stageAfterResultsImport(this.testRepository);
        
        Assert.assertTrue(results);
    }
    
    @Test
    public void testVerifySpinConstraintsByURL() throws OpenRDFException, QueryAllException
    {
        Assert.assertEquals(3, this.testRepositoryConnection.size());
        
        final SpinConstraintRule spinNormalisationRuleImpl = this.getNewTestSpinConstraintRule();
        spinNormalisationRuleImpl.setKey("http://test.queryall.org/spin/test/urlimport/1");
        
        // spinNormalisationRuleImpl.setSpinModuleRegistry(testSpinModuleRegistry1);
        spinNormalisationRuleImpl.addUrlImport(this.testValueFactory.createURI("http://topbraid.org/spin/owlrl-all"));
        
        final boolean results = spinNormalisationRuleImpl.stageAfterResultsImport(this.testRepository);
        
        Assert.assertTrue(results);
    }
    
}
