/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.bsc.forge;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.jboss.forge.maven.MavenCoreFacet;
import org.jboss.forge.project.Project;

/**
 *
 * @author softphone
 */
public class MavenUtil {
    
    public interface F<P,R> {
        
        R f( P param );
    };
    
    /**
     *
     * @param project
     * @param predicate
     * @return
     */
    public static Plugin findPluginManagement( MavenProject project, F<Plugin,Boolean> predicate ) {
        
        java.util.List<Plugin> plugins = project.getPluginManagement().getPlugins();
        
        for( Plugin p : plugins ) {
            
            if( predicate.f(p) ) {
                return p;
            }
        }
        
        return null;
    }
    
    /**
     * add property if it doesn't exist
     * 
     * @param project
     * @param key
     * @param value 
     */
    public static void addMavenProjectProperty( Project project, String key, String value ) 
    {
        final MavenCoreFacet mcf = project.getFacet(MavenCoreFacet.class);
        
        addMavenProjectProperty(mcf, key, value);
     
    }
    
    /**
     * add property if it doesn't exist
     * 
     * @param project
     * @param key
     * @param value 
     */
    private static void addMavenProjectProperty( MavenCoreFacet mcf, String key, String value ) 
    {
        final Model pom = mcf.getPOM();

        java.util.Properties pp = pom.getProperties();

        if( !pp.containsKey(key)) {
            pp.setProperty(key, value);
            
            mcf.setPOM(pom);

        }
        
        
    }
}
