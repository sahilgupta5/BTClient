package com.bittorrentclient.model;

/*
 * Copyright 2006 Robert Sterling Moore II
 * 
 * This computer program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This computer program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this computer program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.bittorrentclient.services.Bencoder;

/**
 * Unencodes a .torrent file and stores its data in a TorrentFile object.
 * 
 * @author Robert S. Moore II
 */
public class TorrentFileHandler
{
	/*
	 * This class is used because Java passes by value for primitive data types,
	 * but passes by reference for Objects. Passing this object instead of a
	 * simple int allows methods to update the value of the index without using
	 * global variables.
	 */
	private class Index
	{
		public int index;

		public Index()
		{
			super();
			this.index = 0;
		}
	}
	
	// Used to determine the next bencoded data type in the file.
	private final int NULL_TYPE = 0;

	private final int STRING = 1;

	private final int INTEGER = 2;

	private final int LIST = 3;

	private final int DICTIONARY = 4;

	private final int STRUCTURE_END = 5;

	// Stores the unencoded data.
	private TorrentFile torrent_file;
	
	//Unbencodes data
	private Bencoder bencoder;

	/**
	 * Constructs a new TorrentFileHandler object
	 * 
	 */
	public TorrentFileHandler()
	{
		super();
		this.torrent_file = new TorrentFile();
		this.setBencoder(new Bencoder());
	}

	public TorrentFile openTorrentFile(String file_name)
	{
		byte[] file_data = getBytesFromFile(file_name);
		HashMap<String, Object> file_data_map;
		Index index = new Index();

		file_data_map = parseDictionary(file_data, index);
		
		if(!storeDataInTorrent(file_data_map))
		{
			return null;
		}
		
		return this.torrent_file;
	}

	@SuppressWarnings("resource")
	private byte[] getBytesFromFile(String file_name)
	{
		File file = new File(file_name);
		long file_size_long = -1;
		byte[] file_bytes = null;
		InputStream file_stream;

		try
		{
			file_stream = new FileInputStream(file);

			// Verify that the file exists
			if (!file.exists())
			{
				System.err
						.println("Error: [TorrentFileHandler.java] The file \""
								+ file_name
								+ "\" does not exist. Please make sure you have the correct path to the file.");
			}

			// Verify that the file is readable
			if (!file.canRead())
			{
				System.err
						.println("Error: [TorrentFileHandler.java] Cannot read from \""
								+ file_name
								+ "\". Please make sure the file permissions are set correctly.");
			}

			// The following code was derived from
			// http://javaalmanac.com/egs/java.io/File2ByteArray.html
			file_size_long = file.length();

			// Avoid overflow in the file length
			if (file_size_long > Integer.MAX_VALUE)
			{
				System.err.println("Error: [TorrentFileHandler.java] The file \"" + file_name
						+ "\" is too large to be read by this class.");
			}

			// Initialize the byte array for the file's data
			file_bytes = new byte[(int) file_size_long];

			int file_offset = 0;
			int bytes_read = 0;

			// Read from the file
			while (file_offset < file_bytes.length
					&& (bytes_read = file_stream.read(file_bytes, file_offset,
							file_bytes.length - file_offset)) >= 0)
			{
				file_offset += bytes_read;
			}

			// Verify that we read everything from the file
			if (file_offset < file_bytes.length)
			{
				throw new IOException("Could not completely read file \""
						+ file.getName() + "\".");
			}

			// End of code from
			// http://javaalmanac.com/egs/java.io/File2ByteArray.html

			file_stream.close();

		}
		catch (FileNotFoundException e)
		{
			System.err
					.println("Error: [TorrentFileHandler.java] The file \""
							+ file_name
							+ "\" does not exist. Please make sure you have the correct path to the file.");
			return null;
		}
		catch (IOException e)
		{
			System.err
					.println("Error: [TorrentFileHandler.java] There was a general, unrecoverable I/O error while reading from \""
							+ file_name + "\".");
			System.err.println(e.getMessage());
		}

		return file_bytes;
	}

