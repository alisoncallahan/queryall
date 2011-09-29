package org.queryall.impl.rdfrule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.queryall.api.profile.Profile;
import org.queryall.api.profile.ProfileSchema;
import org.queryall.api.project.ProjectSchema;
import org.queryall.api.rdfrule.NormalisationRule;
import org.queryall.api.rdfrule.NormalisationRuleSchema;
import org.queryall.api.utils.Constants;
import org.queryall.api.utils.QueryAllNamespaces;
import org.queryall.exception.InvalidStageException;
import org.queryall.utils.ProfileUtils;
import org.queryall.utils.RdfUtils;
import org.queryall.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Peter Ansell p_ansell@yahoo.com
 */
public abstract class NormalisationRuleImpl implements NormalisationRule
{
    protected static final Logger log = LoggerFactory.getLogger(NormalisationRuleImpl.class);
    protected static final boolean _TRACE = NormalisationRuleImpl.log.isTraceEnabled();
    protected static final boolean _DEBUG = NormalisationRuleImpl.log.isDebugEnabled();
    protected static final boolean _INFO = NormalisationRuleImpl.log.isInfoEnabled();
    
    protected Collection<Statement> unrecognisedStatements = new HashSet<Statement>();
    
    private URI key;
    
    private String description = "";
    
    private URI curationStatus = ProjectSchema.getProjectNotCuratedUri();
    
    private URI profileIncludeExcludeOrder = ProfileSchema.getProfileIncludeExcludeOrderUndefinedUri();
    
    private Collection<URI> relatedNamespaces = new ArrayList<URI>(2);
    
    protected final Collection<URI> stages = new ArrayList<URI>(3);
    
    protected final Collection<URI> validStages = new ArrayList<URI>(7);
    
    private int order = 100;
    
    private String title = "";
    
    protected NormalisationRuleImpl()
    {
        
    }
    
    // keyToUse is the URI of the next instance that can be found in
    // myRepository
    protected NormalisationRuleImpl(final Collection<Statement> inputStatements, final URI keyToUse,
            final int modelVersion) throws OpenRDFException
    {
        for(final Statement nextStatement : inputStatements)
        {
            // if(NormalisationRuleImpl._DEBUG)
            // {
            // NormalisationRuleImpl.log.debug("NormalisationRule: nextStatement: " +
            // nextStatement.toString());
            // }
            
            if(nextStatement.getPredicate().equals(RDF.TYPE)
                    && nextStatement.getObject().equals(NormalisationRuleSchema.getNormalisationRuleTypeUri()))
            {
                if(NormalisationRuleImpl._TRACE)
                {
                    NormalisationRuleImpl.log.trace("NormalisationRule: found valid type predicate for URI: "
                            + keyToUse);
                }
                
                this.setKey(keyToUse);
            }
            else if(nextStatement.getPredicate().equals(ProjectSchema.getProjectCurationStatusUri()))
            {
                this.setCurationStatus((URI)nextStatement.getObject());
            }
            else if(nextStatement.getPredicate().equals(NormalisationRuleSchema.getRdfruleOrder()))
            {
                this.setOrder(RdfUtils.getIntegerFromValue(nextStatement.getObject()));
            }
            else if(nextStatement.getPredicate().equals(NormalisationRuleSchema.getRdfruleDescription())
                    || nextStatement.getPredicate().equals(RDFS.COMMENT))
            {
                this.setDescription(nextStatement.getObject().stringValue());
            }
            else if(nextStatement.getPredicate().equals(Constants.DC_TITLE))
            {
                this.setTitle(nextStatement.getObject().stringValue());
            }
            else if(nextStatement.getPredicate().equals(NormalisationRuleSchema.getRdfruleHasRelatedNamespace()))
            {
                this.addRelatedNamespaces((URI)nextStatement.getObject());
            }
            else if(nextStatement.getPredicate().equals(NormalisationRuleSchema.getRdfruleStage()))
            {
                try
                {
                    this.addStage((URI)nextStatement.getObject());
                }
                catch(final InvalidStageException ise)
                {
                    NormalisationRuleImpl.log
                            .error("Stage not applicable for this type of normalisation rule nextStatement.getObject()="
                                    + nextStatement.getObject().stringValue()
                                    + " validStages="
                                    + this.validStages.toString()
                                    + " this.getElementTypes()="
                                    + this.getElementTypes()
                                    + " keyToUse=" + keyToUse.stringValue());
                }
            }
            else if(nextStatement.getPredicate().equals(ProfileSchema.getProfileIncludeExcludeOrderUri()))
            {
                this.setProfileIncludeExcludeOrder((URI)nextStatement.getObject());
            }
            else
            {
                this.addUnrecognisedStatement(nextStatement);
            }
        }
        
        // if(NormalisationRuleImpl._DEBUG)
        // {
        // NormalisationRuleImpl.log.debug("NormalisationRuleImpl.fromRdf: would have returned... result="
        // + this.toString());
        // }
    }
    
