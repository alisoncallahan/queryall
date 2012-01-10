/**
 * 
 */
package org.queryall.api.project;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.openrdf.model.URI;
import org.queryall.api.services.QueryAllEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Project implementations register themselves with this enumeration when they are loaded.
 * 
 * NOTE: This is not an enumeration as java does not support extensible enumerations, but it should
 * act like one for all purposes other than java's underpowered switch case statement.
 * 
 */
public class ProjectEnum extends QueryAllEnum
{
    private static final Logger LOG = LoggerFactory.getLogger(ProjectEnum.class);
    @SuppressWarnings("unused")
    private static final boolean TRACE = ProjectEnum.LOG.isTraceEnabled();
    private static final boolean DEBUG = ProjectEnum.LOG.isDebugEnabled();
    @SuppressWarnings("unused")
    private static final boolean INFO = ProjectEnum.LOG.isInfoEnabled();
    
    protected static final Set<ProjectEnum> ALL_PROJECTS = new HashSet<ProjectEnum>();
    
    public static Collection<ProjectEnum> byTypeUris(final Set<URI> nextTypeUris)
    {
        if(nextTypeUris.size() == 0)
        {
            if(ProjectEnum.DEBUG)
            {
                ProjectEnum.LOG.debug("found an empty URI set for nextProjectUris=" + nextTypeUris);
            }
            return Collections.emptySet();
        }
        
        final Set<ProjectEnum> results = new HashSet<ProjectEnum>();
        
        for(final ProjectEnum nextEnum : ProjectEnum.ALL_PROJECTS)
        {
            if(nextEnum.matchForTypeUris(nextTypeUris))
            {
                if(ProjectEnum.DEBUG)
                {
                    ProjectEnum.LOG.debug("found a matching URI set for nextProjectUris=" + nextTypeUris);
                }
                results.add(nextEnum);
            }
        }
        
        if(ProjectEnum.DEBUG)
        {
            ProjectEnum.LOG
                    .debug("returning results.size()=" + results.size() + " for nextProjectUris=" + nextTypeUris);
        }
        
        return results;
    }
    
    /**
     * Registers the specified project.
     */
    public static void register(final ProjectEnum nextProject)
    {
        if(ProjectEnum.valueOf(nextProject.getName()) != null)
        {
            if(ProjectEnum.DEBUG)
            {
                ProjectEnum.LOG.debug("Cannot register this project again name=" + nextProject.getName());
            }
        }
        else
        {
            ProjectEnum.ALL_PROJECTS.add(nextProject);
        }
    }
    
    public static ProjectEnum register(final String name, final Set<URI> typeURIs)
    {
        final ProjectEnum newProjectEnum = new ProjectEnum(name, typeURIs);
        ProjectEnum.register(newProjectEnum);
        return newProjectEnum;
    }
    
    public static ProjectEnum valueOf(final String string)
    {
        for(final ProjectEnum nextProjectEnum : ProjectEnum.ALL_PROJECTS)
        {
            if(nextProjectEnum.getName().equals(string))
            {
                return nextProjectEnum;
            }
        }
        
        return null;
    }
    
    /**
     * Returns all known/registered projects.
     */
    public static Collection<ProjectEnum> values()
    {
        return Collections.unmodifiableCollection(ProjectEnum.ALL_PROJECTS);
    }
    
    /**
     * Create a new Project enum using the given name, which must be unique.
     * 
     * @param nextName
     * @param nextTypeURIs
     */
    public ProjectEnum(final String nextName, final Set<URI> nextTypeURIs)
    {
        super(nextName, nextTypeURIs);
        ProjectEnum.ALL_PROJECTS.add(this);
    }
}
