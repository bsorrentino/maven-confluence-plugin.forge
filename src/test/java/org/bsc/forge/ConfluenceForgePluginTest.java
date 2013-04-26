package org.bsc.forge;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.forge.test.AbstractShellTest;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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
   public void testDefaultCommand() throws Exception
   {
      getShell().execute("confluence");
   }

   @Test
   public void testCommand() throws Exception
   {
      getShell().execute("confluence command");
   }

   @Test
   public void testPrompt() throws Exception
   {
      queueInputLines("y");
      getShell().execute("confluence prompt foo bar");
   }
}