	/**
	 * Reads the byte at <code>data[index.index]</code> and returns an integer
	 * based on the value.
	 * 
	 * @param data
	 *            Contains bencoded data.
	 * @param index
	 *            A valid index into data that points to the beginning of a
	 *            bencoded String, Integer, List or Dictionary.
	 * @return An <code>int</code> based on the value of the byte at
	 *         <code>data[index.index]</code>.
	 */
	private int getEncodedType(byte[] data, Index index)
	{
		// The value to be returned
		int return_value = NULL_TYPE;

		// Set return_value according to the byte at data[index.index]
		switch ((char) data[index.index])
		{
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				return_value = STRING;
				break;
			case 'i':
				return_value = INTEGER;
				break;
			case 'l':
				return_value = LIST;
				break;
			case 'd':
				return_value = DICTIONARY;
				break;
			case 'e':
				return_value = STRUCTURE_END;
				break;
			default:
				System.err
						.println("Error: [TorrentFileHandler.java] The byte at position "
								+ index.index
								+ " in the .torrent file is not the beginning of a bencoded data type.");
				break;
		}

		return return_value;
	}

	/**
	 * Parses a bencoded String located at <code>data[index.index]</code> and
	 * returns it as a String object. After being called,
	 * <code>index.index</code> points to the byte after the end of the String
	 * (the next data structure).
	 * 
	 * @param data
	 *            Contains bencoded data.
	 * @param index
	 *            A valid index into <code>data</code> that points to the
	 *            beginning of a bencoded String.
	 * @return A String representing the bencoded String at
	 *         <code>data[index.index]</code>.
	 */
	private String parseString(byte[] data, Index index)
	{
		String return_string = null;
		int temp_index = index.index;
		int power_of_ten = 1;
		int length_of_string = 0;
		boolean first_digit = false;
		StringBuffer temp_string = new StringBuffer();

		// Determine the length of the integer representing the String's length.
		while (data[temp_index] != (byte) ':')
		{
			if (first_digit)
			{
				power_of_ten *= 10;
			}
			first_digit = true;
			temp_index++;
		}

		// Determine the length of the string.
		while (data[index.index] != (byte) ':')
		{
			length_of_string += ((data[index.index] - 48) * power_of_ten);
			power_of_ten /= 10;
			index.index++;
		}

		// Skip the ':'
		index.index++;

		// Extract the string.
		while ((length_of_string > 0) && (index.index <= data.length))
		{
			temp_string.append((char) data[index.index]);

			length_of_string--;
			index.index++;
		}

		return_string = temp_string.toString();
		
		return return_string;
	}

	/**
	 * Parses a bencoded Integer located at <code>data[index.index]</code> and
	 * returns it as an Integer object. After being called,
	 * <code>index.index</code> points to the byte after the end of the
	 * Integer (the next data structure).
	 * 
	 * @param data
	 *            Contains bencoded data.
	 * @param index
	 *            A valid index into <code>data</code> that points to the
	 *            beginning of a bencoded Integer.
	 * @return An Integer representing the bencoded Integer at
	 *         <code>data[index.index]</code>.
	 */
	private Integer parseInteger(byte[] data, Index index)
	{
		Integer return_integer;
		int temp_value = 0;
		int power_of_ten = 1;
		boolean first_digit = false;
		boolean is_negative = false;

		// Skip the 'i'
		index.index++;
		
		if(data[index.index] == (byte)'-')
		{
			is_negative = true;
			index.index++;
		}
		int temp_index = index.index;

		// Determine the length of the integer representing the String's length.
		while (data[temp_index] != (byte) 'e')
		{
			if (first_digit)
			{
				power_of_ten *= 10;
			}
			first_digit = true;
			temp_index++;
		}

		// Determine the length of the string.
		while (data[index.index] != (byte) 'e')
		{
			temp_value += ((data[index.index] - 48) * power_of_ten);
			power_of_ten /= 10;
			index.index++;
		}

		// Skip the 'e'
		index.index++;

		if(is_negative)
		{
			return_integer = new Integer(-temp_value);
		}
		else
		{
			return_integer = new Integer(temp_value);
		}
		
		return return_integer;
	}

	/**
	 * Parses a bencoded List located <code>data[index.index]</code> and
	 * returns it as a List object. After being called, <code>index.index</code>
	 * points to the byte after the end of the List (the next data structure).
	 * 
	 * @param data
	 *            Contains bencoded data.
	 * @param index
	 *            A valid index into <code>data</code> that points to the
	 *            beginning of a bencoded List.
	 * @return A List representing the bencoded List at
	 *         <code>data[index.index]</code>.
	 */
	private Vector<Serializable> parseList(byte[] data, Index index)
	{
		Vector<Serializable> return_list = new Vector<Serializable>();

		// Skip the 'l'
		index.index++;

		int next_data_type = getEncodedType(data, index);

		while ((next_data_type != STRUCTURE_END)
				&& (next_data_type != NULL_TYPE) && (index.index < data.length))
		{
			switch(next_data_type)
			{
				case INTEGER:
					return_list.add(parseInteger(data, index));
					break;
				case STRING:
					return_list.add(parseString(data, index));
					break;
				case LIST:
					return_list.add(parseList(data, index));
					break;
				case DICTIONARY:
					return_list.add(parseDictionary(data, index));
					break;
				default:
					System.err.println("Error: [TorrentFileHandler.java] The object at position " + index.index
							+ " is not a valid bencoded data type.");
					return null;
			}
			next_data_type = getEncodedType(data, index);
		}

		//Skip the 'e'
		index.index++;
		
		return return_list;
	}

