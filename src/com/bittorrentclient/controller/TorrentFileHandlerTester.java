/**
 * @author Sahil Gupta and Tioluwa Olarewaju
 * This is the main tested file which runs bittorrent client.
 */

package com.bittorrentclient.controller;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.bittorrentclient.model.TorrentFile;
import com.bittorrentclient.model.TorrentFileHandler;
import com.bittorrentclient.services.Bencoder2;
import com.bittorrentclient.services.ToolKit;
import com.bittorrentclient.services.Utilities;
import com.bittorrentclient.services.Utils;

public class TorrentFileHandlerTester {
	private TorrentFileHandler torrent_file_handler;

	private TorrentFile torrent_file;

	private static String torrentFilePath;
	private static String torrentOutputFileName;

	/*
	 * Added Variable Below for getting the peers
	 */
	String peerID;
	Integer portNumber;
	Long downloadedBytes;
	Long uploadedBytes;
	Long leftBytes;
	String event;
	List<String> peerList;
	int interval;
	int min_interval;
	Socket peerSocket;
	Socket peerSuccessfulSocket;// when the handshake has been successful only
								// then

	int socketTimeout;
	// use this
	int NumberOfPiecesToDownload;

	Long lastRequestTime = System.currentTimeMillis();

	boolean keepAlive = true;
	boolean isChoked = true;
	boolean isInterested = false;
	ArrayList<Boolean> peerHasPieces;

	// Specification says: Ports reserved for BitTorrent are typically
	// 6881-6889.
	// Clients may choose to give up if it cannot establish a port within this
	// range.
	final int BITTORRENT_PORT_MIN_RANGE = 6881;
	final int BITTORRENT_PORT_MAX_RANGE = 6889;

	final int BLOCK_SIZE = 16384;

	boolean completedPieces[];

	ArrayList<ArrayList<byte[]>> piecesList;

	/**
	 * Invokes a private method to load a specific .torrent file, parse it, and
	 * display its unencoded contents.
	 * 
	 */
	public TorrentFileHandlerTester() {
		super();
		this.peerID = "sT199220142010600210";
		this.portNumber = 6881;
		this.downloadedBytes = 0L;
		this.uploadedBytes = 0L;
		this.event = "started";
		this.socketTimeout = 2000;
		peerList = new ArrayList<String>();
		testTorrentFileHandler();
	}

