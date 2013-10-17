package org.bsc.forge;

import org.apache.maven.project.MavenProject;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.forge.maven.MavenCoreFacet;
import org.jboss.forge.project.Project;
import org.jboss.forge.project.packaging.PackagingType;
import org.jboss.forge.resources.DirectoryResource;
import org.jboss.forge.test.AbstractShellTest;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;

public class ConfluenceForgePluginTest extends AbstractShellTest
{
   @Deployment
   public static JavaArchive getDeployment()
   {
      return AbstractShellTest.getDeployment()
            .addPackages(true, ConfluenceForgePlugin.class.getPackage());
   }

   @Test
   public void testSetup() throws Exception
   {
      final Project project = super.initializeProject(PackagingType.JAR);
      Assert.assertNotNull(project);
      Assert.assertThat( project.hasFacet( MavenCoreFacet.class), Is.is(true) );
      
      final MavenCoreFacet mavenFacet = project.getFacet(MavenCoreFacet.class);
      Assert.assertThat( mavenFacet, IsNull.notNullValue() );
      
      final MavenProject mProject = mavenFacet.getMavenProject();
      Assert.assertThat( mProject, IsNull.notNullValue() );
      
      resetInputQueue();
      queueInputLines("y");
      getShell().execute("confluence setup");

      Assert.assertTrue(getOutput().contains(ConfluenceForgePlugin.MESG_FOLDER_CREATED));

      DirectoryResource basedir = project.getProjectRoot();
      
      Assert.assertThat( basedir, IsNull.notNullValue() );
      
      final java.io.File sourceDir = new java.io.File(mProject.getBuild().getSourceDirectory());
      Assert.assertThat( sourceDir.exists(), Is.is(true) );

      final java.io.File siteDir = new java.io.File(sourceDir, "site/confluence");
      Assert.assertThat( siteDir.exists(), Is.is(true) );
   }
   
   @Test
   public void testSetupProvideCustomFolder() throws Exception
   {
      final Project project = super.initializeJavaProject();

      Assert.assertNotNull(project);
      
      resetInputQueue();
      queueInputLines( "n", "confluenceSite", "y");
      getShell().execute("confluence setup");

      
      Assert.assertTrue(getOutput().contains(ConfluenceForgePlugin.MESG_FOLDER_CREATED));
      
      DirectoryResource basedir = project.getProjectRoot();
      
      Assert.assertThat( basedir, IsNull.notNullValue() );
      Assert.assertThat( basedir.getChildDirectory("confluenceSite").exists(), Is.is(true) );
   }
   
   @Test
   public void testSetupInterrupted() throws Exception
   {
      final Project project = super.initializeJavaProject();

      Assert.assertNotNull(project);
      
      resetInputQueue();
      queueInputLines( "n", "", "y");
      getShell().execute("confluence setup");

      
      Assert.assertTrue(getOutput().contains(ConfluenceForgePlugin.MSG_SETUP_INTERRUPTED));
   }
}
