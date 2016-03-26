package com.bittorrentclient.model;

/*	Copyright 2005 Robert Sterling Moore II

This computer program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This computer program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this computer program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
//package main;

import java.util.Vector;

/**
 * A class used for storing the metadata of a .torrent file.
 * @author Robert S. Moore II
 */

@SuppressWarnings("rawtypes")
public class TorrentFile
{
	/**
	 * Stores the URL of the tracker as an HTTP-escaped String.
	 */
	public String tracker_url;
	
	/**
	 * Stores the SHA-1 hash of the bencoded 'info' dictionary as a 20-byte array.
	 */
	public byte[] info_hash_as_binary;
	
	/**
	 * Stores the SHA-1 hash of the bencoded 'info' dictionary as 40 hex digits in ASCII.
	 */
	public String info_hash_as_hex;
	
	/**
	 * Stores the SHA-1 hash of the bencoded 'info' dictionary as an HTTP-escaped string.
	 */
	public String info_hash_as_url;
		
	/**
	 * The number of bytes in the file (for a single-file .torrent).
	 */
	public int file_length;
	
	/**
	 * The size of each piece of the file as broken up by the tracker.
	 * The last piece of the file may be shorter if the file size (in bytes) is 
	 * not a multiple of this value.
	 */
	public int piece_length;
		
	/**
	 * The collection of SHA-1 hash values for each piece of the file stored as byte arrays.
	 */
	public Vector piece_hash_values_as_binary;
	
	/**
	 * The collection of SHA-1 hash values for each piece of the file stored as Strings of hexadecimal digits.
	 */
	public Vector piece_hash_values_as_hex;
	
	/**
	 * The collection of SHA-1 hash values for each piece of the file stored as Strings of HTTP-escaped characters.
	 */
	public Vector piece_hash_values_as_url;
		
	/**
	 * Creates a new TorrentFile object with empty fields.
	 *
	 */
	public TorrentFile()
	{
		super();
		tracker_url = new String();
		piece_hash_values_as_binary = new Vector();
		piece_hash_values_as_url = new Vector();
		piece_hash_values_as_hex = new Vector();
		info_hash_as_binary = new byte[20];
		info_hash_as_url = new String();
		info_hash_as_hex = new String();
		file_length = -1;
		piece_length = -1;
	}	
}
