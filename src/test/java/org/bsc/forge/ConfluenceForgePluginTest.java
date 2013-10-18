package org.bsc.forge;

import java.io.IOException;
import org.apache.maven.project.MavenProject;
import static org.bsc.forge.ConfluenceForgePlugin.PLUGIN_KEY_3;
import org.codehaus.plexus.util.FileUtils;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNull;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.forge.maven.MavenCoreFacet;
import org.jboss.forge.maven.MavenPluginFacet;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.dependencies.DependencyBuilder;
import org.jboss.forge.project.packaging.PackagingType;
import org.jboss.forge.resources.DirectoryResource;
import org.jboss.forge.test.AbstractShellTest;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ConfluenceForgePluginTest extends AbstractShellTest
{
   @Deployment
   public static JavaArchive getDeployment()
   {
      return AbstractShellTest.getDeployment()
            .addPackages(true, ConfluenceForgePlugin.class.getPackage());
   }

   Project project;
   
   @Before
   public void init() throws Exception {

      project = super.initializeProject(PackagingType.JAR);

      Assert.assertNotNull(project);
      Assert.assertThat( project.hasFacet( MavenCoreFacet.class), Is.is(true) );
      Assert.assertThat( project.hasFacet( MavenPluginFacet.class), Is.is(true) );
      
      System.out.printf( "Project basedir [%s]\n", project.getProjectRoot().getFullyQualifiedName());
   }
   
   private void queueInputCfg( 
                            String serverId, 
                            String endPoint,
                            String spaceKey,
                            String parentPageTitle) throws IOException 
   {
      // 1) myServerId --> server id
      // 2)            --> set default endpoint
      // 3) spaceKey   --> set spacekey
      // 4)            --> set default parent page title
      queueInputLines( 
                        serverId, 
                        endPoint, 
                        spaceKey, 
                        parentPageTitle);
       
   }
   
   @Test
   public void testSetup() throws Exception
   {
      
      final MavenCoreFacet mavenFacet = project.getFacet(MavenCoreFacet.class);
      Assert.assertThat( mavenFacet, IsNull.notNullValue() );
      
      final MavenProject mProject = mavenFacet.getMavenProject();
      Assert.assertThat( mProject, IsNull.notNullValue() );
  
      resetInputQueue();
      queueInputLines("y");
      queueInputCfg("myServerId", "http://localhost:8080/", "mySpaceKey", "");
      
      getShell().execute("confluence setup");

      Assert.assertTrue(getOutput().contains(ConfluenceForgePlugin.MESG_FOLDER_CREATED));

      final java.util.Properties pp = mavenFacet.getPOM().getProperties();
      
      Assert.assertThat( pp, IsNull.notNullValue());
      Assert.assertThat( pp.isEmpty(), Is.is(false));
      Assert.assertThat( pp.containsKey("confluence.home"), Is.is(true));
      Assert.assertThat( pp.getProperty("confluence.home"), IsEqual.equalTo("http://localhost:8080"));
      
      DirectoryResource basedir = project.getProjectRoot();
      
      Assert.assertThat( basedir, IsNull.notNullValue() );
      
      final java.io.File sourceDir = new java.io.File(project.getProjectRoot().getFullyQualifiedName());

      final java.io.File siteDir = new java.io.File(sourceDir, "src/site/confluence");
      Assert.assertThat( siteDir.exists(), Is.is(true) );

      final java.io.File tmpDir = new java.io.File("/tmp/test");
      if( !tmpDir.exists() ) {
          Assert.assertThat(tmpDir.mkdirs(), Is.is(true));
      }
      
      FileUtils.copyDirectory(mProject.getBasedir(), tmpDir );
   }
   
   @Test
   public void testSetupProvideCustomFolder() throws Exception
   {
      resetInputQueue();
      queueInputLines( "n", "confluenceSite", "y");
      queueInputCfg("myServerId", "", "mySpaceKey", "");
      getShell().execute("confluence setup");

      
      Assert.assertTrue(getOutput().contains(ConfluenceForgePlugin.MESG_FOLDER_CREATED));
      
      DirectoryResource basedir = project.getProjectRoot();
      
      Assert.assertThat( basedir, IsNull.notNullValue() );
      Assert.assertThat( basedir.getChildDirectory("confluenceSite").exists(), Is.is(true) );
      
      
   }
   
   @Test
   public void testSetupInterrupted() throws Exception
   {
      
      resetInputQueue();
      queueInputLines( "n", "", "y");
      getShell().execute("confluence setup");

      
      Assert.assertTrue(getOutput().contains(ConfluenceForgePlugin.MSG_SETUP_INTERRUPTED));
   }
   
   @Test
   public void testGetConfluencePlugin() throws Exception {
       
       final DependencyBuilder confluencePluginDep = 
          DependencyBuilder.create(PLUGIN_KEY_3 );

       Assert.assertThat( confluencePluginDep, IsNull.notNullValue() );

       final MavenPluginFacet pluginFacet = 
               project.getFacet(MavenPluginFacet.class);
       
       pluginFacet.addPluginRepository(MavenPluginFacet.KnownRepository.CENTRAL);
       
       Assert.assertThat( pluginFacet, IsNull.notNullValue() );
       Assert.assertThat( pluginFacet.hasPlugin(confluencePluginDep), Is.is(false) );
       

   }
}