    /**
     * 
     * @param nextRelatedNamespace
     */
    @Override
    public void addRelatedNamespaces(final URI nextRelatedNamespace)
    {
        this.relatedNamespaces.add(nextRelatedNamespace);
    }
    
    /**
     * @return the Stages
     */
    @Override
    public void addStage(final URI stage) throws InvalidStageException
    {
        if(this.validInStage(stage))
        {
            this.stages.add(stage);
        }
        else
        {
            throw new InvalidStageException("Attempted to add a stage that was not in the list of valid stages");
        }
    }
    
    @Override
    public void addUnrecognisedStatement(final Statement unrecognisedStatement)
    {
        this.unrecognisedStatements.add(unrecognisedStatement);
    }
    
    /**
     * Internal method used by subclasses to add each of their valid stages to the internal list
     * 
     * @return the validStages
     */
    protected void addValidStage(final URI validStage)
    {
        this.validStages.add(validStage);
    }
    
    @Override
    public int compareTo(final NormalisationRule otherRule)
    {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;
        
        if(this == otherRule)
        {
            return EQUAL;
        }
        
        if(this.getOrder() < otherRule.getOrder())
        {
            return BEFORE;
        }
        
        if(this.getOrder() > otherRule.getOrder())
        {
            return AFTER;
        }
        
        return this.getKey().stringValue().compareTo(otherRule.getKey().stringValue());
    }
    
    @Override
    public URI getCurationStatus()
    {
        return this.curationStatus;
    }
    
    /**
     * @return the namespace used to represent objects of this type by default
     */
    @Override
    public QueryAllNamespaces getDefaultNamespace()
    {
        return QueryAllNamespaces.RDFRULE;
    }
    
    @Override
    public String getDescription()
    {
        return this.description;
    }
    
    /**
     * @return the key
     */
    @Override
    public URI getKey()
    {
        return this.key;
    }
    
    @Override
    public int getOrder()
    {
        return this.order;
    }
    
    @Override
    public URI getProfileIncludeExcludeOrder()
    {
        return this.profileIncludeExcludeOrder;
    }
    
    /**
     * @return the relatedNamespaces
     */
    @Override
    public Collection<URI> getRelatedNamespaces()
    {
        return this.relatedNamespaces;
    }
    
    /**
     * @return the Stages
     */
    @Override
    public Collection<URI> getStages()
    {
        return Collections.unmodifiableCollection(this.stages);
    }
    
    @Override
    public String getTitle()
    {
        return this.title;
    }
    
    @Override
    public Collection<Statement> getUnrecognisedStatements()
    {
        return this.unrecognisedStatements;
    }
    
    @Override
    public boolean isUsedWithProfileList(final List<Profile> orderedProfileList, final boolean allowImplicitInclusions,
            final boolean includeNonProfileMatched)
    {
        return ProfileUtils.isUsedWithProfileList(this, orderedProfileList, allowImplicitInclusions,
                includeNonProfileMatched);
    }
    
    @Override
    public Object normaliseByStage(final URI stage, final Object input)
    {
        if(!this.validInStage(stage))
        {
            if(NormalisationRuleImpl._TRACE)
            {
                NormalisationRuleImpl.log
                        .trace("NormalisationRuleImpl.normaliseByStage : found an invalid stage for this type of rule (this may not be an error) stage="
                                + stage);
            }
            
            return input;
        }
        
        if(!this.usedInStage(stage))
        {
            if(NormalisationRuleImpl._DEBUG)
            {
                NormalisationRuleImpl.log
                        .debug("NormalisationRuleImpl.normaliseByStage : found an inapplicable stage for this type of rule key="
                                + this.getKey().stringValue() + " stage=" + stage);
            }
            
            return input;
        }
        
        if(stage.equals(NormalisationRuleSchema.getRdfruleStageQueryVariables()))
        {
            return this.stageQueryVariables(input);
        }
        else if(stage.equals(NormalisationRuleSchema.getRdfruleStageAfterQueryCreation()))
        {
            return this.stageAfterQueryCreation(input);
        }
        else if(stage.equals(NormalisationRuleSchema.getRdfruleStageAfterQueryParsing()))
        {
            return this.stageAfterQueryParsing(input);
        }
        else if(stage.equals(NormalisationRuleSchema.getRdfruleStageBeforeResultsImport()))
        {
            return this.stageBeforeResultsImport(input);
        }
        else if(stage.equals(NormalisationRuleSchema.getRdfruleStageAfterResultsImport()))
        {
            return this.stageAfterResultsImport(input);
        }
        else if(stage.equals(NormalisationRuleSchema.getRdfruleStageAfterResultsToPool()))
        {
            return this.stageAfterResultsToPool(input);
        }
        else if(stage.equals(NormalisationRuleSchema.getRdfruleStageAfterResultsToDocument()))
        {
            return this.stageAfterResultsToDocument(input);
        }
        
        throw new InvalidStageException("Normalisation rule stage unknown : stage=" + stage);
    }
    
