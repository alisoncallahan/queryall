package org.queryall.api.ruletest;

import java.util.Collection;

import org.openrdf.model.URI;
import org.queryall.api.base.BaseQueryAllInterface;

/**
 * @author Peter Ansell p_ansell@yahoo.com
 */
public interface RuleTest extends BaseQueryAllInterface, Comparable<RuleTest>
{
    Collection<URI> getRuleUris();
    
    Collection<URI> getStages();
    
    void addRuleUri(URI rdfRuleUri);
    
    void addStage(URI stage);
    
}
