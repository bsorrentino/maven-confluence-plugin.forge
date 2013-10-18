package org.bsc.forge;

import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.apache.maven.project.MavenProject;
import org.bsc.forge.MavenUtil.F;
import static org.bsc.forge.MavenUtil.addMavenProjectProperty;
import org.codehaus.plexus.util.IOUtil;

import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.maven.MavenCoreFacet;
import org.jboss.forge.maven.MavenPluginFacet;
import org.jboss.forge.maven.plugins.ConfigurationBuilder;
import org.jboss.forge.maven.plugins.MavenPluginBuilder;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.services.ResourceFactory;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellColor;
import org.jboss.forge.shell.exceptions.AbortedException;
import org.jboss.forge.shell.plugins.RequiresProject;
import org.jboss.forge.shell.plugins.SetupCommand;

/**
 *
 */
@Alias("confluence")
@RequiresFacet({MavenCoreFacet.class, MavenPluginFacet.class})
@RequiresProject
public class ConfluenceForgePlugin implements Plugin {

    public static final String MSG_SETUP_INTERRUPTED = "setup interrupted!";
    public static final String MESG_FOLDER_CREATED = "folder created!";

    public static final String PLUGIN_GROUPID = "org.bsc.maven";
    public static final String PLUGIN_ARTIFACTID = "maven-confluence-reporting-plugin";
    public static final String PLUGIN_VERSION = "3.4.2";

    public static final String PLUGIN_KEY_2 = PLUGIN_GROUPID + ":" + PLUGIN_ARTIFACTID;
    public static final String PLUGIN_KEY_3 = PLUGIN_GROUPID + ":" + PLUGIN_ARTIFACTID + ":" + PLUGIN_VERSION;

    @Inject
    private Shell/*Prompt*/ prompt;

    @Inject
    private Project project;

    @Inject
    private ResourceFactory resourceFactory;

