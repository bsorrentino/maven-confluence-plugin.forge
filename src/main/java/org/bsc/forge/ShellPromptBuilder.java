package org.bsc.forge;

import org.jboss.forge.shell.Shell;
import org.jboss.forge.shell.ShellPrompt;
import org.jboss.forge.shell.exceptions.AbortedException;

public abstract class ShellPromptBuilder<V> {

	private String message;
	private String interruptMessage = "Operation Interrupted!";
	private V defaultValue;
	private F<V,V> transformer;
	private F<Object,V> getter;
	
	public ShellPromptBuilder<V> setMessage(String message) {
		this.message = message;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public ShellPromptBuilder<V> setDefaultValue(V defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}

	public ShellPromptBuilder<V> setTransformer(F<V, V> transformer) {
		this.transformer = transformer;
		return this;
	}
	
	public ShellPromptBuilder<V> setInterruptMessage(String interruptMessage) {
		this.interruptMessage = interruptMessage;
		return this;
	}

	@SuppressWarnings("unchecked")
	public <E> ShellPromptBuilder<V> setGetter(F<E, V> getter) {
		this.getter = (F<Object, V>) getter;
		return this;
	}

	public boolean isValid( V value ) {
		return null!=value;
	}
	
	protected void reset() {
		setTransformer(null)
		.setDefaultValue(null)
		.setMessage(null)
		.setGetter(null)
		;
		
	}
	
	protected abstract V prompt( ShellPrompt shell, String msg );
	
	protected abstract V prompt( ShellPrompt shell, String msg, V _default );
	
    public final <E> V input( Shell shell, E element  )   
    {
    	
    	V _default = null;
    	
    	if( getter!= null ) {
    		_default = getter.f(element);
    	}

    	if( !isValid(_default) ) {
			_default = defaultValue;
		}
    	
        V value = (isValid(_default)) ? 
        		prompt( shell, String.format("%s. Press enter for default '%s': ", message, String.valueOf(_default)), _default ) :
        		prompt( shell, String.format("%s. Press enter to abort: ", message));
        		        				;

        if (transformer != null) {
            value = transformer.f(value);
        }

        if (!isValid(value)) {
            shell.println(interruptMessage);
            throw new AbortedException(interruptMessage);
        }

        reset();
        return value;
    }


}
