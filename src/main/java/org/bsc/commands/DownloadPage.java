package org.bsc.commands;

import java.io.PrintStream;
import java.lang.reflect.Method;

import javax.inject.Inject;

import org.apache.maven.settings.Proxy;
import org.bsc.confluence.ConfluenceProxy;
import org.bsc.confluence.ConfluenceService;
import org.bsc.confluence.ConfluenceService.Model.Page;
import org.bsc.confluence.ConfluenceServiceFactory;
import org.bsc.core.MavenHelper;
import org.bsc.ssl.SSLCertificateInfo;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.maven.plugins.Configuration;
import org.jboss.forge.addon.maven.plugins.MavenPlugin;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.maven.projects.MavenPluginFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.Projects;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.ResourceFactory;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.hints.InputType;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UIPrompt;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

import rx.functions.Action1;

public class DownloadPage extends AbstractProjectCommand implements Constants {

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

    private final CoordinateBuilder confluencePluginDep = CoordinateBuilder.create(PLUGIN_KEY_3);

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
        final Project project = Projects.getSelectedProject(getProjectFactory(),
                builder.getUIContext());

        DirectoryResource ds = (DirectoryResource) project.getRoot();

        DirectoryResource siteDir = ds.getChildDirectory("src/site/confluence");

        target.setDefaultValue(siteDir.exists() ? siteDir : ds);

        final MavenPluginFacet pluginFacet = project.getFacet(MavenPluginFacet.class);

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

        final UIPrompt prompt = context.getPrompt();
        final PrintStream out = context.getUIContext().getProvider().getOutput().out();

        final Project project = super.getSelectedProject(context);

        final MavenFacet facet = project.getFacet(MavenFacet.class);

        final MavenPluginFacet pluginFacet = project.getFacet(MavenPluginFacet.class);

        if (!pluginFacet.hasPlugin(confluencePluginDep)) {
            throw new IllegalStateException(String.format(
                    "Project hasn't defined Plugin [%s]", PLUGIN_KEY_3));
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
                   (c) -> {

                        final Page page = c.getPage(space, title.getValue());
                        
                        final String targetPath = String.format("%s/%s.confluence", target.getValue().getFullyQualifiedName(), title.getValue());

                        FileResource<?> file = (FileResource<?>) resourceFactory.create(new java.io.File(targetPath));

                        if (!file.exists()) {

                            if (!file.createNewFile()) {

                                throw new Exception(String.format("error creating file [%s]", file.getName()));
                            }
                        }

                        file.setContents(page.getContent());
                        out.printf("set donloaded content to [%s]\n", file.getName());

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
            Action1<ConfluenceService> task) throws Exception {

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

 
}
