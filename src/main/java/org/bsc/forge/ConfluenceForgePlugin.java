package org.bsc.forge;


import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.IOUtil;

import org.jboss.forge.shell.plugins.Alias;
import org.jboss.forge.shell.plugins.PipeOut;
import org.jboss.forge.shell.plugins.Plugin;
import org.jboss.forge.shell.plugins.RequiresFacet;
import org.jboss.forge.maven.MavenCoreFacet;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.services.ResourceFactory;
import org.jboss.forge.resources.DirectoryResource;
import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellColor;
import org.jboss.forge.shell.plugins.RequiresProject;
import org.jboss.forge.shell.plugins.SetupCommand;

/**
 *
 */
@Alias("confluence")
@RequiresFacet({MavenCoreFacet.class})
@RequiresProject
public class ConfluenceForgePlugin implements Plugin
{
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
       
       final String sourceDirectory = mProject.getBuild().getSourceDirectory();
       
       final String siteDir = String.format("%s/site/confluence", sourceDirectory);
       
       java.io.File siteDirFile = new java.io.File( siteDir );
       
       //final DirectoryResource root = project.getProjectRoot();
       //final DirectoryResource siteDirRes = root.createFrom(siteDirFile);
       
       while( !siteDirFile.exists() ) {
        
            final boolean createFolder = prompt.promptBoolean( String.format("Do you want create missing folder [%s] ", siteDir), true);

            if( createFolder ) {

                final boolean success = siteDirFile.mkdirs();

                if( success ) {
                     out.println( MESG_FOLDER_CREATED);
                     break;
                }
                else {
                    out.println(ShellColor.RED, String.format("error creating folder [%s]", siteDir));
                    return;
                }
            }
            else {

                final String newFolder = prompt.prompt( "Please, give me the site folder relative to ${basedir}. press enter to abort");

                if( newFolder==null || newFolder.isEmpty()) {
                     out.println( MSG_SETUP_INTERRUPTED);
                    return;
                }
                siteDirFile = new java.io.File( mProject.getBasedir(), newFolder );

            }
        }
       
       //DirectoryResource siteDirRes =  (DirectoryResource) resourceFactory.getResourceFrom(siteDirFile);
        
       try {
            final java.io.InputStream confluenceTemplatePage = getClass().getClassLoader().getResourceAsStream("template.confluence");
            final java.io.Writer confluenceHomePage = new FileWriter( new java.io.File(siteDirFile, "home.confluence") );

            IOUtil.copy( confluenceTemplatePage , confluenceHomePage);
            
       } catch (IOException ex) {
           
           out.println(ShellColor.RED, "error copying home page template ....! Set VERBOSE for details");
                   
           printVerbose(ex);
           
           Logger.getLogger(ConfluenceForgePlugin.class.getName()).log(Level.SEVERE, null, ex);
       }
       try {
            final java.io.InputStream siteTemplatePage = getClass().getClassLoader().getResourceAsStream("site.xml");
            final java.io.Writer sitePage = new FileWriter( new java.io.File(siteDirFile, "site.xml") );

            IOUtil.copy( siteTemplatePage , sitePage);
            
       } catch (IOException ex) {
           
           out.println(ShellColor.RED, "error copying site template ....! Set VERBOSE for details");
                   
           printVerbose(ex);
           
           Logger.getLogger(ConfluenceForgePlugin.class.getName()).log(Level.SEVERE, null, ex);
       }
        
   }
   
   private void printVerbose( Throwable t ) {
   
       java.io.StringWriter sw = new java.io.StringWriter(4096);
       java.io.PrintWriter w = new java.io.PrintWriter( sw );
       t.printStackTrace( w );
       
       prompt.printlnVerbose(ShellColor.RED, sw.toString());
   }
   
    public static final String MSG_SETUP_INTERRUPTED = "setup interrupted!";
    public static final String MESG_FOLDER_CREATED = "folder created!";
    
    public static final String PLUGIN_VERSION = "3.4.2";
   

}
