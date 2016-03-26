package com.bittorrentclient.services;

/*

 * Copyright 2006 Robert Sterling Moore II This computer program is free
 * software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version. This
 * computer program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this computer program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

import java.util.BitSet;

/**
 * @author Robert S. Moore II
 */
public class ToolKit
{
	public static byte[] intToBigEndianBytes(int int_value, byte[] bytes,
			int offset)
	{
		bytes[3 + offset] = (byte) int_value;
		int_value >>>= 8;
		bytes[2 + offset] = (byte) int_value;
		int_value >>>= 8;
		bytes[1 + offset] = (byte) int_value;
		int_value >>>= 8;
		bytes[0 + offset] = (byte) int_value;
		return bytes;
	}

	public static int bigEndianBytesToInt(byte[] bytes, int i)
	{
		return ((bytes[0 + i] & 0xFF) << 24) + ((bytes[1 + i] & 0xFF) << 16)
				+ ((bytes[2 + i] & 0xFF) << 8) + (bytes[3 + i] & 0xFF);
	}

	/**
	 * Converts a byte[] into a BitSet. 
	 * @author Chris Lauderdale
	 * @param bytes
	 *            A <code>byte[]</code> representing the bits in <code>bytes</code>.
	 * @return A BitSet object representing the input byte[].
	 */
	public static BitSet bytesToBitSet(byte[] bytes)
	{
		final BitSet rv=new BitSet();
		bytesToBitSet(bytes,rv);
		return rv;
	}
	
	/**
	 * Converts a byte[] into a BitSet. 
	 * @author Chris Lauderdale
	 * @param bytes
	 *            A <code>byte[]</code> representing the bits in <code>bytes</code>.
	 * @param bits
	 *            The {@link BitSet} to fill.
	 */
	public static void bytesToBitSet(byte[] bytes,final BitSet bits)
	{
		int i, j, k;
		bits.clear();
		for (i = k = 0; i < bytes.length; i++)
			for (j = 0x80; j != 0; j >>>= 1, k++)
				if ((bytes[i] & j) != 0)
					bits.set(k);
	}

	/**
	 * Converts a <code>BitSet</code> into a <code>byte[]</code>.
	 * @author Chris Lauderdale
	 * @param bits A {@link BitSet}.
	 * @param bytes The <code>byte</code> array to fill.
	 */
	public static void bitSetToBytes(BitSet bits, byte[] bytes)
	{
		int i;
		java.util.Arrays.fill(bytes,(byte)0);
		for(i=bits.nextSetBit(0);i>=0;i=bits.nextSetBit(i+1))
			bytes[i>>>3]|=0x80>>>(i&7);
	}
	
	/**
	 * Converts a <code>BitSet</code> into a <code>byte[]</code>.
	 * @author Chris Lauderdale
	 * @param bits A <code>BitSet</code>.
	 * @return A <code>byte[]</code> containing the same bits stored in <code>bits</code>.
	 */
	public static byte[] bitSetToBytes(BitSet bits)
	{
		final byte[] bytes = new byte[(bits.size() + 7) >>> 3];
		bitSetToBytes(bits,bytes);
		return bytes;
	}

	static void dumpBytes(String s, final byte[] array)
	{
		if (s != null)
			System.out.print(s);
		for (int i = 0; i < array.length; i++)
		{
			if ((i & 15) == 0)
				System.out.print("\n@" + Integer.toString(i, 16) + ':');
			System.out.print(" " + Integer.toString(255 & (int) array[i], 16));
		}
		System.out.println();
	}
	
	private static final String ALLOWED_CHARS="$-_!";
	private static char[] HEX_CHARS={'0','1','2','3','4','5','6','7',
		                             '8','9','A','B','C','D','E','F'};
	public static String makeHTTPEscaped(String str)
	{
		final int l=str.length();
		char c;
		for(int i=0;i<l;i++)
		{
			c=str.charAt(i);
			if((c>='A' && c<='Z')
			  || (c>='a' && c<='z')
			  || (c>='0' && c<='9')
			  || ALLOWED_CHARS.indexOf(c)>=0)
				continue;
			
			final StringBuffer rv=new StringBuffer(str.substring(0,i));
			c&=0xFF;
			rv.append('%')
			  .append(HEX_CHARS[c>>>4])
			  .append(HEX_CHARS[c&15]);
			for(++i;i<l;i++)
			{
				c=str.charAt(i);
				if((c>='A' && c<='Z')
				  || (c>='a' && c<='z')
				  || (c>='0' && c<='9')
				  || ALLOWED_CHARS.indexOf(c)>=0)
					rv.append(c);
				else
				{
					c&=0xFF;
					rv.append('%')
					  .append(HEX_CHARS[c>>>4])
					  .append(HEX_CHARS[c&15]);
				}
			}
			return rv.toString();
		}
		return str;
	}
}
