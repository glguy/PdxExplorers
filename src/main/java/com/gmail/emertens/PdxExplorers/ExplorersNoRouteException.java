package com.gmail.emertens.PdxExplorers;

/**
 * This exception is thrown when the plugin attempts to use a
 * non-existent route.
 * 
 * @author Eric Mertens
 */
public class ExplorersNoRouteException extends ExplorersException {

	private static final long serialVersionUID = -3165500227164658272L;

	public ExplorersNoRouteException() { super("No such route"); }
}
