package com.gmail.emertens.PdxExplorers;

/**
 * This exception is used when a user attempts to perform an action
 * without proper permissions.
 * 
 * @author Eric Mertens
 */
public class ExplorersPermissionException extends ExplorersException {
	private static final long serialVersionUID = 923923728061734630L;

	public ExplorersPermissionException() {
		super("You do not have permission");
	}
}
