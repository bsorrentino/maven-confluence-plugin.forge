package org.bsc.forge;

import org.jboss.forge.shell.ShellPrompt;

public class ShellPromptStringBuilder extends ShellPromptBuilder<String> {

	@Override
	protected String prompt(ShellPrompt shell, String msg) {
		return shell.prompt(msg);
	}

	@Override
	protected String prompt(ShellPrompt shell, String msg, String _default) {
		return shell.prompt(msg, _default);
	}

	@Override
	public final boolean isValid(String value) {
		return value != null && !value.isEmpty();
	}

}
