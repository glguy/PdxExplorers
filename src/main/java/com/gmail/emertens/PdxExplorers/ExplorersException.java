package com.gmail.emertens.PdxExplorers;

/**
 * The class ExplorersException is the base class for exceptions
 * thrown by the PdxExplorers plugin.
 * 
 * @author Eric Mertens
 */
public class ExplorersException extends Exception {
	
	private static final long serialVersionUID = -8542744148905758853L;

	public ExplorersException() { }
	public ExplorersException(String msg) { super(msg); }
}
