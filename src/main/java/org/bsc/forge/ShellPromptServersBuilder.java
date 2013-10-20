package org.bsc.forge;

import java.util.Collections;

import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.jboss.forge.shell.ShellPrompt;

public class ShellPromptServersBuilder extends ShellPromptBuilder<String> {

	final java.util.List<String> serverIdList ;
	
	public ShellPromptServersBuilder( Settings settings ) {
		final java.util.List<Server> serverList = settings.getServers();
		
		
		serverIdList = new java.util.ArrayList<String>(serverList.size());
		for( Server s : serverList ){
			
			serverIdList.add( s.getId() );
		}
		
		Collections.sort(serverIdList);
	}

	@Override
	protected String prompt(ShellPrompt shell, String msg) {
		msg = String.format("%s. Press '0' to abort': ", getMessage() );
		
		int index =  shell.promptChoice(msg, serverIdList);
		
		return (index >= 0 ) ? serverIdList.get(index) : null;
	}

	@Override
	protected String prompt(ShellPrompt shell, String msg, String _default) {

		msg = String.format("%s. Press '0' for default '%s': ", getMessage(), _default);
		
	   int index =  shell.promptChoice(msg, serverIdList);
		
		return (index >= 0 ) ? serverIdList.get(index) : _default;
	}

}
