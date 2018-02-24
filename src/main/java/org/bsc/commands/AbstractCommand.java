package org.bsc.commands;

import java.io.PrintStream;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.dependencies.builder.DependencyQueryBuilder;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.ui.context.UIContextProvider;

public abstract class AbstractCommand extends AbstractProjectCommand implements Constants {

	@Inject DependencyResolver dependencyResolver;
	
    private CoordinateBuilder confluencePluginDep;
    
    private java.util.Properties buildProperties = new java.util.Properties();
    
    @PostConstruct
    void initialize() {

    		try( java.io.InputStream is = getClass().getClassLoader().getResourceAsStream("build.properties") ) {

    			buildProperties.load( is );
    			
    		}
    		catch( Exception ex ) {
    			
    		}
    }
    
    <T extends UIContextProvider> void printBuildInfos(final T context) {
    		final PrintStream out = context.getUIContext().getProvider().getOutput().out();
    		
		out.printf( "version=%s\ntimestamp=%s\n", 
				buildProperties.getProperty("version", "unknown"),
				//buildProperties.getProperty("revision", "unknown"),
				buildProperties.getProperty("timestamp", "unknown")
			);
		out.flush();
    	
    }
    
    <T extends UIContextProvider> CoordinateBuilder getConfluencePluginDependency(final T context) {
    	
    		if( Objects.isNull(confluencePluginDep) ) {
    		
			final PrintStream out = context.getUIContext().getProvider().getOutput().out();
			
			final java.util.List<org.jboss.forge.addon.dependencies.Coordinate> coords = 
					dependencyResolver.resolveVersions(
							DependencyQueryBuilder.create( String.format("%s:%s", PLUGIN_GROUPID, PLUGIN_ARTIFACTID) ));
			
			//coords.forEach( c -> out.println(c) ); out.flush();
			
			if( coords.isEmpty() ) {
				out.println("Plugin avaliable dependencies not found! default 5.0 is used"); out.flush();
				return CoordinateBuilder.create(String.format("%s:%s", PLUGIN_GROUPID, PLUGIN_ARTIFACTID, "5.0"));
			}
			
			final org.jboss.forge.addon.dependencies.Coordinate coord = coords.get( coords.size()-1 );
			out.printf( "use plugin [%s]\n", coord.toString()); out.flush();
			
			
			confluencePluginDep =  CoordinateBuilder.create(coord);
    		}
		return confluencePluginDep;
		
	}

}
