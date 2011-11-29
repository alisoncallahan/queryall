/**
 * 
 */
package org.queryall.impl.querytype.test;

import org.queryall.api.profile.Profile;
import org.queryall.api.querytype.RegexInputQueryType;
import org.queryall.api.test.AbstractRegexInputQueryTypeTest;
import org.queryall.impl.profile.ProfileImpl;
import org.queryall.impl.querytype.RegexInputQueryTypeImpl;

/**
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class RegexInputQueryTypeImpl3Test extends AbstractRegexInputQueryTypeTest
{
    @Override
    public Profile getNewTestProfile()
    {
        return new ProfileImpl();
    }
    
    @Override
    public RegexInputQueryType getNewTestRegexInputQueryType()
    {
        return new RegexInputQueryTypeImpl();
    }
}