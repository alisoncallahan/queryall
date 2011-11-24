package org.queryall.impl.rdfrule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.queryall.api.profile.Profile;
import org.queryall.api.profile.ProfileSchema;
import org.queryall.api.rdfrule.NormalisationRule;
import org.queryall.api.rdfrule.NormalisationRuleSchema;
import org.queryall.api.rdfrule.TransformingRuleSchema;
import org.queryall.api.rdfrule.ValidatingRuleSchema;
import org.queryall.api.utils.Constants;
import org.queryall.api.utils.QueryAllNamespaces;
import org.queryall.exception.InvalidStageException;
import org.queryall.impl.base.BaseQueryAllImpl;
import org.queryall.utils.ProfileUtils;
import org.queryall.utils.RdfUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Peter Ansell p_ansell@yahoo.com
 */
public abstract class BaseRuleImpl extends BaseQueryAllImpl implements NormalisationRule
{
    private static final Logger log = LoggerFactory.getLogger(BaseRuleImpl.class);
    private static final boolean _TRACE = BaseRuleImpl.log.isTraceEnabled();
    private static final boolean _DEBUG = BaseRuleImpl.log.isDebugEnabled();
    private static final boolean _INFO = BaseRuleImpl.log.isInfoEnabled();
    
    private URI profileIncludeExcludeOrder = ProfileSchema.getProfileIncludeExcludeOrderUndefinedUri();
    
    private Collection<URI> relatedNamespaces = new ArrayList<URI>(2);
    
    protected final Set<URI> stages = new HashSet<URI>(4);
    
    protected final Set<URI> validStages = new HashSet<URI>(7);
    
    private int order = 100;
    
    protected BaseRuleImpl()
    {
        
    }
    
    // keyToUse is the URI of the next instance that can be found in
    // myRepository
    protected BaseRuleImpl(final Collection<Statement> inputStatements, final URI keyToUse, final int modelVersion)
        throws OpenRDFException
    {
        super(inputStatements, keyToUse, modelVersion);
        
        final Collection<Statement> currentUnrecognisedStatements = this.resetUnrecognisedStatements();
        
        for(final Statement nextStatement : currentUnrecognisedStatements)
        {
            // if(NormalisationRuleImpl._DEBUG)
            // {
            // NormalisationRuleImpl.log.debug("NormalisationRule: nextStatement: " +
            // nextStatement.toString());
            // }
            
            if(nextStatement.getPredicate().equals(RDF.TYPE)
                    && nextStatement.getObject().equals(NormalisationRuleSchema.getNormalisationRuleTypeUri()))
            {
                if(BaseRuleImpl._TRACE)
                {
                    BaseRuleImpl.log.trace("NormalisationRule: found valid type predicate for URI: " + keyToUse);
                }
                
                this.setKey(keyToUse);
            }
            else if(nextStatement.getPredicate().equals(NormalisationRuleSchema.getRdfruleOrder()))
            {
                this.setOrder(RdfUtils.getIntegerFromValue(nextStatement.getObject()));
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
                    BaseRuleImpl.log
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
            throw new InvalidStageException("Attempted to add a stage that was not in the list of valid stages", this,
                    stage);
        }
    }
    
    /**
     * Internal method used by subclasses to add each of their valid stages to the internal list
     * 
     * @return the validStages
     * @throws InvalidStageException
     */
    protected void addValidStage(final URI validStage) throws InvalidStageException
    {
        if(validStage == null)
        {
            throw new IllegalArgumentException("Valid stage was null");
        }
        
        if(!this.validStages.contains(validStage))
        {
            if(NormalisationRuleSchema.getAllStages().contains(validStage))
            {
                this.validStages.add(validStage);
            }
            else
            {
                throw new InvalidStageException(
                        "Could not assign a stage as valid as it was not recognised as a stage by this implementation",
                        this, validStage);
            }
        }
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
    
    /**
     * @return the namespace used to represent objects of this type by default
     */
    @Override
    public QueryAllNamespaces getDefaultNamespace()
    {
        return QueryAllNamespaces.RDFRULE;
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
    public final Set<URI> getStages()
    {
        return Collections.unmodifiableSet(this.stages);
    }
    
    @Override
    public boolean isUsedWithProfileList(final List<Profile> orderedProfileList, final boolean allowImplicitInclusions,
            final boolean includeNonProfileMatched)
    {
        return ProfileUtils.isUsedWithProfileList(this, orderedProfileList, allowImplicitInclusions,
                includeNonProfileMatched);
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
    public boolean toRdf(final Repository myRepository, final int modelVersion, final URI... contextKey)
        throws OpenRDFException
    {
        super.toRdf(myRepository, modelVersion, contextKey);
        
        final RepositoryConnection con = myRepository.getConnection();
        
        final ValueFactory f = Constants.valueFactory;
        
        try
        {
            if(BaseRuleImpl._DEBUG)
            {
                BaseRuleImpl.log.debug("NormalisationRuleImpl.toRdf: keyToUse=" + contextKey);
            }
            
            final URI keyUri = this.getKey();
            final Literal orderLiteral = f.createLiteral(this.getOrder());
            final URI profileIncludeExcludeOrderLiteral = this.getProfileIncludeExcludeOrder();
            
            con.setAutoCommit(false);
            
            if(modelVersion <= 2)
            {
                con.add(keyUri, RDF.TYPE, NormalisationRuleSchema.version2NormalisationRuleTypeUri, contextKey);
            }
            else
            {
                for(final URI nextElementType : this.getElementTypes())
                {
                    // hide the TransformationRule and ValidationRule types from clients below
                    // version 5 so that it will not prevent them parsing the rules,
                    // as the types here need to be a subset of the types in the clients
                    // implementation, and these types would break all version 1-4 systems otherwise
                    if(modelVersion < 5)
                    {
                        if(!nextElementType.equals(TransformingRuleSchema.getTransformingRuleTypeUri())
                                && !nextElementType.equals(ValidatingRuleSchema.getValidatingRuleTypeUri()))
                        {
                            con.add(keyUri, RDF.TYPE, nextElementType, contextKey);
                        }
                    }
                    else
                    {
                        con.add(keyUri, RDF.TYPE, nextElementType, contextKey);
                    }
                }
            }
            
            con.add(keyUri, NormalisationRuleSchema.getRdfruleOrder(), orderLiteral, contextKey);
            con.add(keyUri, ProfileSchema.getProfileIncludeExcludeOrderUri(), profileIncludeExcludeOrderLiteral,
                    contextKey);
            
            if(this.getRelatedNamespaces() != null)
            {
                for(final URI nextRelatedNamespace : this.getRelatedNamespaces())
                {
                    con.add(keyUri, NormalisationRuleSchema.getRdfruleHasRelatedNamespace(), nextRelatedNamespace,
                            contextKey);
                }
            }
            
            if(this.stages != null)
            {
                for(final URI nextStage : this.stages)
                {
                    con.add(keyUri, NormalisationRuleSchema.getRdfruleStage(), nextStage, contextKey);
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
            
            BaseRuleImpl.log.error("RepositoryException: " + re.getMessage());
        }
        finally
        {
            con.close();
        }
        
        return false;
    }
    
    @Override
    public final boolean usedInStage(final org.openrdf.model.URI stage)
    {
        return this.getStages().contains(stage);
    }
    
    @Override
    public final boolean validInStage(final org.openrdf.model.URI stage)
    {
        return this.getValidStages().contains(stage);
    }
}
