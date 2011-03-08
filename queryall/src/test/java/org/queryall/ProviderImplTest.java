/**
 * 
 */
package org.queryall;

import org.openrdf.model.URI;
import org.queryall.impl.ProfileImpl;
import org.queryall.impl.ProviderImpl;

/**
 * Provides the implementation of the Provider class 
 * for the Abstract test class provided with queryall-api-tests.jar
 * 
 * @author peter
 *
 */
public class ProviderImplTest extends AbstractProviderTest
{
    @Override
    public Provider getNewTestProvider()
    {
        return new ProviderImpl();
    }

    public URI getProfileExcludeThenIncludeURI()
    {
        return ProfileImpl.getExcludeThenIncludeUri();
    }

    public URI getProfileIncludeThenExcludeURI()
    {
        return ProfileImpl.getIncludeThenExcludeUri();
    }

    public Profile getNewTestProfile()
    {
        return new ProfileImpl();
    }

    public URI getProfileIncludeExcludeOrderUndefinedUri()
    {
        return ProfileImpl.getProfileIncludeExcludeOrderUndefinedUri();
    }
}