	/**
	 * Parses a bencoded Dictionary located <code>data[index.index]</code> and
	 * returns it as a Map object. After being called, <code>index.index</code>
	 * points to the byte after the end of the Dictionary (the next data
	 * structure).
	 * 
	 * @param data
	 *            Contains bencoded data.
	 * @param index
	 *            A valid index into <code>data</code> that points to the
	 *            beginning of a bencoded Dictionary.
	 * @return A Map representing the bencoded Dictionary at
	 *         <code>data[index.index]</code>.
	 */
	private HashMap<String, Object> parseDictionary(byte[] data, Index index)
	{
		HashMap<String, Object> returned_map = new HashMap<String, Object>(10);
		String key;
		Object value;

		// Skip the 'd'
		index.index++;

		int next_data_type = getEncodedType(data, index);

		// As long as there isn't an error or the end of our dictionary, keep
		// parsing the entries.
		while ((next_data_type != NULL_TYPE)
				&& (next_data_type != STRUCTURE_END)
				&& (index.index < data.length))
		{
			// The key is ALWAYS a string.
			if (next_data_type != STRING)
			{
				System.err
						.println("Error: [TorrentFileHandler.java] The bencoded object beginning at index "
								+ index.index
								+ " is not a String, but must be according to the BitTorrent definition.");
			}

			key = parseString(data, index);

			// Now get the data type of the value
			next_data_type = getEncodedType(data, index);

			switch (next_data_type)
			{
				case INTEGER:
					value = parseInteger(data, index);
					break;
				case STRING:
					value = parseString(data, index);
					break;
				case LIST:
					value = parseList(data, index);
					break;
				case DICTIONARY:
					if(key.equalsIgnoreCase("info"))
					{
						int old_index = index.index;
						value = parseDictionary(data, index);
						byte[] info = new byte[index.index-old_index];
					
						for(int i = 0; i < info.length; i++)
						{
							info[i] = data[old_index + i];
						}
						torrent_file.info_hash_as_binary = generateSHA1Hash(info);
						torrent_file.info_hash_as_url = byteArrayToURLString(torrent_file.info_hash_as_binary);
						torrent_file.info_hash_as_hex = byteArrayToByteString(torrent_file.info_hash_as_binary);
					}
					else
					{
						value = parseDictionary(data, index);
					}
					break;
				default:
					System.err.println("Error: [TorrentFileHandler.java] The value of the key \"" + key
							+ "\" is not a valid bencoded data type.");
					return null;
			}

			returned_map.put(key, value);
			//System.out.println("[" + key + "/" + value.toString() + "]");
			
			next_data_type = getEncodedType(data, index);
		}
		
		//Skip the 'e'
		index.index++;
		
		return returned_map;
	}
	
	private boolean storeDataInTorrent(Map<String, Object> torrent_data_map)
	{
		Map<?, ?> info_map = (Map<?, ?>)torrent_data_map.get("info");
		if(info_map == null)
		{
			System.err.println("Error: [TorrentFileHandler.java] Could not retrieve the info dictionary.");
			return false;
		}
		if(!getPieceHashes((String)info_map.get("pieces")))
		{
			return false;
		}
		
		torrent_file.tracker_url = (String)torrent_data_map.get("announce");
		if(torrent_file.tracker_url == null)
		{
			System.err.println("Error: [TorrentFileHandler.java] Could not retrieve the tracker URL.");
			return false;
		}
		
		torrent_file.file_length = ((Integer)info_map.get("length")).intValue();
		if(torrent_file.file_length < 0)
		{
			System.err.println("Error: [TorrentFileHandler.java] Could not retrieve the file length.");
			return false;
		}
		
		torrent_file.piece_length = ((Integer)info_map.get("piece length")).intValue();
		if(torrent_file.piece_length < 0)
		{
			System.err.println("Error: [TorrentFileHandler.java] Could not retrieve the piece length.");
			return false;
		}
		
		return true;
		
	}
	
