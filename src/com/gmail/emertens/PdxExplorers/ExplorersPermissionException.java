package com.gmail.emertens.PdxExplorers;

public class ExplorersPermissionException extends ExplorersException {
	private static final long serialVersionUID = 923923728061734630L;

	public ExplorersPermissionException() {
		super("You do not have permission");
	}
}
