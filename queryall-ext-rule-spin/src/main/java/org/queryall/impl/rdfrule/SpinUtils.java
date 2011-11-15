package org.queryall.impl.rdfrule;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;

import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.sail.memory.MemoryStore;
import org.queryall.exception.QueryAllException;
import org.queryall.utils.RdfUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.spin.arq.ARQ2SPIN;
import org.topbraid.spin.arq.ARQFactory;

import com.hp.hpl.jena.graph.GraphMaker;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.impl.ModelMakerImpl;
import com.hp.hpl.jena.shared.ReificationStyle;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.FileUtils;
import com.hp.hpl.jena.util.LocationMapper;
import com.hp.hpl.jena.util.LocatorFile;

/**
 * @author Peter Ansell p_ansell@yahoo.com
 * 
 * TODO: Change this from a static/Singleton into a more customisable structure using a configurable location mapping file
 */
public class SpinUtils
{
    private static final Logger log = LoggerFactory.getLogger(SpinUtils.class);
    @SuppressWarnings("unused")
    private static final boolean _TRACE = SpinUtils.log.isTraceEnabled();
    @SuppressWarnings("unused")
    private static final boolean _DEBUG = SpinUtils.log.isDebugEnabled();
    @SuppressWarnings("unused")
    private static final boolean _INFO = SpinUtils.log.isInfoEnabled();
    private static LocationMapper lMap;
    private static FileManager fileManager;
    
    static
    {
        Model mappingConfig = ModelFactory.createDefaultModel(ReificationStyle.Minimal);
        
        final InputStream stream = SpinUtils.class.getResourceAsStream("/queryall-jena-location-mapping.n3");
        
        mappingConfig.read(stream, "http://temp.base.uri.fake/", "N3");
        
        log.info("mappingConfig.size()="+mappingConfig.size());
        
        lMap = new LocationMapper();
        
        lMap.processConfig(mappingConfig);
        
        // FIXME: Make SPIN/ARQ not require this line
        LocationMapper.setGlobalLocationMapper(lMap);
        
        fileManager = new FileManager(lMap);
        
//        fileManager.addLocatorFile();
//        fileManager.addLocatorClassLoader(fileManager.getClass().getClassLoader());
        fileManager.addLocator(new JenaLocatorClass(SpinUtils.class));
        
        InputStream testStream = SpinUtils.class.getClassLoader().getResourceAsStream("/test/owlrl-all");
        
        log.info("testStream="+testStream);

        testStream = fileManager.getClass().getClassLoader().getResourceAsStream("/test/owlrl-all");        
        
        log.info("testStream="+testStream);

        testStream = SpinUtils.class.getResourceAsStream("/test/owlrl-all");        

        log.info("testStream="+testStream);

        // FIXME: Make SPIN/ARQ not require this line
        FileManager.setGlobalFileManager(fileManager);
    }
    
    /**
     * Takes the RDF statements from a Jena Model and adds them to the given contexts in a Sesame
     * repository
     * 
     * @param inputModel
     * @param outputRepository
     *            If outputRepository is null, a new in-memory repository is created
     * @param contexts
     * @return
     * @throws QueryAllException
     */
    public static Repository addJenaModelToSesameRepository(final Model inputModel, Repository outputRepository,
            final org.openrdf.model.Resource... contexts) throws QueryAllException
    {
        if(outputRepository == null)
        {
            outputRepository = new SailRepository(new MemoryStore());
            try
            {
                outputRepository.initialize();
            }
            catch(final RepositoryException e)
            {
                SpinUtils.log.error("Found unexpected exception initialising in memory repository", e);
                throw new QueryAllException("Found unexpected exception initialising in memory repository", e);
            }
        }
        
        final ByteArrayOutputStream internalOutputStream = new ByteArrayOutputStream();
        
        // write out the triples from the model into the output stream
        inputModel.write(internalOutputStream);
        
        // use the resulting byte[] as input to an InputStream
        final InputStream bufferedInputStream =
                new BufferedInputStream(new ByteArrayInputStream(internalOutputStream.toByteArray()));
        
        RepositoryConnection connection = null;
        
        try
        {
            connection = outputRepository.getConnection();
            
            connection.add(bufferedInputStream, "http://spin.example.org/", RDFFormat.RDFXML, contexts);
            
            connection.commit();
        }
        catch(final Exception e)
        {
            SpinUtils.log.error("Found exception while attempting to add data to OpenRDF repository", e);
            
            try
            {
                if(connection != null)
                {
                    connection.rollback();
                }
            }
            catch(final RepositoryException e1)
            {
                SpinUtils.log.error(
                        "Found exception while attempting to rollback connection due to previous exception", e1);
            }
            
            throw new QueryAllException("Found exception while attempting to add data to OpenRDF repository", e);
        }
        finally
        {
            try
            {
                if(connection != null)
                {
                    connection.close();
                }
            }
            catch(final RepositoryException e)
            {
                SpinUtils.log.error("Found exception while attempting to close connection in finally block", e);
            }
        }
        
        return outputRepository;
    }
    