	@SuppressWarnings("unchecked")
	private boolean getPieceHashes(String hash_string)
	{
		if(hash_string.length() % 20 != 0)
		{
			System.err.println("Error: [TorrentFileHandler.java] The SHA-1 hash for the file's pieces is not the correct length.");
			return false;
		}
		
		byte[] binary_data = new byte[hash_string.length()];
		byte[] individual_hash;
		int number_of_pieces = binary_data.length / 20;
		
		for(int i = 0; i < binary_data.length; i++)
		{
			binary_data[i] = (byte)hash_string.charAt(i);
		}
		
		for(int i = 0; i < number_of_pieces; i++)
		{
			individual_hash = new byte[20];
			for(int j = 0; j < 20; j++)
			{
				individual_hash[j] = binary_data[(20*i)+j];
			}
			torrent_file.piece_hash_values_as_binary.add(individual_hash);
			torrent_file.piece_hash_values_as_hex.add(byteArrayToByteString(individual_hash));
			torrent_file.piece_hash_values_as_url.add(byteArrayToURLString(individual_hash));
		}
		
		return true;
	}

	/*
	 * Stolen from byteArrayToByteString
	 */
	private static String byteArrayToURLString(byte in[])
	{
        byte ch = 0x00;
        int i = 0;
        if (in == null || in.length <= 0) {
            return null;
        }
        String pseudo[] = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "A", "B", "C", "D", "E", "F"
        };
        StringBuffer out = new StringBuffer(in.length * 2);

        while (i < in.length) {
            // First check to see if we need ASCII or HEX
            if ((in[i] >= '0' && in[i] <= '9') || (in[i] >= 'a' && in[i] <= 'z') || (in[i] >= 'A' && in[i] <= 'Z') || in[i] == '-' || in[i] == '_' || in[i] == '.' || in[i] == '~') {
                out.append((char) in[i]);
                i++;
            } else {
                out.append('%');
                ch = (byte) (in[i] & 0xF0); // Strip off high nibble

                ch = (byte) (ch >>> 4); // shift the bits down

                ch = (byte) (ch & 0x0F); // must do this is high order bit is
                // on!

                out.append(pseudo[(int) ch]); // convert the nibble to a
                // String Character

                ch = (byte) (in[i] & 0x0F); // Strip off low nibble

                out.append(pseudo[(int) ch]); // convert the nibble to a
                // String Character

                i++;
            }
        }

        String rslt = new String(out);

        return rslt;
	}

	/**
	 * 
	 * Convert a byte[] array to readable string format. This makes the "hex"
	 * readable!
	 * 
	 * @author Jeff Boyle
	 * 
	 * @return result String buffer in String format
	 * 
	 * @param in
	 *            byte[] buffer to convert to string format
	 * 
	 */
	// Taken from http://www.devx.com/tips/Tip/13540
	private static String byteArrayToByteString(byte in[])
	{
        byte ch = 0x00;
        int i = 0;
        if (in == null || in.length <= 0) {
            return null;
        }
        String pseudo[] = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "A", "B", "C", "D", "E", "F"
        };
        StringBuffer out = new StringBuffer(in.length * 2);

        while (i < in.length) {
            ch = (byte) (in[i] & 0xF0); // Strip off high nibble

            ch = (byte) (ch >>> 4); // shift the bits down

            ch = (byte) (ch & 0x0F); // must do this is high order bit is on!

            out.append(pseudo[(int) ch]); // convert the nibble to a String
            // Character

            ch = (byte) (in[i] & 0x0F); // Strip off low nibble

            out.append(pseudo[(int) ch]); // convert the nibble to a String
            // Character

            i++;
        }

        String rslt = new String(out);

        return rslt;
	}

	private byte[] generateSHA1Hash(byte[] bytes)
	{
		try
		{
			byte[] hash = new byte[20];
			MessageDigest sha = MessageDigest.getInstance("SHA-1");
			hash = sha.digest(bytes);

			return hash;
		}
		catch (NoSuchAlgorithmException e)
		{
			System.err
					.println("Error: [TorrentFileHandler.java] \"SHA-1\" is not a valid algorithm name.");
			System.exit(1);
		}
		return null;
	}

	public Bencoder getBencoder() {
		return bencoder;
	}

	public void setBencoder(Bencoder bencoder) {
		this.bencoder = bencoder;
	}
}
