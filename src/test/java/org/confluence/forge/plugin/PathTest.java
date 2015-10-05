package org.confluence.forge.plugin;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;

public class PathTest
{
    
    @Test
    public void resolvePath() {
        
        
        final Path p = Paths.get("/Volumes/SSD/Users/softphone/WORKSPACES/GITHUB/MAVEN/maven-confluence-plugin/maven-confluence-core/src/test/resources/TEST1.md");
        
        final Path resolved = p.resolveSibling("TEST1.confluence");
        
        final Path fileName = p.getFileName();
        
        final String fileNameWithOutExt = fileName.toString().replaceFirst("[.][^.]+$", "");
        
        Assert.assertThat( fileNameWithOutExt, IsEqual.equalTo("TEST1"));
        
        
        System.out.printf( "fileNameCount=[%d], fileNane=[%s] - resoved path=[%s]\n", fileName.getNameCount(),p.getFileName(), resolved);
        Assert.assertThat( resolved.endsWith("TEST1.confluence"), Is.is(true));
        
        
    }
}