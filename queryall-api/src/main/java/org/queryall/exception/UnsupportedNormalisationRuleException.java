/**
 * 
 */
package org.queryall.exception;

import org.queryall.api.rdfrule.NormalisationRule;

/**
 * An exception that is thrown when an unknown NormalisationRule is encountered
 * 
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class UnsupportedNormalisationRuleException extends QueryAllException
{
    
    /**
     * 
     */
    private static final long serialVersionUID = 9132659393857953163L;
    private NormalisationRule ruleCause;
    
    /**
     * 
     */
    public UnsupportedNormalisationRuleException()
    {
        super();
    }
    
    /**
     * @param message
     */
    public UnsupportedNormalisationRuleException(final String message)
    {
        super(message);
    }
    
    /**
     * @param message
     */
    public UnsupportedNormalisationRuleException(final String message, NormalisationRule nextRule)
    {
        super(message);
        this.setRuleCause(nextRule);
    }

    /**
     * @param message
     * @param cause
     */
    public UnsupportedNormalisationRuleException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
    
    /**
     * @param message
     * @param cause
     */
    public UnsupportedNormalisationRuleException(final String message, NormalisationRule nextRule, final Throwable cause)
    {
        super(message, cause);
        this.setRuleCause(nextRule);
    }
    
    /**
     * @param cause
     */
    public UnsupportedNormalisationRuleException(final Throwable cause)
    {
        super(cause);
    }

    /**
     * @return the ruleCause
     */
    public NormalisationRule getRuleCause()
    {
        return ruleCause;
    }

    /**
     * @param ruleCause the ruleCause to set
     */
    public void setRuleCause(NormalisationRule ruleCause)
    {
        this.ruleCause = ruleCause;
    }
    
}
