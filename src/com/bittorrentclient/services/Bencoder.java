package com.bittorrentclient.services;
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

@SuppressWarnings("rawtypes")
public class Bencoder
{
	// Used to determine the next bencoded input type in the file.
	private final int NULL_TYPE = 0;

	private final int STRING = 1;

	private final int INTEGER = 2;

	private final int LIST = 3;

	private final int DICTIONARY = 4;

	private final int STRUCTURE_END = 5;

	private class Index
	{
		public int index;

		public Index()
		{
			this.index = 0;
		}
	}

	private Index index;

	public Bencoder()
	{
		super();
		index = new Index();
	}

	public byte[] bencodeString(String input)
	{
		
		int length_of_string = input.length();
		int digits_of_string_length = 0;

		while (length_of_string > 0)
		{
			length_of_string /= 10;
			digits_of_string_length++;
		}

		// We need to store the length of the string, a ':', and the string
		// itself
		byte[] return_bytes = new byte[input.length() + digits_of_string_length
				+ 1];

		length_of_string = input.length();

		for (int i = digits_of_string_length + 1; i < return_bytes.length; i++)
		{
			return_bytes[i] = (byte) input.charAt(i - digits_of_string_length
					- 1);
		}

		return_bytes[digits_of_string_length] = (byte) ':';

		while (digits_of_string_length > 0)
		{
			return_bytes[digits_of_string_length - 1] = (byte) ((length_of_string % 10) + 48);
			length_of_string /= 10;
			digits_of_string_length--;
		}

		return return_bytes;
	}

	public String unbencodeString(byte[] input)
	{
		index.index = 0;
		return unbencodeString(input, index);
	}

	/**
	 * Parses a bencoded String located <code>input[index</code> and returns
	 * it as a String object.
	 * 
	 * @param input
	 *            Contains bencoded data.
	 * @param index
	 *            A valid index into <code>data</code> that points to the
	 *            beginning of a bencoded String.
	 * @return A String representing the bencoded String at
	 *         <code>data[index]</code>.
	 */
	public String unbencodeString(byte[] input, Index index)
	{
		String return_string = null;
		int temp_index = index.index;
		int power_of_ten = 1;
		int length_of_string = 0;
		boolean first_digit = false;
		StringBuffer temp_string = new StringBuffer();

		// Determine the length of the integer representing the String's length.
		while (input[temp_index] != (byte) ':')
		{
			if (first_digit)
			{
				power_of_ten *= 10;
			}
			first_digit = true;
			temp_index++;
		}

		// Determine the length of the string.
		while (input[index.index] != (byte) ':')
		{
			length_of_string += ((input[index.index] - 48) * power_of_ten);
			power_of_ten /= 10;
			index.index++;
			
		}

		// Skip the ':'
		index.index++;

		// Extract the string.
		while ((length_of_string > 0) && (index.index <= input.length))
		{
			temp_string.append((char) input[index.index]);

			length_of_string--;
			index.index++;
		}

		return_string = temp_string.toString();

		return return_string;
	}

	public byte[] bencodeInteger(Integer input)
	{
		byte[] return_bytes;

		if (input.intValue() == 0)
		{
			return_bytes = new byte[3];
			return_bytes[0] = (byte) 'i';
			return_bytes[1] = (byte) 48;
			return_bytes[2] = (byte) 'e';
		}

		else
		{
			int number_of_digits = 0;
			int input_value = input.intValue();
			boolean is_negative = false;
			if (input_value < 0)
			{
				is_negative = true;
				input_value = -input_value;
			}

			while (input_value > 0)
			{
				number_of_digits++;
				input_value /= 10;
			}

			input_value = input.intValue();

			if (is_negative)
			{
				input_value = -input_value;
			}

			if (is_negative)
			{
				return_bytes = new byte[number_of_digits + 3];
				return_bytes[1] = (byte) '-';
			}
			else
			{
				return_bytes = new byte[number_of_digits + 2];
			}
			return_bytes[0] = (byte) 'i';

			return_bytes[return_bytes.length - 1] = (byte) 'e';

			int stop_value = 0;
			if (is_negative)
			{
				stop_value = 1;
			}
			for (int i = return_bytes.length - 2; i > stop_value; i--)
			{
				return_bytes[i] = (byte) ((input_value % 10) + 48);
				input_value /= 10;
			}
		}
		return return_bytes;
	}