    @SetupCommand
    public void setup(PipeOut out) {

        final MavenCoreFacet facet = project.getFacet(MavenCoreFacet.class);

        final MavenProject mProject = facet.getMavenProject();

        final String siteDir = String.format("%s/src/site/confluence", project.getProjectRoot().getFullyQualifiedName());

        java.io.File siteDirFile = new java.io.File(siteDir);

       //final DirectoryResource root = project.getProjectRoot();
        //final DirectoryResource siteDirRes = root.createFrom(siteDirFile);
        while (!siteDirFile.exists()) {

            final boolean createFolder = prompt.promptBoolean(String.format("Do you want create missing folder [%s]: ", siteDirFile.getPath()), true);

            if (createFolder) {

                final boolean success = siteDirFile.mkdirs();

                if (success) {
                    out.println(MESG_FOLDER_CREATED);
                    break;
                } else {
                    out.println(ShellColor.RED, String.format("error creating folder [%s]!", siteDir));
                    return;
                }
            } else {

                final String newFolder = prompt.prompt("Please, give me the site folder relative to ${basedir}. press enter to abort: ");

                if (newFolder == null || newFolder.isEmpty()) {
                    out.println(MSG_SETUP_INTERRUPTED);
                    return;
                }
                siteDirFile = new java.io.File(mProject.getBasedir(), newFolder);

            }
        }

       //DirectoryResource siteDirRes =  (DirectoryResource) resourceFactory.getResourceFrom(siteDirFile);
        try {
            final java.io.InputStream confluenceTemplatePage = getClass().getClassLoader().getResourceAsStream("template.confluence");
            final java.io.Writer confluenceHomePage = new FileWriter(new java.io.File(siteDirFile, "home.confluence"));

            IOUtil.copy(confluenceTemplatePage, confluenceHomePage);

        } catch (IOException ex) {

            out.println(ShellColor.RED, "error copying home page template ....! Set VERBOSE for details");

            printVerbose(ex);

            Logger.getLogger(ConfluenceForgePlugin.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            final java.io.InputStream siteTemplatePage = getClass().getClassLoader().getResourceAsStream("site.xml");
            final java.io.Writer sitePage = new FileWriter(new java.io.File(siteDirFile, "site.xml"));

            IOUtil.copy(siteTemplatePage, sitePage);

        } catch (IOException ex) {

            out.println(ShellColor.RED, "error copying site template ....! Set VERBOSE for details");

            printVerbose(ex);

            Logger.getLogger(ConfluenceForgePlugin.class.getName()).log(Level.SEVERE, null, ex);
        }

        createConfluenceMavenPlugin(mProject);
    }

    private void createConfluenceMavenPlugin(final MavenProject mProject) {

        final org.apache.maven.model.Plugin plugin = mProject.getPlugin(PLUGIN_KEY_2);

        if (plugin != null) {

            prompt.println("Plugin already exist!");
            return;
        }

        final DependencyBuilder confluencePluginDep
                = DependencyBuilder.create(PLUGIN_KEY_3);

        final MavenPluginFacet pluginFacet
                = project.getFacet(MavenPluginFacet.class);

        if (pluginFacet.hasPlugin(confluencePluginDep)) {

            prompt.println("Plugin already exist!");
            return;

        }

        {
            final MavenPluginBuilder pb
                    = MavenPluginBuilder.create()
                      .setDependency(confluencePluginDep)  
                    ;
            final ConfigurationBuilder cb = pb.createConfiguration();
            
            // SET CONFIGURATION
            cb.createConfigurationElement("serverId")
            .setText(inputConfigElement("Please, give me the serverId for access to confluence", null))
            .getParentPluginConfig()
            .createConfigurationElement("endPoint")
                    .setText(inputConfigElement("Please, give me the confluence's 'URL'", new F<String, String>() {
                        @Override
                        public String f(String param) {
                                          
                            if( param.trim().endsWith("/")) {
                                param = param.substring(0, param.length()-1);
                            }
                            
                            addMavenProjectProperty(project, "confluence.home", param );

                            return "${confluence.home}/rpc/xmlrpc";
                        }
                    })
            )
            .getParentPluginConfig()
            .createConfigurationElement("spaceKey")
                    .setText(inputConfigElement("Please, give me the confluence's 'SPACE KEY'", null))
            .getParentPluginConfig()
                 .createConfigurationElement("parentPageTitle")
                    .setText(inputConfigElement("Please, give me the confluence's 'PARENT PAGE NAME'", "Home", null))
            .getParentPluginConfig()
                .createConfigurationElement("wikiFilesExt")
                    .setText(".confluence")
            .getParentPluginConfig()
             .createConfigurationElement("properties")
                        ;
            
            pb.setConfiguration(cb);

            pluginFacet.addPlugin(pb);
            
        }
    }

    private void printVerbose(Throwable t) {

        java.io.StringWriter sw = new java.io.StringWriter(4096);
        java.io.PrintWriter w = new java.io.PrintWriter(sw);
        t.printStackTrace(w);

        prompt.printlnVerbose(ShellColor.RED, sw.toString());
    }

    private String inputConfigElement(String msg, String defaultValue, F<String, String> functor) {

        String value = prompt.prompt(String.format("%s. Press enter for default '%s': ", msg, defaultValue), defaultValue);

        if (functor != null) {
            value = functor.f(value);
        }

        if (value == null || value.isEmpty()) {
            prompt.println(MSG_SETUP_INTERRUPTED);
            throw new AbortedException(MSG_SETUP_INTERRUPTED);
        }

        return value;
    }

    private String inputConfigElement(String msg, F<String, String> functor) {

        String value = prompt.prompt(String.format("%s. Press enter to abort: ", msg));
        if (functor != null) {
            value = functor.f(value);
        }

        if (value == null || value.isEmpty()) {
            prompt.println(MSG_SETUP_INTERRUPTED);
            throw new AbortedException(MSG_SETUP_INTERRUPTED);
        }


        return value;
    }

}
