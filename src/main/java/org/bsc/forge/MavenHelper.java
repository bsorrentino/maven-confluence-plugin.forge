/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.bsc.forge;

import java.io.File;

import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingResult;
import org.jboss.forge.maven.MavenCoreFacet;
import org.jboss.forge.maven.plugins.ConfigurationBuilder;
import org.jboss.forge.maven.plugins.ConfigurationElementBuilder;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.ProjectModelException;
import org.jboss.forge.shell.util.OSUtils;

/**
 *
 * @author softphone
 */
public class MavenHelper {
	
	private static final String M2_HOME = System.getenv().get("M2_HOME");
   
	/**
	 * 
	 * @return
	 */
	public static  Settings getSettings()
	   {
	      try
	      {
	         SettingsBuilder settingsBuilder = new DefaultSettingsBuilderFactory().newInstance();
	         SettingsBuildingRequest settingsRequest = new DefaultSettingsBuildingRequest();
	         settingsRequest
	                  .setUserSettingsFile(new File(OSUtils.getUserHomeDir().getAbsolutePath() + "/.m2/settings.xml"));

	         if (M2_HOME != null)
	            settingsRequest.setGlobalSettingsFile(new File(M2_HOME + "/conf/settings.xml"));

	         SettingsBuildingResult settingsBuildingResult = settingsBuilder.build(settingsRequest);
	         Settings effectiveSettings = settingsBuildingResult.getEffectiveSettings();

	         if (effectiveSettings.getLocalRepository() == null)
	         {
	            effectiveSettings.setLocalRepository(OSUtils.getUserHomeDir().getAbsolutePath() + "/.m2/repository");
	         }

	         return effectiveSettings;
	      }
	      catch (SettingsBuildingException e)
	      {
	         throw new ProjectModelException(e);
	      }
	   }    
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
     * 
     * @param conf
     * @param id
     * @return
     */
    public static ConfigurationElementBuilder getOrCreateConfigurationElement( ConfigurationBuilder cb, String id ) {
    	
    	return (ConfigurationElementBuilder) (( cb.hasConfigurationElement(id) ) ? 
    			cb.getConfigurationElement(id) :
    			cb.createConfigurationElement(id));
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
   
    /**
     * add or update property 
     * 
     * @param project
     * @param key
     * @param value 
     */
    public static void setMavenProjectProperty( Project project, String key, String value ) 
    {
        final MavenCoreFacet mcf = project.getFacet(MavenCoreFacet.class);
        
        setMavenProjectProperty(mcf, key, value);
     
    }
    
    /**
     * add or update property 
     * 
     * @param project
     * @param key
     * @param value 
     */
    private static void setMavenProjectProperty( MavenCoreFacet mcf, String key, String value ) 
    {
        final Model pom = mcf.getPOM();

        java.util.Properties pp = pom.getProperties();

        pp.setProperty(key, value);
            
        mcf.setPOM(pom);
        
    }
    
}