	public Integer unbencodeInteger(byte[] input)
	{
		index.index = 0;
		return unbencodeInteger(input, index);
	}

	/**
	 * Parses a bencoded Integer stored in <code>input</code> and returns it
	 * as an Integer object.
	 * 
	 * @param input
	 *            Contains bencoded data.
	 * @param index
	 *            A valid index into <code>input</code> that points to the
	 *            beginning of a bencoded Integer.
	 * @return An Integer representing the bencoded Integer in
	 *         <code>input</code>.
	 */
	public Integer unbencodeInteger(byte[] input, Index index)
	{
		Integer return_integer;
		int temp_value = 0;
		int power_of_ten = 1;
		boolean first_digit = false;
		boolean is_negative = false;

		// Skip the 'i'
		index.index++;

		if (input[index.index] == (byte) '-')
		{
			is_negative = true;
			index.index++;
		}

		int temp_index = index.index;

		// Determine the length of the integer representing the String's length.
		while (input[temp_index] != (byte) 'e')
		{
			if (first_digit)
			{
				power_of_ten *= 10;
			}
			first_digit = true;
			temp_index++;
		}

		// Determine the length of the string.
		while (input[index.index] != (byte) 'e')
		{
			temp_value += ((input[index.index] - 48) * power_of_ten);
			power_of_ten /= 10;
			index.index++;
		}

		// Skip the 'e'
		index.index++;

		if (is_negative)
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
	 * Bencodes a HashMap into a bencoded dictionary.The HashMap must contain
	 * only bencoded strings representing bencoded integers, strings, lists, and
	 * dictionaries.
	 * 
	 * @param input
	 *            A HashMap containing bencoded objects representing the entries
	 *            in a dictionary.
	 * @return
	 */
	public byte[] bencodeDictionary(HashMap<?, ?> input)
	{
		byte[] return_bytes = null;
		int length = 0;
		Vector<byte[]> dictionary_entries = new Vector<byte[]>();

		Iterator<?> input_iterator = input.entrySet().iterator();

		while (input_iterator.hasNext())
		{
			Map.Entry me = (Map.Entry) input_iterator.next();
			length += ((byte[]) me.getKey()).length;
			length += ((byte[]) me.getValue()).length;
			dictionary_entries.add((byte[]) me.getKey());
			dictionary_entries.add((byte[]) me.getValue());
		}
		return_bytes = new byte[length + 2];
		return_bytes[0] = (byte) 'd';
		return_bytes[return_bytes.length - 1] = (byte) 'e';

		int index = 1;

		for (int i = 0; i < dictionary_entries.size(); i++)
		{

			for (int j = 0; j < ((byte[]) dictionary_entries.get(i)).length; j++)
			{
				return_bytes[index] = ((byte[]) dictionary_entries.get(i))[j];
				index++;
			}
		}

		return return_bytes;
	}

	/**
	 * Parses a bencoded Dictionary represented by <code>input</code> and
	 * returns it as a Map object.
	 * 
	 * @param input
	 *            Contains bencoded input.
	 * @param index
	 *            A valid index into <code>input</code> that points to the
	 *            beginning of a bencoded Dictionary.
	 * @return A Map representing the bencoded Dictionary at <code>input</code>.
	 */
	public HashMap<String, Object> unbencodeDictionary(byte[] input)
	{
		index.index = 0;
		return unbencodeDictionary(input, index);
	}

	/**
	 * Parses a bencoded Dictionary located <code>input[index]</code> and
	 * returns it as a Map object.
	 * 
	 * @param input
	 *            Contains bencoded input.
	 * @param index
	 *            A valid index into <code>input</code> that points to the
	 *            beginning of a bencoded Dictionary.
	 * @return A Map representing the bencoded Dictionary at
	 *         <code>input[index]</code>.
	 */
	public HashMap<String, Object> unbencodeDictionary(byte[] input, Index index)
	{
		HashMap<String, Object> returned_map = new HashMap<String, Object>();
		String key;
		Object value;

		// Skip the 'd'
		index.index++;

		int next_data_type = getEncodedType(input, index.index);

		// As long as there isn't an error or the end of our dictionary, keep
		// parsing the entries.
		while ((next_data_type != NULL_TYPE)
				&& (next_data_type != STRUCTURE_END)
				&& (index.index < input.length))
		{
			// The key is ALWAYS a string.
			if (next_data_type != STRING)
			{
				System.err
						.println("Error: The bencoded object beginning at index.index "
								+ index.index
								+ " is not a String, but must be according to the BitTorrent definition.");
			}

			key = unbencodeString(input, index);

			// Now get the input type of the value
			next_data_type = getEncodedType(input, index.index);

			switch (next_data_type)
			{
				case INTEGER:
					value = unbencodeInteger(input, index);
					break;
				case STRING:
					value = unbencodeString(input, index);
					break;
				case LIST:
					value = unbencodeList(input, index);
					break;
				case DICTIONARY:
					value = unbencodeDictionary(input, index);
					break;
				default:
					System.err.println("Error: The value of the key \"" + key
							+ "\" is not a valid bencoded data type.");
					return null;
			}

			returned_map.put(key, value);
			// System.out.println("[" + key + "/" + value.toString() + "]");

			next_data_type = getEncodedType(input, index.index);
		}

		// Skip the 'e'
		index.index++;

		return returned_map;
	}

	/**
	 * Takes a vector of bencoded objects and returns a bencoded list containing
	 * the same data.
	 * 
	 * @param input
	 *            A Vector containing bencoded data.
	 * @return
	 */
	public byte[] bencodeList(Vector<?> input)
	{
		
		byte[] return_bytes = null;
		int length = 0;

		for (int i = 0; i < input.size(); i++)
		{
			length += ((byte[])input.get(i)).length;
		}
		
		return_bytes = new byte[length + 2];
		return_bytes[0] = (byte)'l';
		return_bytes[return_bytes.length - 1] = (byte)'e';
		
		int index = 1;
		
		for(int i = 0; i < input.size(); i++)
		{
			for(int j = 0; j < ((byte[])input.get(i)).length; j++)
			{
				return_bytes[index] = ((byte[])input.get(i))[j];
				index++;
			}
		}

		return return_bytes;
	}

	public Vector<Serializable> unbencodeList(byte[] input)
	{
		index.index = 0;
		return unbencodeList(input, index);
	}

	/**
	 * Parses a bencoded List stored in <code>input</code> and returns it as a
	 * List object.
	 * 
	 * @param input
	 *            Contains a bencoded List.
	 * @param index
	 *            A valid index into <code>input</code> that points to the
	 *            beginning of a bencoded List.
	 * @return A List representing the bencoded List in <code>input</code>.
	 */
	public Vector<Serializable> unbencodeList(byte[] input, Index index)
	{
		Vector<Serializable> return_list = new Vector<Serializable>();

		// Skip the 'l'
		index.index++;

		int next_data_type = getEncodedType(input, index.index);

		while ((next_data_type != STRUCTURE_END)
				&& (next_data_type != NULL_TYPE)
				&& (index.index < input.length))
		{
			switch (next_data_type)
			{
				case INTEGER:
					return_list.add(unbencodeInteger(input, index));
					break;
				case STRING:
					return_list.add(unbencodeString(input, index));
					break;
				case LIST:
					return_list.add(unbencodeList(input, index));
					break;
				case DICTIONARY:
					return_list.add(unbencodeDictionary(input, index));
					break;
				default:
					System.err.println("Error: The object at position "
							+ index.index
							+ " is not a valid bencoded data type.");
					return null;
			}
			next_data_type = getEncodedType(input, index.index);
		}

		// Skip the 'e'
		index.index++;

		return return_list;
	}

	/**
	 * Reads the byte at <code>input[index]</code> and returns an integer
	 * based on the value.
	 * 
	 * @param input
	 *            Contains bencoded input.
	 * @param index
	 *            A valid index into input that points to the beginning of a
	 *            bencoded String, Integer, List or Dictionary.
	 * @return An <code>int</code> based on the value of the byte at
	 *         <code>input[index]</code>.
	 */
	public int getEncodedType(byte[] input, int index)
	{
		// The value to be returned
		int return_value = NULL_TYPE;

		// Set return_value according to the byte at input[index]
		switch ((char) input[index])
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
						.println("Error: The byte at position "
								+ index
								+ " in the .torrent file is not the beginning of a bencoded data type.");
				break;
		}

		return return_value;
	}
}
