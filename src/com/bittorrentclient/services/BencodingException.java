package com.bittorrentclient.services;

/**
 * Custom exception for Bencoding.
 * 
 * @author Isaac Yochelson, Robert Schomburg and Fernando Geraci
 *
 */

@SuppressWarnings("serial")
public class BencodingException extends Exception {
	
	/**
	 * Constructs a new BencodignException.
	 * @param error the message of the error being thrown
	 */
	public BencodingException(String error) {
		super(error);
	}

}