    public static OntModel addSesameRepositoryToJenaModel(final Repository inputRepository, Model outputModel,
            final String baseURI, final org.openrdf.model.Resource... contexts)
    {
        // if the outputModel was not defined, then create a new in memory model to contain the results
        if(outputModel == null)
        {
            outputModel = ModelFactory.createDefaultModel(ReificationStyle.Minimal);
        }
        
        // Model baseModel = ModelFactory.createDefaultModel(ReificationStyle.Minimal);
        
        final ByteArrayOutputStream internalOutputStream = new ByteArrayOutputStream();
        
        // write out the triples from the model into the output stream
        RdfUtils.toOutputStream(inputRepository, internalOutputStream, RDFFormat.RDFXML, contexts);
        
        // use the resulting byte[] as input to an InputStream
        final InputStream bufferedInputStream =
                new BufferedInputStream(new ByteArrayInputStream(internalOutputStream.toByteArray()));
        
        outputModel.read(bufferedInputStream, baseURI);
        
        // TODO: Should be creating this model earlier and then adding the triples to it instead of
        // to the outputModel?
        return ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, outputModel);
    }
    
    public static String getTurtleSPINQueryFromSPARQL(final String query)
    {
        final Model model = ModelFactory.createDefaultModel(ReificationStyle.Minimal);
        model.setNsPrefix("rdf", com.hp.hpl.jena.vocabulary.RDF.getURI());
        
        final Query arqQuery = ARQFactory.get().createQuery(model, query);
        // We don't need the results of this operation, but we do need its side-effects on "model"
        new ARQ2SPIN(model).createQuery(arqQuery, null);
        
        final StringWriter output = new StringWriter();
        
        System.out.println("SPIN query in Turtle:");
        model.write(output, FileUtils.langTurtle);
        
        final String turtleString = output.toString();
        
        return turtleString;
    }
    
    /**
     * 
     * 
     * @param classpathRef A reference on the classpath
     * @return
     */
    public static OntModel loadModelFromClasspath(String classpathRef)
    {
        SpinUtils.log.info("loading model from classpathRef=" + classpathRef);
        
        final Model baseModel = ModelFactory.createDefaultModel(ReificationStyle.Minimal);
        
        if(classpathRef.startsWith("file:"))
        {
            log.debug("classpathRef did not start with file:, prepending file: to it");
            classpathRef = classpathRef.substring("file:".length());
        }
        
        final InputStream stream = SpinUtils.class.getResourceAsStream(classpathRef);
        
        baseModel.read(stream, "http://temp.base.uri.fake/");
        
        // FIXME: Cannot determine way to use the fileManager to load from a classpath reference or attach the fileManager or locationMapper to use for other resolutions
//        Model fileManagerModel = fileManager.loadModel(classpathRef);
        
//        ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, new ModelMakerImpl(new GraphMaker()), base)
        
//        log.info("fileManagerModel.isIsomorphicWith(baseModel)="+fileManagerModel.isIsomorphicWith(baseModel));
        
        // TODO: make the OntModelSpec here configurable
        return ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, baseModel);
    }
    
    public static OntModel loadModelFromUrl(final String url)
    {
        SpinUtils.log.info("loading model from url=" + url);
        
        Model baseModel = ModelFactory.createDefaultModel(ReificationStyle.Minimal);
        baseModel.add(fileManager.loadModel(url));
        
        // TODO: make the OntModelSpec here configurable
        return ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, baseModel);
    }
    
}
