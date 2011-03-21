
package org.queryall.impl;

import java.util.HashSet;
import java.util.Collection;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import org.queryall.helpers.*;

/**
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class RegexNormalisationRule extends NormalisationRuleImpl
{
    private static final Logger log = Logger
            .getLogger(RegexNormalisationRule.class.getName());
    private static final boolean _TRACE = RegexNormalisationRule.log
            .isTraceEnabled();
    private static final boolean _DEBUG = RegexNormalisationRule.log
            .isDebugEnabled();
    @SuppressWarnings("unused")
    private static final boolean _INFO = RegexNormalisationRule.log
            .isInfoEnabled();
    
    private String inputMatchRegex = "";
    private String inputReplaceRegex = "";
    private String outputMatchRegex = "";
    private String outputReplaceRegex = "";

    private static URI regexruleTypeUri;
    private static URI regexruleInputMatchRegex;
    private static URI regexruleInputReplaceRegex;
    private static URI regexruleOutputMatchRegex;
    private static URI regexruleOutputReplaceRegex;
    
    static
    {
        final ValueFactory f = Constants.valueFactory;
        
        RegexNormalisationRule.setRegexRuleTypeUri(f.createURI(RegexNormalisationRule.rdfruleNamespace, "RegexNormalisationRule"));
        RegexNormalisationRule.setRegexRuleInputMatchRegex(f
                .createURI(RegexNormalisationRule.rdfruleNamespace, "inputMatchRegex"));
        RegexNormalisationRule.setRegexRuleInputReplaceRegex(f
                .createURI(RegexNormalisationRule.rdfruleNamespace, "inputReplaceRegex"));
        RegexNormalisationRule.setRegexRuleOutputMatchRegex(f
                .createURI(RegexNormalisationRule.rdfruleNamespace, "outputMatchRegex"));
        RegexNormalisationRule.setRegexRuleOutputReplaceRegex(f
                .createURI(RegexNormalisationRule.rdfruleNamespace, "outputReplaceRegex"));
    }
    
    // keyToUse is the URI of the next instance that can be found in
    // myRepository
    public RegexNormalisationRule(Collection<Statement> inputStatements, URI keyToUse, int modelVersion)
            throws OpenRDFException
    {
        super(inputStatements, keyToUse, modelVersion);
        
        @SuppressWarnings("unused")
        boolean isValid = false;

        for(final Statement nextStatement : inputStatements)
        {
            if(RegexNormalisationRule._TRACE)
            {
                RegexNormalisationRule.log
                        .trace("RegexNormalisationRule: nextStatement: "
                                + nextStatement.toString());
            }
            
            if(nextStatement.getPredicate().equals(RDF.TYPE)
                    && ( nextStatement.getObject().equals(
                            RegexNormalisationRule.getRegexRuleTypeUri()) || nextStatement.getObject().equals(
                            RegexNormalisationRule.getRegexRuleTypeUri()) )
              )
            {
                if(RegexNormalisationRule._TRACE)
                {
                    RegexNormalisationRule.log
                            .trace("RegexNormalisationRule: found valid type predicate for URI: "
                                    + keyToUse);
                }
                
                isValid = true;
                this.setKey(keyToUse);
            }
            else if(nextStatement.getPredicate().equals(
                    RegexNormalisationRule.getRegexRuleInputMatchRegex()))
            {
                this.setInputMatchRegex(nextStatement.getObject()
                        .stringValue());
            }
            else if(nextStatement.getPredicate().equals(
                    RegexNormalisationRule.getRegexRuleInputReplaceRegex()))
            {
                this.setInputReplaceRegex(nextStatement.getObject()
                        .stringValue());
            }
            else if(nextStatement.getPredicate().equals(
                    RegexNormalisationRule.getRegexRuleOutputMatchRegex()))
            {
                this.setOutputMatchRegex(nextStatement.getObject()
                        .stringValue());
            }
            else if(nextStatement.getPredicate().equals(
                    RegexNormalisationRule.getRegexRuleOutputReplaceRegex()))
            {
                this.setOutputReplaceRegex(nextStatement.getObject()
                        .stringValue());
            }
            else
            {
                unrecognisedStatements.add(nextStatement);
            }
        }
        
        Collection<URI> tempValidStages = new HashSet<URI>();
        tempValidStages.add(getRdfruleStageQueryVariables());
        tempValidStages.add(getRdfruleStageAfterQueryCreation());
        tempValidStages.add(getRdfruleStageBeforeResultsImport());
        tempValidStages.add(getRdfruleStageAfterResultsToDocument());

        this.setValidStages(tempValidStages);
        

        // TODO: iterate over the tempUnrecognisedStatements and add them to unrecognisedStatements
        //this.unrecognisedStatements = tempUnrecognisedStatements;
        
        // stages.add(NormalisationRule.rdfruleStageQueryVariables.stringValue());
        // stages.add(NormalisationRule.rdfruleStageBeforeResultsImport.stringValue());

        if(RegexNormalisationRule._TRACE)
        {
            RegexNormalisationRule.log
                    .trace("RegexNormalisationRule.fromRdf: would have returned... result="
                            + this.toString());
        }
        
        // if(!isValid)
        // {
            // throw new RuntimeException(
                    // "RegexNormalisationRule.fromRdf: result was not valid");
        // }
    }
    
    public static boolean schemaToRdf(Repository myRepository, String keyToUse, int modelVersion) throws OpenRDFException
    {
        NormalisationRuleImpl.schemaToRdf(myRepository, keyToUse, modelVersion);
        
        final RepositoryConnection con = myRepository.getConnection();
        
        final ValueFactory f = Constants.valueFactory;
        
        try
        {
            final URI contextKeyUri = f.createURI(keyToUse);
            
            con.setAutoCommit(false);
            
            con.add(RegexNormalisationRule.getRegexRuleTypeUri(), RDF.TYPE, OWL.CLASS, contextKeyUri);
            
            con.add(RegexNormalisationRule.getRegexRuleTypeUri(), RDFS.SUBCLASSOF, NormalisationRuleImpl.getNormalisationRuleTypeUri(), contextKeyUri);

            con.add(RegexNormalisationRule.getRegexRuleTypeUri(), RDFS.LABEL, f.createLiteral("A regular expression based normalisation rule intended to denormalise parts of queries to match endpoints, and renormalise the output of the query to match the normalised form."), contextKeyUri);

            con.add(RegexNormalisationRule.getRegexRuleInputMatchRegex(), RDF.TYPE, OWL.DATATYPEPROPERTY, contextKeyUri);
            con.add(RegexNormalisationRule.getRegexRuleInputMatchRegex(), RDFS.RANGE, RDFS.LITERAL, contextKeyUri);
            con.add(RegexNormalisationRule.getRegexRuleInputMatchRegex(), RDFS.DOMAIN, RegexNormalisationRule.getRegexRuleTypeUri(), contextKeyUri);
            con.add(RegexNormalisationRule.getRegexRuleInputMatchRegex(), RDFS.LABEL, f.createLiteral("The input regular expression that is used to identify whether the denormalisation part of the rule matches the data, and if so, whether there are any matching groups that need to be substitued into the replacement pattern."), contextKeyUri);

            con.add(RegexNormalisationRule.getRegexRuleInputReplaceRegex(), RDF.TYPE, OWL.DATATYPEPROPERTY, contextKeyUri);
            con.add(RegexNormalisationRule.getRegexRuleInputReplaceRegex(), RDFS.RANGE, RDFS.LITERAL, contextKeyUri);
            con.add(RegexNormalisationRule.getRegexRuleInputReplaceRegex(), RDFS.DOMAIN, RegexNormalisationRule.getRegexRuleTypeUri(), contextKeyUri);
            con.add(RegexNormalisationRule.getRegexRuleInputReplaceRegex(), RDFS.LABEL, f.createLiteral("The pattern that is used together with the input match regular expression to define what the denormalised data should be."), contextKeyUri);

            con.add(RegexNormalisationRule.getRegexRuleOutputMatchRegex(), RDF.TYPE, OWL.DATATYPEPROPERTY, contextKeyUri);
            con.add(RegexNormalisationRule.getRegexRuleOutputMatchRegex(), RDFS.RANGE, RDFS.LITERAL, contextKeyUri);
            con.add(RegexNormalisationRule.getRegexRuleOutputMatchRegex(), RDFS.DOMAIN, RegexNormalisationRule.getRegexRuleTypeUri(), contextKeyUri);
            con.add(RegexNormalisationRule.getRegexRuleOutputMatchRegex(), RDFS.LABEL, f.createLiteral("The output regular expression that is used to identify whether the renormalisation part of the rule matches the data, and if so, whether there are any matching groups that need to be substitued into the replacement pattern."), contextKeyUri);

            con.add(RegexNormalisationRule.getRegexRuleOutputReplaceRegex(), RDF.TYPE, OWL.DATATYPEPROPERTY, contextKeyUri);
            con.add(RegexNormalisationRule.getRegexRuleOutputReplaceRegex(), RDFS.RANGE, RDFS.LITERAL, contextKeyUri);
            con.add(RegexNormalisationRule.getRegexRuleOutputReplaceRegex(), RDFS.DOMAIN, RegexNormalisationRule.getRegexRuleTypeUri(), contextKeyUri);
            con.add(RegexNormalisationRule.getRegexRuleOutputReplaceRegex(), RDFS.LABEL, f.createLiteral("The pattern that is used together with the output match regular expression to define what the normalised data should be."), contextKeyUri);

            // for(String nextValidStage : validStages)
            // {
                // con.add(RegexNormalisationRule.regexruleTypeUri, NormalisationRule.rdfruleTypeValidForStage,
                    // f.createURI(nextValidStage), contextKeyUri);
            // }

            // If everything went as planned, we can commit the result
            con.commit();
            
            return true;
        }
        catch (final RepositoryException re)
        {
            // Something went wrong during the transaction, so we roll it back
            if(con != null)
            {
                con.rollback();
            }
            
            RegexNormalisationRule.log.error("RepositoryException: "
                    + re.getMessage());
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
    
    public String applyInputRuleToString(String inputText)
    {
        return this.applyRegex(inputText, this.getInputMatchRegex(),
                this.getInputReplaceRegex());
    }
    
    public String applyOutputRuleToString(String inputText)
    {
        return this.applyRegex(inputText, this.getOutputMatchRegex(),
                this.getOutputReplaceRegex());
    }
    
    private String applyRegex(String inputText, String matchRegex,
            String replaceRegex)
    {
        try
        {
            if((matchRegex == null) || (replaceRegex == null))
            {
                if(RegexNormalisationRule._TRACE)
                {
                    RegexNormalisationRule.log
                            .trace("RegexNormalisationRule.applyRegex: something was null matchRegex="
                                    + matchRegex
                                    + ", replaceRegex="
                                    + replaceRegex);
                }
                
                return inputText;
            }
            
            if(RegexNormalisationRule._DEBUG)
            {
                RegexNormalisationRule.log
                        .debug("RegexNormalisationRule.applyRegex: matchRegex="
                                + matchRegex + ", replaceRegex=" + replaceRegex);
            }
            
            if(matchRegex.trim().equals(""))
            {
                if(RegexNormalisationRule._DEBUG)
                {
                    RegexNormalisationRule.log
                            .debug("RegexNormalisationRule.applyRegex: matchRegex was empty, returning inputText");
                }
                
                return inputText;
            }
            
            String debugInputText = "";
            
            // only take a copy of the string if we need it for debugging
            if(RegexNormalisationRule._DEBUG)
            {
                debugInputText = inputText;
            }
            
            inputText = inputText.replaceAll(matchRegex, replaceRegex);
            
            if(RegexNormalisationRule._DEBUG)
            {
                RegexNormalisationRule.log
                        .debug("RegexNormalisationRule.applyRegex: regex complete input="
                                + debugInputText);
                RegexNormalisationRule.log
                        .debug("RegexNormalisationRule.applyRegex: regex complete result="
                                + inputText);
            }
        }
        catch (final PatternSyntaxException pse)
        {
            RegexNormalisationRule.log
                    .error("RegexNormalisationRule.applyRegex: PatternSyntaxException="
                            + pse.getMessage());
        }
        catch (final IllegalArgumentException iae)
        {
            RegexNormalisationRule.log
                    .error("RegexNormalisationRule.applyRegex: IllegalArgumentException="
                            + iae.getMessage());
        }
        catch (final IndexOutOfBoundsException ioobe)
        {
            RegexNormalisationRule.log
                    .error("RegexNormalisationRule.applyRegex: IndexOutOfBoundsException="
                            + ioobe.getMessage());
        }
        
        return inputText;
    }
    
    public Object stageQueryVariables(Object input)
    {
        return stages.contains(getRdfruleStageQueryVariables()) ? this.applyInputRuleToString((String)input) : input ;
    }
    
    public Object stageAfterQueryCreation(Object input)
    {
        return stages.contains(getRdfruleStageAfterQueryCreation()) ? this.applyInputRuleToString((String)input) : input ;
    }

    public Object stageAfterQueryParsing(Object input)
    {
        return input;
    }

    public Object stageBeforeResultsImport(Object input)
    {
        return stages.contains(getRdfruleStageBeforeResultsImport()) ? this.applyOutputRuleToString((String)input) : input ;
    }

    public Object stageAfterResultsImport(Object input)
    {
        return input;
    }

    public Object stageAfterResultsToPool(Object input)
    {
        return input;
    }

    public Object stageAfterResultsToDocument(Object input)
    {
        return stages.contains(getRdfruleStageAfterResultsToDocument()) ? this.applyOutputRuleToString((String)input) : input ;
    }

    // NOTE: it is quite okay to have an empty replace regex, but an empty match
    // is not considered useful here
    public boolean hasInputRule()
    {
        return (this.getInputMatchRegex() != null)
                && (this.getInputReplaceRegex() != null)
                && !this.getInputMatchRegex().trim().equals("");
    }
    
    // NOTE: it is quite okay to have an empty replace regex, but an empty match
    // is not considered useful here
    public boolean hasOutputRule()
    {
        return (this.getOutputMatchRegex() != null)
                && (this.getOutputReplaceRegex() != null)
                && !this.getOutputMatchRegex().trim().equals("");
    }
    
    
    @Override
    public String toHtml()
    {
        String result = "";
        
        result += "<div class=\"rulekey\">Rule Key: "
                + StringUtils.xmlEncodeString(this.getKey().stringValue()) + "</div>\n";
        result += "<div class=\"description\">Description: "
                + StringUtils.xmlEncodeString(this.getDescription()) + "</div>\n";
        result += "<div class=\"order\">Order: "
                + StringUtils.xmlEncodeString(this.getOrder() + "") + "</div>\n";
        result += "<div class=\"inputmatchregex\">Input Match Regex: "
                + StringUtils.xmlEncodeString(this.getInputMatchRegex()) + "</div>\n";
        result += "<div class=\"inputreplaceregex\">Input Replace Regex: "
                + StringUtils.xmlEncodeString(this.getInputReplaceRegex())
                + "</div>\n";
        result += "<div class=\"outputmatchregex\">Output Match Regex: "
                + StringUtils.xmlEncodeString(this.getOutputMatchRegex()) + "</div>\n";
        result += "<div class=\"outputreplaceregex\">Output Replace Regex: "
                + StringUtils.xmlEncodeString(this.getOutputReplaceRegex())
                + "</div>\n";
        
        return result;
    }
    
    @Override
    public String toHtmlFormBody()
    {
        StringBuilder sb = new StringBuilder();
        
        @SuppressWarnings("unused")
        String prefix = "rdfrule_";
        
        return sb.toString();
    }
    
    @Override
    public boolean toRdf(Repository myRepository, URI keyToUse, int modelVersion) throws OpenRDFException
    {
        super.toRdf(myRepository, keyToUse, modelVersion);
        
        final RepositoryConnection con = myRepository.getConnection();
        
        final ValueFactory f = Constants.valueFactory;
        
        try
        {
            if(RegexNormalisationRule._DEBUG)
            {
                RegexNormalisationRule.log
                        .debug("RegexNormalisationRule.toRdf: keyToUse="
                                + keyToUse);
            }
            
            final URI keyUri = keyToUse;
            final Literal inputMatchRegexLiteral = f
                    .createLiteral(this.getInputMatchRegex());
            final Literal inputReplaceRegexLiteral = f
                    .createLiteral(this.getInputReplaceRegex());
            final Literal outputMatchRegexLiteral = f
                    .createLiteral(this.getOutputMatchRegex());
            final Literal outputReplaceRegexLiteral = f
                    .createLiteral(this.getOutputReplaceRegex());
            // final Literal descriptionLiteral = f
                    // .createLiteral(this.description);
            // final Literal orderLiteral = f.createLiteral(this.order);
            // final URI profileIncludeExcludeOrderLiteral = f
                    // .createURI(this.getProfileIncludeExcludeOrder());
            
            // URI curationStatusLiteral = null;
            // 
            // if((this.curationStatus == null)
                    // || this.curationStatus.trim().equals(""))
            // {
                // curationStatusLiteral = ProjectImpl.projectNotCuratedUri;
            // }
            // else
            // {
                // curationStatusLiteral = f.createURI(this.curationStatus);
            // }
            
            con.setAutoCommit(false);
            
            con.add(keyUri, RDF.TYPE, RegexNormalisationRule.getRegexRuleTypeUri(),
                    keyUri);
            // con.add(keyUri, ProjectImpl.projectCurationStatusUri,
                    // curationStatusLiteral, keyUri);
            con.add(keyUri, RegexNormalisationRule.getRegexRuleInputMatchRegex(),
                    inputMatchRegexLiteral, keyUri);
            con.add(keyUri, RegexNormalisationRule.getRegexRuleInputReplaceRegex(),
                    inputReplaceRegexLiteral, keyUri);
            con.add(keyUri, RegexNormalisationRule.getRegexRuleOutputMatchRegex(),
                    outputMatchRegexLiteral, keyUri);
            con.add(keyUri, RegexNormalisationRule.getRegexRuleOutputReplaceRegex(),
                    outputReplaceRegexLiteral, keyUri);
            // if(modelVersion == 1)
            // {
                // con.add(keyUri, RegexNormalisationRule.rdfruleDescription,
                    // descriptionLiteral, keyUri);
            // }
            // else
            // {
                // con.add(keyUri, RDFS.COMMENT,
                    // descriptionLiteral, keyUri);
            // }
            // con.add(keyUri, RegexNormalisationRule.rdfruleOrder, orderLiteral,
                    // keyUri);
            // con.add(keyUri, ProfileImpl.getProfileIncludeExcludeOrderUri(),
                    // profileIncludeExcludeOrderLiteral, keyUri);
            
            // if(this.relatedNamespaces != null)
            // {
                // for(final String nextRelatedNamespace : this.relatedNamespaces)
                // {
                    // con.add(keyUri, RegexNormalisationRule.rdfruleHasRelatedNamespace, f.createURI(nextRelatedNamespace));
                // }
            // }
// 
            // if(this.unrecognisedStatements != null)
            // {
                // for(final Statement nextUnrecognisedStatement : this.unrecognisedStatements)
                // {
                    // con.add(nextUnrecognisedStatement);
                // }
            // }
            
            // If everything went as planned, we can commit the result
            con.commit();
            
            return true;
        }
        catch (final RepositoryException re)
        {
            // Something went wrong during the transaction, so we roll it back
            con.rollback();
            
            RegexNormalisationRule.log.error("RepositoryException: "
                    + re.getMessage());
        }
        finally
        {
            con.close();
        }
        
        return false;
    }
    
    @Override
    public String toString()
    {
        String result = "\n";
        
        result += "key=" + this.getKey() + "\n";
        result += "order=" + this.getOrder()+ "\n";
        result += "inputMatchRegex=" + this.getInputMatchRegex() + "\n";
        result += "inputReplaceRegex=" + this.getInputReplaceRegex() + "\n";
        result += "outputMatchRegex=" + this.getOutputMatchRegex() + "\n";
        result += "outputReplaceRegex=" + this.getOutputReplaceRegex() + "\n";
        result += "description=" + this.getDescription()+ "\n";
        
        return result;
    }
    
    /**
     * @return the URI used for the rdf Type of these elements
     */
    @Override
    public URI getElementType()
    {
        return getRegexRuleTypeUri();
    }

	/**
	 * @param inputMatchRegex the inputMatchRegex to set
	 */
	public void setInputMatchRegex(String inputMatchRegex) {
		this.inputMatchRegex = inputMatchRegex;
	}

	/**
	 * @return the inputMatchRegex
	 */
	public String getInputMatchRegex() {
		return inputMatchRegex;
	}

	/**
	 * @param inputReplaceRegex the inputReplaceRegex to set
	 */
	public void setInputReplaceRegex(String inputReplaceRegex) {
		this.inputReplaceRegex = inputReplaceRegex;
	}

	/**
	 * @return the inputReplaceRegex
	 */
	public String getInputReplaceRegex() {
		return inputReplaceRegex;
	}

	/**
	 * @param outputMatchRegex the outputMatchRegex to set
	 */
	public void setOutputMatchRegex(String outputMatchRegex) {
		this.outputMatchRegex = outputMatchRegex;
	}

	/**
	 * @return the outputMatchRegex
	 */
	public String getOutputMatchRegex() {
		return outputMatchRegex;
	}

	/**
	 * @param outputReplaceRegex the outputReplaceRegex to set
	 */
	public void setOutputReplaceRegex(String outputReplaceRegex) {
		this.outputReplaceRegex = outputReplaceRegex;
	}

	/**
	 * @return the outputReplaceRegex
	 */
	public String getOutputReplaceRegex() {
		return outputReplaceRegex;
	}

	/**
	 * @param regexruleTypeUri the regexruleTypeUri to set
	 */
	public static void setRegexRuleTypeUri(URI regexruleTypeUri) {
		RegexNormalisationRule.regexruleTypeUri = regexruleTypeUri;
	}

	/**
	 * @return the regexruleTypeUri
	 */
	public static URI getRegexRuleTypeUri() {
		return regexruleTypeUri;
	}

	/**
	 * @param rdfruleInputMatchRegex the rdfruleInputMatchRegex to set
	 */
	public static void setRegexRuleInputMatchRegex(
			URI rdfruleInputMatchRegex) {
		RegexNormalisationRule.regexruleInputMatchRegex = rdfruleInputMatchRegex;
	}

	/**
	 * @return the rdfruleInputMatchRegex
	 */
	public static URI getRegexRuleInputMatchRegex() {
		return regexruleInputMatchRegex;
	}

	/**
	 * @param rdfruleInputReplaceRegex the rdfruleInputReplaceRegex to set
	 */
	public static void setRegexRuleInputReplaceRegex(
			URI rdfruleInputReplaceRegex) {
		RegexNormalisationRule.regexruleInputReplaceRegex = rdfruleInputReplaceRegex;
	}

	/**
	 * @return the rdfruleInputReplaceRegex
	 */
	public static URI getRegexRuleInputReplaceRegex() {
		return regexruleInputReplaceRegex;
	}

	/**
	 * @param rdfruleOutputMatchRegex the rdfruleOutputMatchRegex to set
	 */
	public static void setRegexRuleOutputMatchRegex(
			URI rdfruleOutputMatchRegex) {
		RegexNormalisationRule.regexruleOutputMatchRegex = rdfruleOutputMatchRegex;
	}

	/**
	 * @return the rdfruleOutputMatchRegex
	 */
	public static URI getRegexRuleOutputMatchRegex() {
		return regexruleOutputMatchRegex;
	}

	/**
	 * @param rdfruleOutputReplaceRegex the rdfruleOutputReplaceRegex to set
	 */
	public static void setRegexRuleOutputReplaceRegex(
			URI rdfruleOutputReplaceRegex) {
		RegexNormalisationRule.regexruleOutputReplaceRegex = rdfruleOutputReplaceRegex;
	}

	/**
	 * @return the rdfruleOutputReplaceRegex
	 */
	public static URI getRegexRuleOutputReplaceRegex() {
		return regexruleOutputReplaceRegex;
	}
}
