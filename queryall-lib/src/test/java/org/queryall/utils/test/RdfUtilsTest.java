/**
 * 
 */
package org.queryall.utils.test;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.memory.MemoryStore;
import org.queryall.api.NamespaceEntry;
import org.queryall.enumerations.Constants;
import org.queryall.utils.RdfUtils;

/**
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class RdfUtilsTest
{
    private Repository testRepository;
    private ValueFactory testValueFactory;
    
    private String applicationRdfXml;
    private String textRdfN3;
    private String textPlain;
    private String bogusContentType1;
    private String bogusContentType2;
    
    private String trueString;
    private String falseString;
    private String booleanDataType;
    private URI booleanDataTypeUri;
    private Value trueBooleanTypedLiteral;
    private Value falseBooleanTypedLiteral;
    private Value trueBooleanNativeLiteral;
    private Value falseBooleanNativeLiteral;
    private Value trueBooleanStringLiteral;
    private Value falseBooleanStringLiteral;
    
    private String dateTimeDataType;
    private Date testDate;
    private String testDateString;
    private Value testDateTypedLiteral;
    private Value testDateNativeLiteral;
    private Value testDateStringLiteral;
    
    private Literal testFloatTypedLiteral;
    private Literal testFloatNativeLiteral;
    private Literal testFloatStringLiteral;
    private float testFloat;
    private String floatDataType;
    
    private Value testIntTypedLiteral;
    private Value testIntNativeLiteral;
    private Value testIntStringLiteral;
    private int testInt;
    private String intDataType;
    
    private Value testLongTypedLiteral;
    private Value testLongNativeLiteral;
    private Value testLongStringLiteral;
    private long testLong;
    private String longDataType;
    private RepositoryConnection testRepositoryConnection;
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
        this.testRepository = new SailRepository(new MemoryStore());
        this.testRepository.initialize();
        this.testRepositoryConnection = this.testRepository.getConnection();
        this.testValueFactory = new ValueFactoryImpl();
        
        this.applicationRdfXml = "application/rdf+xml";
        this.textRdfN3 = "text/rdf+n3";
        this.textPlain = "text/plain";
        this.bogusContentType1 = "bogus1";
        this.bogusContentType2 = "bogus2";
        
        this.trueString = "true";
        this.falseString = "false";
        this.booleanDataType = "http://www.w3.org/2001/XMLSchema#boolean";
        this.booleanDataTypeUri = this.testValueFactory.createURI(this.booleanDataType);
        this.trueBooleanTypedLiteral = this.testValueFactory.createLiteral(this.trueString, this.booleanDataTypeUri);
        this.falseBooleanTypedLiteral = this.testValueFactory.createLiteral(this.falseString, this.booleanDataTypeUri);
        this.trueBooleanNativeLiteral = this.testValueFactory.createLiteral(true);
        this.falseBooleanNativeLiteral = this.testValueFactory.createLiteral(false);
        this.trueBooleanStringLiteral = this.testValueFactory.createLiteral(this.trueString);
        this.falseBooleanStringLiteral = this.testValueFactory.createLiteral(this.falseString);
        
        this.dateTimeDataType = "http://www.w3.org/2001/XMLSchema#dateTime";
        final Calendar testDateCalendar = Constants.ISO8601UTC().getCalendar();
        testDateCalendar.set(2010, 01, 02, 03, 04, 05);
        this.testDate = testDateCalendar.getTime();
        final GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(this.testDate.getTime());
        DatatypeFactory df;
        try
        {
            df = DatatypeFactory.newInstance();
        }
        catch(final DatatypeConfigurationException dce)
        {
            throw new IllegalStateException("Exception while obtaining DatatypeFactory instance", dce);
        }
        final XMLGregorianCalendar xmlDate = df.newXMLGregorianCalendar(gc);
        this.testDateString = Constants.ISO8601UTC().format(this.testDate);
        this.testDateTypedLiteral = this.testValueFactory.createLiteral(this.testDateString, this.dateTimeDataType);
        this.testDateNativeLiteral = this.testValueFactory.createLiteral(xmlDate);
        this.testDateStringLiteral = this.testValueFactory.createLiteral(this.testDateString);
        
        this.floatDataType = "http://www.w3.org/2001/XMLSchema#float";
        this.testFloat = 0.278134f;
        this.testFloatTypedLiteral =
                this.testValueFactory.createLiteral(Float.toString(this.testFloat), this.floatDataType);
        this.testFloatNativeLiteral = this.testValueFactory.createLiteral(this.testFloat);
        this.testFloatStringLiteral = this.testValueFactory.createLiteral(Float.toString(this.testFloat));
        
        this.intDataType = "http://www.w3.org/2001/XMLSchema#int";
        this.testInt = 278134;
        this.testIntTypedLiteral =
                this.testValueFactory.createLiteral(Integer.toString(this.testInt), this.intDataType);
        this.testIntNativeLiteral = this.testValueFactory.createLiteral(this.testInt);
        this.testIntStringLiteral = this.testValueFactory.createLiteral(Integer.toString(this.testInt));
        
        this.longDataType = "http://www.w3.org/2001/XMLSchema#long";
        this.testLong = 278134965145L;
        this.testLongTypedLiteral =
                this.testValueFactory.createLiteral(Long.toString(this.testLong), this.longDataType);
        this.testLongNativeLiteral = this.testValueFactory.createLiteral(this.testLong);
        this.testLongStringLiteral = this.testValueFactory.createLiteral(Long.toString(this.testLong));
    }
    
    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
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
        
        this.applicationRdfXml = null;
        this.textRdfN3 = null;
        this.textPlain = null;
        this.bogusContentType1 = null;
        this.bogusContentType2 = null;
        
        this.trueString = null;
        this.falseString = null;
        this.booleanDataType = null;
        this.booleanDataTypeUri = null;
        
        this.trueBooleanTypedLiteral = null;
        this.falseBooleanTypedLiteral = null;
        
        this.trueBooleanNativeLiteral = null;
        this.falseBooleanNativeLiteral = null;
        
        this.trueBooleanStringLiteral = null;
        this.falseBooleanStringLiteral = null;
    }
    
    /**
     * Test method for
     * {@link org.queryall.utils.RdfUtils#findBestContentType(java.lang.String, java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public void testFindBestContentTypeAgainst1BogusType()
    {
        Assert.assertEquals(this.applicationRdfXml,
                RdfUtils.findBestContentType(this.applicationRdfXml, this.bogusContentType1, this.textRdfN3));
        Assert.assertEquals(this.applicationRdfXml,
                RdfUtils.findBestContentType(this.bogusContentType1, this.applicationRdfXml, this.textRdfN3));
        Assert.assertEquals(this.textRdfN3,
                RdfUtils.findBestContentType(this.bogusContentType1, this.textRdfN3, this.applicationRdfXml));
        
        Assert.assertEquals(this.textRdfN3,
                RdfUtils.findBestContentType(this.textRdfN3, this.bogusContentType1, this.applicationRdfXml));
        Assert.assertEquals(this.textRdfN3,
                RdfUtils.findBestContentType(this.bogusContentType1, this.textRdfN3, this.applicationRdfXml));
        Assert.assertEquals(this.applicationRdfXml,
                RdfUtils.findBestContentType(this.bogusContentType1, this.applicationRdfXml, this.textRdfN3));
        
        Assert.assertEquals(this.textPlain,
                RdfUtils.findBestContentType(this.textPlain, this.bogusContentType1, this.applicationRdfXml));
        Assert.assertEquals(this.textPlain,
                RdfUtils.findBestContentType(this.bogusContentType1, this.textPlain, this.applicationRdfXml));
        Assert.assertEquals(this.applicationRdfXml,
                RdfUtils.findBestContentType(this.bogusContentType1, this.applicationRdfXml, this.textPlain));
        
    }
    
    /**
     * Test method for
     * {@link org.queryall.utils.RdfUtils#findBestContentType(java.lang.String, java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public void testFindBestContentTypeAgainst2BogusTypes()
    {
        Assert.assertEquals(this.applicationRdfXml,
                RdfUtils.findBestContentType(this.applicationRdfXml, this.bogusContentType1, this.bogusContentType2));
        Assert.assertEquals(this.applicationRdfXml,
                RdfUtils.findBestContentType(this.bogusContentType1, this.applicationRdfXml, this.bogusContentType2));
        Assert.assertEquals(this.applicationRdfXml,
                RdfUtils.findBestContentType(this.bogusContentType1, this.bogusContentType2, this.applicationRdfXml));
        
        Assert.assertEquals(this.textRdfN3,
                RdfUtils.findBestContentType(this.textRdfN3, this.bogusContentType1, this.bogusContentType2));
        Assert.assertEquals(this.textRdfN3,
                RdfUtils.findBestContentType(this.bogusContentType1, this.textRdfN3, this.bogusContentType2));
        Assert.assertEquals(this.textRdfN3,
                RdfUtils.findBestContentType(this.bogusContentType1, this.bogusContentType2, this.textRdfN3));
        
        Assert.assertEquals(this.textPlain,
                RdfUtils.findBestContentType(this.textPlain, this.bogusContentType1, this.bogusContentType2));
        Assert.assertEquals(this.textPlain,
                RdfUtils.findBestContentType(this.bogusContentType1, this.textPlain, this.bogusContentType2));
        Assert.assertEquals(this.textPlain,
                RdfUtils.findBestContentType(this.bogusContentType1, this.bogusContentType2, this.textPlain));
        
    }
    
    /**
     * Test method for
     * {@link org.queryall.utils.RdfUtils#findBestContentType(java.lang.String, java.lang.String, java.lang.String)}
     * .
     */
    @Test
    public void testFindBestContentTypeWith3RealTypes()
    {
        Assert.assertEquals(this.applicationRdfXml,
                RdfUtils.findBestContentType(this.applicationRdfXml, this.textPlain, this.textRdfN3));
        Assert.assertEquals(this.textPlain,
                RdfUtils.findBestContentType(this.textPlain, this.applicationRdfXml, this.textRdfN3));
        Assert.assertEquals(this.textPlain,
                RdfUtils.findBestContentType(this.textPlain, this.textRdfN3, this.applicationRdfXml));
        
        Assert.assertEquals(this.textRdfN3,
                RdfUtils.findBestContentType(this.textRdfN3, this.textPlain, this.applicationRdfXml));
        Assert.assertEquals(this.textPlain,
                RdfUtils.findBestContentType(this.textPlain, this.textRdfN3, this.applicationRdfXml));
        Assert.assertEquals(this.textPlain,
                RdfUtils.findBestContentType(this.textPlain, this.applicationRdfXml, this.textRdfN3));
        
        Assert.assertEquals(this.textPlain,
                RdfUtils.findBestContentType(this.textPlain, this.textRdfN3, this.applicationRdfXml));
        Assert.assertEquals(this.textRdfN3,
                RdfUtils.findBestContentType(this.textRdfN3, this.textPlain, this.applicationRdfXml));
        Assert.assertEquals(this.textRdfN3,
                RdfUtils.findBestContentType(this.textRdfN3, this.applicationRdfXml, this.textPlain));
        
    }
    
    /**
     * Test method for
     * {@link org.queryall.utils.RdfUtils#getDateTimeFromValue(org.openrdf.model.Value)}.
     * 
     * 
     * TODO: make this work
     */
    @Test
    @Ignore
    public void testGetDateTimeFromValue()
    {
        try
        {
            Assert.assertEquals(this.testDate.getTime(), RdfUtils.getDateTimeFromValue(this.testDateTypedLiteral)
                    .getTime());
            Assert.assertEquals(this.testDate.getTime(), RdfUtils.getDateTimeFromValue(this.testDateNativeLiteral)
                    .getTime());
            Assert.assertEquals(this.testDate.getTime(), RdfUtils.getDateTimeFromValue(this.testDateStringLiteral)
                    .getTime());
        }
        catch(final ParseException e)
        {
            Assert.fail("Found unexpected ParseException e=" + e.getMessage());
        }
    }
    
    /**
     * Test method for
     * {@link org.queryall.utils.RdfUtils#getFloatFromValue(org.openrdf.model.Value)}.
     */
    @Test
    public void testGetFloatFromValue()
    {
        Assert.assertEquals(this.testFloat, RdfUtils.getFloatFromValue(this.testFloatTypedLiteral), 0.0000001);
        Assert.assertEquals(this.testFloat, RdfUtils.getFloatFromValue(this.testFloatNativeLiteral), 0.0000001);
        Assert.assertEquals(this.testFloat, RdfUtils.getFloatFromValue(this.testFloatStringLiteral), 0.0000001);
    }
    
    /**
     * Test method for
     * {@link org.queryall.utils.RdfUtils#getIntegerFromValue(org.openrdf.model.Value)}.
     */
    @Test
    public void testGetIntegerFromValue()
    {
        Assert.assertEquals(this.testInt, RdfUtils.getIntegerFromValue(this.testIntTypedLiteral));
        Assert.assertEquals(this.testInt, RdfUtils.getIntegerFromValue(this.testIntNativeLiteral));
        Assert.assertEquals(this.testInt, RdfUtils.getIntegerFromValue(this.testIntStringLiteral));
    }
    
    /**
     * Test method for {@link org.queryall.utils.RdfUtils#getLongFromValue(org.openrdf.model.Value)}
     * .
     */
    @Test
    public void testGetLongFromValue()
    {
        Assert.assertEquals(this.testLong, RdfUtils.getLongFromValue(this.testLongTypedLiteral));
        Assert.assertEquals(this.testLong, RdfUtils.getLongFromValue(this.testLongNativeLiteral));
        Assert.assertEquals(this.testLong, RdfUtils.getLongFromValue(this.testLongStringLiteral));
    }
    
    /**
     * Test method for
     * {@link org.queryall.utils.RdfUtils#getNamespaceEntries(org.openrdf.repository.Repository)}.
     */
    @Test
    public void testGetNamespaceEntries()
    {
        final InputStream nextInputStream = this.getClass().getResourceAsStream("/testconfigs/namespaceentry-1.n3");
        
        try
        {
            Assert.assertNotNull("Could not find test file", nextInputStream);
            
            this.testRepositoryConnection.add(nextInputStream, "", RDFFormat.N3);
            this.testRepositoryConnection.commit();
            
            final Map<URI, NamespaceEntry> results = RdfUtils.getNamespaceEntries(this.testRepository);
            
            Assert.assertEquals(1, results.size());
            
            for(final URI nextNamespaceEntryUri : results.keySet())
            {
                Assert.assertEquals("Results did not contain correct namespace entry URI",
                        this.testValueFactory.createURI("http://example.org/ns:abc"), nextNamespaceEntryUri);
            }
        }
        catch(final RDFParseException ex)
        {
            Assert.fail("Found unexpected RDFParseException : " + ex.getMessage());
        }
        catch(final RepositoryException ex)
        {
            Assert.fail("Found unexpected RepositoryException : " + ex.getMessage());
        }
        catch(final IOException ex)
        {
            Assert.fail("Found unexpected IOException : " + ex.getMessage());
        }
    }
    
    /**
     * Test method for
     * {@link org.queryall.utils.RdfUtils#getBooleanFromValue(org.openrdf.model.Value)}.
     */
    @Test
    public void testGetNativeBooleanFromValue()
    {
        Assert.assertTrue(RdfUtils.getBooleanFromValue(this.trueBooleanNativeLiteral));
        Assert.assertFalse(RdfUtils.getBooleanFromValue(this.falseBooleanNativeLiteral));
    }
    
    /**
     * Test method for
     * {@link org.queryall.utils.RdfUtils#getNormalisationRules(org.openrdf.repository.Repository)}.
     */
    @Test
    @Ignore
    public void testGetNormalisationRules()
    {
        Assert.fail("Not yet implemented");
    }
    
    /**
     * Test method for
     * {@link org.queryall.utils.RdfUtils#getProfiles(org.openrdf.repository.Repository)}.
     */
    @Test
    @Ignore
    public void testGetProfiles()
    {
        Assert.fail("Not yet implemented");
    }
    
    /**
     * Test method for
     * {@link org.queryall.utils.RdfUtils#getProviders(org.openrdf.repository.Repository)}.
     */
    @Test
    @Ignore
    public void testGetProviders()
    {
        Assert.fail("Not yet implemented");
    }
    
    /**
     * Test method for
     * {@link org.queryall.utils.RdfUtils#getQueryTypes(org.openrdf.repository.Repository)}.
     */
    @Test
    @Ignore
    public void testGetQueryTypes()
    {
        Assert.fail("Not yet implemented");
    }
    
    /**
     * Test method for
     * {@link org.queryall.utils.RdfUtils#getRuleTests(org.openrdf.repository.Repository)}.
     */
    @Test
    @Ignore
    public void testGetRuleTests()
    {
        Assert.fail("Not yet implemented");
    }
    
    /**
     * Test method for
     * {@link org.queryall.utils.RdfUtils#getBooleanFromValue(org.openrdf.model.Value)}.
     */
    @Test
    public void testGetStringBooleanFromValue()
    {
        Assert.assertTrue(RdfUtils.getBooleanFromValue(this.trueBooleanStringLiteral));
        Assert.assertFalse(RdfUtils.getBooleanFromValue(this.falseBooleanStringLiteral));
    }
    
    /**
     * Test method for
     * {@link org.queryall.utils.RdfUtils#getBooleanFromValue(org.openrdf.model.Value)}.
     */
    @Test
    public void testGetTypedBooleanFromValue()
    {
        Assert.assertTrue(RdfUtils.getBooleanFromValue(this.trueBooleanTypedLiteral));
        Assert.assertFalse(RdfUtils.getBooleanFromValue(this.falseBooleanTypedLiteral));
    }
    
    /**
     * Test method for
     * {@link org.queryall.utils.RdfUtils#getUTF8StringValueFromSesameValue(org.openrdf.model.Value)}
     * .
     */
    @Test
    @Ignore
    public void testGetUTF8StringValueFromSesameValue()
    {
        Assert.fail("Not yet implemented");
    }
    
    /**
     * Test method for {@link org.queryall.utils.RdfUtils#getWriterFormat(java.lang.String)}.
     */
    @Test
    @Ignore
    public void testGetWriterFormat()
    {
        Assert.fail("Not yet implemented");
    }
    
}