    @Override
    public void setCurationStatus(final URI curationStatus)
    {
        this.curationStatus = curationStatus;
    }
    
    public void setDescription(final String description)
    {
        this.description = description;
    }
    
    /**
     * @param key
     *            the key to set
     */
    @Override
    public void setKey(final String nextKey)
    {
        this.setKey(StringUtils.createURI(nextKey));
    }
    
    @Override
    public void setKey(final URI nextKey)
    {
        this.key = nextKey;
    }
    
    @Override
    public void setOrder(final int order)
    {
        this.order = order;
    }
    
    @Override
    public void setProfileIncludeExcludeOrder(final URI profileIncludeExcludeOrder)
    {
        this.profileIncludeExcludeOrder = profileIncludeExcludeOrder;
    }
    
    @Override
    public void setTitle(final String title)
    {
        this.title = title;
    }
    
    @Override
    public boolean toRdf(final Repository myRepository, final URI keyToUse, final int modelVersion)
        throws OpenRDFException
    {
        final RepositoryConnection con = myRepository.getConnection();
        
        final ValueFactory f = Constants.valueFactory;
        
        try
        {
            if(NormalisationRuleImpl._DEBUG)
            {
                NormalisationRuleImpl.log.debug("NormalisationRuleImpl.toRdf: keyToUse=" + keyToUse);
            }
            
            final URI keyUri = this.getKey();
            final Literal titleLiteral = f.createLiteral(this.getTitle());
            final Literal descriptionLiteral = f.createLiteral(this.getDescription());
            final Literal orderLiteral = f.createLiteral(this.getOrder());
            final URI profileIncludeExcludeOrderLiteral = this.getProfileIncludeExcludeOrder();
            
            URI curationStatusLiteral = null;
            
            if((this.curationStatus == null))
            {
                curationStatusLiteral = ProjectSchema.getProjectNotCuratedUri();
            }
            else
            {
                curationStatusLiteral = this.curationStatus;
            }
            
            con.setAutoCommit(false);
            
            if(modelVersion <= 2)
            {
                con.add(keyUri, RDF.TYPE, NormalisationRuleSchema.version2NormalisationRuleTypeUri, keyToUse);
            }
            else
            {
                for(final URI nextElementType : this.getElementTypes())
                {
                    con.add(keyUri, RDF.TYPE, nextElementType, keyToUse);
                }
            }
            
            con.add(keyUri, ProjectSchema.getProjectCurationStatusUri(), curationStatusLiteral, keyToUse);
            
            if(modelVersion == 1)
            {
                con.add(keyUri, NormalisationRuleSchema.getRdfruleDescription(), descriptionLiteral, keyToUse);
            }
            else
            {
                con.add(keyUri, RDFS.COMMENT, descriptionLiteral, keyToUse);
            }
            
            con.add(keyUri, Constants.DC_TITLE, titleLiteral, keyToUse);
            
            con.add(keyUri, NormalisationRuleSchema.getRdfruleOrder(), orderLiteral, keyToUse);
            con.add(keyUri, ProfileSchema.getProfileIncludeExcludeOrderUri(), profileIncludeExcludeOrderLiteral,
                    keyToUse);
            
            if(this.getRelatedNamespaces() != null)
            {
                for(final URI nextRelatedNamespace : this.getRelatedNamespaces())
                {
                    con.add(keyUri, NormalisationRuleSchema.getRdfruleHasRelatedNamespace(), nextRelatedNamespace,
                            keyToUse);
                }
            }
            
            if(this.stages != null)
            {
                for(final URI nextStage : this.stages)
                {
                    con.add(keyUri, NormalisationRuleSchema.getRdfruleStage(), nextStage, keyToUse);
                }
            }
            
            if(this.unrecognisedStatements != null)
            {
                for(final Statement nextUnrecognisedStatement : this.unrecognisedStatements)
                {
                    con.add(nextUnrecognisedStatement, keyToUse);
                }
            }
            
            // If everything went as planned, we can commit the result
            con.commit();
            
            return true;
        }
        catch(final RepositoryException re)
        {
            // Something went wrong during the transaction, so we roll it back
            con.rollback();
            
            NormalisationRuleImpl.log.error("RepositoryException: " + re.getMessage());
        }
        finally
        {
            con.close();
        }
        
        return false;
    }
    
    @Override
    public boolean usedInStage(final org.openrdf.model.URI stage)
    {
        return this.getStages().contains(stage);
    }
    
    @Override
    public boolean validInStage(final org.openrdf.model.URI stage)
    {
        return this.getValidStages().contains(stage);
    }
}
