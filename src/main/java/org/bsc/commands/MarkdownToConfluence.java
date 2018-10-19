package org.bsc.commands;

import static java.lang.String.format;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bsc.markdown.ToConfluenceSerializer;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.ui.command.AbstractUICommand;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.hints.InputType;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.output.UIOutput;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.pegdown.PegDownProcessor;
import org.pegdown.ast.Node;
import org.pegdown.ast.RootNode;

public class MarkdownToConfluence extends AbstractUICommand {

    @Inject
    @WithAttributes(label = "source", required = true, type = InputType.FILE_PICKER)
    private UIInput<FileResource<?>> source;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(MarkdownToConfluence.class)
                .name("confluence-from-md")
                .category(Categories.create("Confluence"));
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
    
        builder.add(source);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        final UIOutput output = context.getUIContext().getProvider().getOutput();
        
        final FileResource<?> sourceFR = source.getValue();
        
        final Path path = Paths.get(sourceFR.getFullyQualifiedName());
        
        final String fileNameWithOutExt = path.getFileName().toString().replaceFirst("[.][^.]+$", "");

        final ToConfluenceSerializer ser = new ToConfluenceSerializer() {

            @Override
            protected void notImplementedYet(Node node) {
                output.err().printf("Node [%s] not supported yet.", node.getClass().getSimpleName());
            }

        };
        
        final PegDownProcessor p = new PegDownProcessor(ToConfluenceSerializer.extensions());
        
        final char[] contents = IOUtils.toCharArray(sourceFR.getResourceInputStream());
        
        final RootNode root = p.parseMarkdown( contents );

        root.accept( ser );
        
        final Path resolved = path.resolveSibling( fileNameWithOutExt.concat(".confluence"));

        FileUtils.writeStringToFile( resolved.toFile(), ser.toString());
        
        return Results
                .success( format("file [%s] succesfully created!", resolved.getFileName()));
    }
}
