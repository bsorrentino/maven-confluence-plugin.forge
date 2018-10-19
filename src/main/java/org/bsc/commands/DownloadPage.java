package org.bsc.commands;

import java.io.PrintStream;
import java.util.Objects;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.apache.maven.settings.Proxy;
import org.bsc.confluence.ConfluenceProxy;
import org.bsc.confluence.ConfluenceService;
import org.bsc.confluence.ConfluenceServiceFactory;
import org.bsc.core.MavenHelper;
import org.bsc.ssl.SSLCertificateInfo;
import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.dependencies.builder.DependencyQueryBuilder;
import org.jboss.forge.addon.maven.plugins.Configuration;
import org.jboss.forge.addon.maven.plugins.MavenPlugin;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.maven.projects.MavenPluginFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.Projects;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIContextProvider;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.hints.InputType;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

public class DownloadPage extends AbstractCommand implements Constants {

    @Inject
    @WithAttributes(label = "Username", required = true)
    UIInput<String> username;

    @Inject
    @WithAttributes(label = "Password", required = true, type = InputType.SECRET)
    UIInput<String> password;

    @Inject
    @WithAttributes(label = "Target", required = true, type = InputType.DIRECTORY_PICKER)
    private UIInput<DirectoryResource> target;

    @Inject
    @WithAttributes(label = "Title", required = true)
    UIInput<String> title;

    @Inject
    ProjectFactory projectFactory;

    @Inject
    ResourceFactory resourceFactory;

	@Inject 
	DependencyResolver dependencyResolver;

      @Override
    protected boolean isProjectRequired() {
        return true;
    }

    @Override
    protected ProjectFactory getProjectFactory() {
        return projectFactory;
    }

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(DownloadPage.class)
                .name("confluence-downloadPage")
                .category(Categories.create("Confluence"));
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
    	
 	   printBuildInfos(builder);

        final Project project = Projects.getSelectedProject(getProjectFactory(),
                builder.getUIContext());

        DirectoryResource ds = (DirectoryResource) project.getRoot();

        DirectoryResource siteDir = ds.getChildDirectory("src/site/confluence");

        target.setDefaultValue(siteDir.exists() ? siteDir : ds);

        final MavenPluginFacet pluginFacet = project.getFacet(MavenPluginFacet.class);

        final CoordinateBuilder confluencePluginDep = getConfluencePluginDependency(builder);
        
        if (pluginFacet.hasPlugin(confluencePluginDep)) {
            final MavenPlugin plugin = pluginFacet.getPlugin(confluencePluginDep);

            final Configuration conf = plugin.getConfig();

            if (conf.hasConfigurationElement("title")) {
                title.setDefaultValue(conf.getConfigurationElement("title").getText());
            }

        }

        builder.add(username);
        builder.add(password);
        builder.add(target);
        builder.add(title);
    }

    @Override
    public Result execute(UIExecutionContext context) {

        final PrintStream out = context.getUIContext().getProvider().getOutput().out();

        final Project project = super.getSelectedProject(context);

        final MavenFacet facet = project.getFacet(MavenFacet.class);

        final MavenPluginFacet pluginFacet = project.getFacet(MavenPluginFacet.class);

        final CoordinateBuilder confluencePluginDep = getConfluencePluginDependency(context);

        if (!pluginFacet.hasPlugin(confluencePluginDep)) {
            throw new IllegalStateException(String.format(
                    "Project hasn't defined Plugin [%s]", confluencePluginDep));
        }

        final MavenPlugin plugin = pluginFacet.getPlugin(confluencePluginDep);

        final Configuration conf = plugin.getConfig();

        final String endPoint = conf.getConfigurationElement(CFGELEM_ENDPOINT)
                .getText();
        final String space = conf.getConfigurationElement(CFGELEM_SPACEKEY)
                .getText();

        try {
            final ConfluenceService.Credentials credentials = 
        			new ConfluenceService.Credentials(username.getValue(), password.getValue());

            final SSLCertificateInfo ssl = new SSLCertificateInfo();

            final String resolvedEndpoint = facet.resolveProperties(endPoint);

            confluenceExecute(
            			resolvedEndpoint,
            			credentials, 
            			ssl,
                   c -> {
                       
                       final String targetPath = String.format("%s/%s.confluence", target.getValue().getFullyQualifiedName(), title.getValue());

                       final FileResource<?> file = (FileResource<?>) resourceFactory.create(new java.io.File(targetPath));

                       if (!file.exists()) {

                           if (!file.createNewFile()) {

                               throw new Error(String.format("error creating file [%s]", file.getName()));
                           }
                       }

                        c.getPage(space, title.getValue()).join().ifPresent( page -> {
                            
                    
                            //final String content = page.getContent();
                            //file.setContents( content );
                            try {
                                final String content = String.valueOf(page.getClass().getMethod("getContent").invoke(page));
                                file.setContents( content );
                                out.printf("set donloaded content to [%s]\n", file.getName());
                            } catch (Exception e) {
                                throw new Error(e);
                            }
                            
                        });
                        

                    });
        } catch (Exception e) {
            return Results.fail("error!", e);
        }

        return Results.success("completed!");

    }

    /**
     *
     * @param endpoint
     * @param username
     * @param password
     * @param task
     */
    protected void confluenceExecute(
    			String endpoint, 
    			ConfluenceService.Credentials credentials, 
    			SSLCertificateInfo ssl,
            Consumer<ConfluenceService> task) throws Exception {

        ConfluenceService confluence = null;

        try {


            ConfluenceProxy proxyInfo = null;

            final Proxy activeProxy = MavenHelper.getSettings().getActiveProxy();

            if (activeProxy != null) {

                proxyInfo = new ConfluenceProxy(
                			activeProxy.getHost(),
                         activeProxy.getPort(),
                         activeProxy.getUsername(),
                         activeProxy.getPassword(),
                         activeProxy.getNonProxyHosts());
            }

            confluence = ConfluenceServiceFactory.createInstance( endpoint, credentials, proxyInfo, ssl);

            confluence.call( task );

        } catch (Exception e) {

			// getLog().error("has been imposssible connect to confluence due exception",
            // e);
            throw e;
        } 

    }
    
    CoordinateBuilder confluencePluginDep;
    
    <T extends UIContextProvider> CoordinateBuilder getConfluencePluginDependency(final T context) {
    	
    		if( Objects.isNull(confluencePluginDep) ) {
    		
			final PrintStream out = context.getUIContext().getProvider().getOutput().out();
			
			final java.util.List<org.jboss.forge.addon.dependencies.Coordinate> coords = 
					dependencyResolver.resolveVersions(
							DependencyQueryBuilder.create( String.format("%s:%s", PLUGIN_GROUPID, PLUGIN_ARTIFACTID) ));
			
			
			if( coords.isEmpty() ) {
				out.println("Plugin avaliable dependencies not found! default 5.0 is used");
				return CoordinateBuilder.create(String.format("%s:%s", PLUGIN_GROUPID, PLUGIN_ARTIFACTID, "5.0"));
			}
			
			final org.jboss.forge.addon.dependencies.Coordinate coord = coords.get( coords.size()-1 );
			out.printf( "use plugin [%s]\n", coord.toString());
			
			confluencePluginDep =  CoordinateBuilder.create(coord);
    		}
		return confluencePluginDep;
		
	}
}