	/**
	 * Generates a new TorrentFileHandlerTester object to demonstrate how to use
	 * a TorrentFileHandler object.
	 * 
	 * @param args
	 *            Not used.
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.out.println(
					"Torrent input file and torrent output file name not specified. Args are:");
			for (String arg : args) {
				System.out.println(arg);
			}
			System.exit(1);
		}

		torrentFilePath = new String(args[0]);
		torrentOutputFileName = new String(args[1]);

		TorrentFileHandlerTester tfht = new TorrentFileHandlerTester();
		System.out.println(tfht.torrent_file.tracker_url);

		try {
			tfht.sendGet(tfht.torrent_file.tracker_url,
					tfht.torrent_file.info_hash_as_url, tfht.peerID,
					tfht.portNumber, tfht.downloadedBytes, tfht.uploadedBytes,
					new Long(tfht.torrent_file.file_length), tfht.event);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void testTorrentFileHandler() {
		torrent_file_handler = new TorrentFileHandler();
		torrent_file = torrent_file_handler
				.openTorrentFile(TorrentFileHandlerTester.torrentFilePath);

		if (torrent_file != null) {
			NumberOfPiecesToDownload = torrent_file.piece_hash_values_as_url
					.size();
			completedPieces = new boolean[NumberOfPiecesToDownload];
			piecesList = new ArrayList<ArrayList<byte[]>>(
					NumberOfPiecesToDownload);
			for (int i = 0; i < completedPieces.length; i++) {
				completedPieces[i] = false;
			}
			System.out.println("Tracker URL: " + torrent_file.tracker_url);
			System.out
					.println("File Size (Bytes): " + torrent_file.file_length);
			System.out.println(
					"Piece Size (Bytes): " + torrent_file.piece_length);
			System.out.println(
					"SHA-1 Info Hash: " + torrent_file.info_hash_as_url);
			for (int i = 0; i < torrent_file.piece_hash_values_as_hex
					.size(); i++) {
				System.out.println("SHA-1 Hash for Piece [" + i + "]: "
						+ (String) torrent_file.piece_hash_values_as_url
								.elementAt(i));
			}
		} else {
			System.err.println(
					"Error: There was a problem when unencoding the file \"dsl-4.4.10.iso.torrent\".");
			System.err.println("\t\tPerhaps it does not exist.");
		}
	}

	@SuppressWarnings("rawtypes")
	public void sendGet(String announce, String infoHashUrl, String peer_id,
			Integer port, Long uploadedBytes, Long downloadedBytes,
			Long leftBytes, String event) throws Exception {

		// Create the URL for the get request
		String peerIDEncoding = ToolKit.makeHTTPEscaped(peer_id);
		String query = String.format(
				"info_hash=%s&peer_id=%s&port=%s&uploaded=%s&downloaded=%s&left=%s&event=%s",
				infoHashUrl, peerIDEncoding, port, uploadedBytes,
				downloadedBytes, leftBytes, event);
		URL trackerUrl = new URL(announce + "?" + query);

		// contact the tracker url
		try {
			InputStream inputStreamFromServer = trackerUrl.openStream();
			byte[] responseInBytes = new byte[512];
			int b = -1;
			int pos = 0;
			while ((b = inputStreamFromServer.read()) != -1) {
				responseInBytes[pos] = (byte) b;
				++pos;
			}
			// get the peer list along with port number to contact
			Map trackerResponse = (Map) Bencoder2.decode(responseInBytes);
			peerList = new ArrayList<String>(Arrays
					.asList(Utilities.decodeCompressedPeers(trackerResponse)));
			HashSet<String> hs = new HashSet<String>();
			hs.addAll(peerList);
			peerList.clear();
			peerList.addAll(hs);
			ArrayList<String> peerListTemp = new ArrayList<String>();
			for (int i = 0; i < peerList.size(); i++) {
				peerListTemp.add(peerList.get(i));
				System.out.println(peerList.get(i));
			}
			peerList = peerListTemp;

			inputStreamFromServer.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// for each and every peer try to make a connection
		// if file has been downloaded before reaching all the peers
		// then exit before hand
		boolean stop = false;
		while (!stop) {
			for (String peer : peerList) {
				peerHandshake(peer);
			}

			for (int i = 0; i < completedPieces.length; i++) {
				if (!completedPieces[i])
					socketTimeout = socketTimeout + 1000;
				else {
					System.out.println("File downloaded");
					stop = true;
					break;
				}
			}
		}
	}

	public void peerHandshake(String peer) throws Exception {
		String[] ipAddressAndPort = peer.split(":");
		String ipAddress = ipAddressAndPort[0];
		Integer port = Integer.parseInt(ipAddressAndPort[1]);
		if (!openSocket(ipAddress, port)) {
			System.out.println("Connection failed with peer at ip address: "
					+ ipAddress + " and port: " + port + "\n");
		} else {
			// Hurray! Connection successful. Time to download pieces from peer.
			System.out.println("Connection succeeded with peer at ip address: "
					+ ipAddress + " and port: " + port + "\n");
			goUntilUnChoked();
		}
	}

	public Boolean openSocket(String ipAddress, Integer port) throws Exception {

		peerSocket = null;

		while (port <= 65535 && peerSocket == null) {
			try {
				// if socket is not created within 2 seconds, move on to the
				// next peer.
				peerSocket = new Socket();
				peerSocket.connect(new InetSocketAddress(ipAddress, port),
						socketTimeout);
			} catch (Exception e) {
				port++;
			}
		}

		if (peerSocket == null) {
			return false;
		}

		try {
			DataOutputStream peerStream = new DataOutputStream(
					peerSocket.getOutputStream());
			peerStream.writeByte(19);
			peerStream.write("BitTorrent protocol".getBytes());
			peerStream.write(new byte[8]);
			peerStream.write(this.torrent_file.info_hash_as_binary);
			peerStream.write(this.peerID.getBytes());

			if (checkPeer(peerSocket)) {
				System.out.println("Peer response is correct");
				peerSuccessfulSocket = peerSocket;
				return true;
			}
		} catch (UnknownHostException e) {
			return false;
			// TODO Auto-generated catch block
			// e.printStackTrace();
		} catch (IOException e) {
			return false;
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
		return false;
	}

	public boolean checkPeer(Socket peerSocket) throws Exception {
		DataInputStream is = new DataInputStream(peerSocket.getInputStream());
		int protocolIdentifierLength = is.readByte();

		if (protocolIdentifierLength != 19) {
			return false;
		}

		byte[] protocolString = new byte[protocolIdentifierLength];
		is.readFully(protocolString);

		if (!Arrays.equals(protocolString, "BitTorrent protocol".getBytes())) {
			return false;
		}

		byte[] reserved = new byte[8];
		is.readFully(reserved);

		byte[] infoHash = new byte[20];
		is.readFully(infoHash);

		if (!Arrays.equals(infoHash, this.torrent_file.info_hash_as_binary)) {
			return false;
		}

		byte[] peerId = new byte[20];
		is.readFully(peerId);

		return true;
	}

	public static final byte KEEP_ALIVE = -1;// keep-alive: <len=0000>
	public static final byte CHOKE = 0;// choke: <len=0001><id=0>
	public static final byte UNCHOKE = 1;// unchoke: <len=0001><id=1>
	public static final byte INTERESTED = 2;// interested: <len=0001><id=2>
	public static final byte UNINTERESTED = 3;// not interested:
												// <len=0001><id=3>
	public static final byte HAVE = 4;// have: <len=0005><id=4><piece index>
	public static final byte BITFIELD = 5;// bitfield:
											// <len=0001+X><id=5><bitfield>
	public static final byte REQUEST = 6;// request:
											// <len=0013><id=6><index><begin><length>
	public static final byte PIECE = 7;// piece:
										// <len=0009+X><id=7><index><begin><block>
	public static final byte CANCEL = 8;// cancel:
										// <len=0013><id=8><index><begin><length>

	public void goUntilUnChoked() {
		boolean stop = false;
		BitSet torrentBitSet = new BitSet();
		while (!stop) {
			try {
				DataOutputStream peerStream = new DataOutputStream(
						peerSuccessfulSocket.getOutputStream());

				DataInputStream is = new DataInputStream(
						peerSuccessfulSocket.getInputStream());

				int prefixLength = is.readInt();
				System.out.println("Prefix Length: " + prefixLength);

				if (prefixLength == 0) { // keep alive message

				} else if (prefixLength > 0) {
					byte messageId = is.readByte();
					System.out.println("Message ID: " + messageId);
					switch (messageId) {
					case BITFIELD: // bitfield: <len=0001+X><id=5><bitfield>
						System.out.println("bitfield received");
						byte[] torrentBitField = new byte[prefixLength - 1];
						is.readFully(torrentBitField);
						torrentBitSet = BitSet.valueOf(torrentBitField);
						peerStream.writeInt(1);
						peerStream.writeByte(INTERESTED);
						break;
					case UNCHOKE: // unchoke: <len=0001><id=1>
						if (prefixLength != 1) {
							throw new ProtocolException();
						}
						System.out.println("unchoked!");
						stop = true;
						break;
					default:
						// discard it
						is.skipBytes(prefixLength - 1);
						break;
					}
				}
				// keep sending interested until we get unchoked
				if (!stop) {
					peerStream.writeInt(1);
					peerStream.writeByte(INTERESTED);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		requestPieces(torrentBitSet);

	}

	private void requestPieces(BitSet torrentBitSet) {
		int index = getNextPieceIndex(0, torrentBitSet);
		if (!completedPieces[index]) {
			piecesList.add(index, new ArrayList<byte[]>());
			int downloadedPieceBytes = 0;
			int begin = 0;
			boolean stop = false;
			while (!stop) {

				try {

					DataOutputStream peerStream = new DataOutputStream(
							peerSuccessfulSocket.getOutputStream());

					DataInputStream is = new DataInputStream(
							peerSuccessfulSocket.getInputStream());

					peerStream.writeInt(13);
					peerStream.writeByte(REQUEST);
					int length = (int) Math.min(BLOCK_SIZE,
							this.torrent_file.file_length - downloadedBytes);
					writeRequestPayload(index, begin, length, peerStream);

					int prefixLength = is.readInt();
					System.out.println("Prefix Length: " + prefixLength);

					if (prefixLength == 0) { // keep alive message

					} else if (prefixLength > 0) {
						byte messageId = is.readByte();
						System.out.println("Message ID: " + messageId);
						switch (messageId) {
						case CHOKE: // choke: <len=0001><id=0>
							if (prefixLength != 1) {
								throw new ProtocolException(
										"pl " + prefixLength);
							}
							System.out.println("choked!");
							// Move on to next peer
							break;
						case UNCHOKE: // unchoke: <len=0001><id=1>
							if (prefixLength != 1) {
								throw new ProtocolException();
							}
							System.out.println("unchoked!");

							break;
						case HAVE: // have: <len=0005><id=4><piece index>

							if (prefixLength != 5) {
								throw new ProtocolException();
							}
							int pieceIndex = is.readInt();
							System.out.println(
									"have!, piece index:" + pieceIndex);

							break;
						case PIECE: // piece:
									// <len=0009+X><id=7><index><begin><block>

							if (prefixLength < 10) {
								throw new ProtocolException();
							}

							int thisIndex = is.readInt();
							int thisBegin = is.readInt();
							byte[] block = new byte[prefixLength - 9];
							addDownLoadedBytes(prefixLength - 9);
							is.readFully(block);

							piecesList.get(index).add(block);
							downloadedPieceBytes = downloadedPieceBytes
									+ prefixLength - 9;
							begin = begin + prefixLength - 9;
							System.out.println("piece received, index: "
									+ thisIndex + " begin: " + thisBegin);

							break;
						case CANCEL: // cancel:
										// <len=0013><id<=8><index><begin><length>
							System.out.println(
									"cancel prefix length is " + prefixLength);

							if (prefixLength != 13) {
								throw new ProtocolException();
							}
							break;
						default:
							// discard it
							is.skipBytes(prefixLength - 1);
							break;

						}
					}

					if (downloadedBytes == this.torrent_file.file_length) {
						stop = true;
						System.out.println("done");
						writeFile();
					} else if (downloadedPieceBytes == this.torrent_file.piece_length) {
						System.out.println("Next piece, index: " + index);
						completedPieces[index] = true;
						downloadedPieceBytes = 0;
						begin = 0;
						index++;
						piecesList.add(index, new ArrayList<byte[]>());
					}

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public void writeFile() {
		try {
			// convert array of bytes into file
			FileOutputStream fileOuputStream = new FileOutputStream(
					TorrentFileHandlerTester.torrentOutputFileName);

			for (int i = 0; i < piecesList.size(); i++) {
				ArrayList<byte[]> myList = piecesList.get(i);
				for (int x = 0; x < myList.size(); x++) {
					byte[] block = myList.get(x);
					fileOuputStream.write(block);
				}
			}
			fileOuputStream.close();
			System.out.println("Done");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void writeRequestPayload(int index, int begin, int length,
			DataOutputStream peerStream) throws Exception {
		byte[] payload = new byte[12];
		System.arraycopy(Utils.intToByteArray(index), 0, payload, 0, 4);
		System.arraycopy(Utils.intToByteArray(begin), 0, payload, 4, 4);
		System.arraycopy(Utils.intToByteArray(length), 0, payload, 8, 4);

		int payloadOffset = 0;
		while (payloadOffset < payload.length) {
			int payloadMissing = payload.length - payloadOffset;
			int payloadChunk = payloadMissing > 1 << 10 ? 1 << 10
					: payloadMissing;
			peerStream.write(payload, payloadOffset, payloadChunk);

			payloadOffset += payloadChunk;
		}
	}

	public int getNextPieceIndex(int currentIndex, BitSet torrentBitSet) {
		int i = currentIndex;
		while (!torrentBitSet.get(i)) {
			i++;
		}
		return i;
	}

	private void addDownLoadedBytes(Integer byteNumber) {
		downloadedBytes += byteNumber;
		System.out.println("Downloaded bytes: " + downloadedBytes);
	}

}
