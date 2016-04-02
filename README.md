# A Very Light Bit Torrent Client in Java

* Sahil Gupta
* Tioluwa Olarewaju

Professor: Dr. Ellen Zegura

## Background
This light Bit torrent client has been implemented in JAVA. We have used Bencoding libraries available online. In the bibliography you will find the references to the libraries we have used. Since the point of this project was to implement Bit Torrent protocol and not learning about Bencoding, Endianness, low level details such as bit manipulation and hashing, we have referred to code from several resources such as Stack Overflow, existing libraries which perform the required functionality. We also researched online to make sure we understand the concepts for Bit Torrent protocol. We have listed in the Bibliography some links we used to research. It is worth mentioning that team-mate, Sahil, had helped in initial scoping of the project in class and directed students to a lab at another university (Swarthmore, UCB) for Bit Torrent client.

##Instruction to get this running
This project has a TorrentFileHandlerTester.java which has a main method which essentially runs the entire program/client. Since, this program is not using multi-threading, we are using a highly sequential approach for downloading pieces/blocks. Just download the entire the project and import into eclipse, IDE. Make sure all the references are correct and no linking errors exist in the project. Change the path in the main function to the torrent you want to download. The variable which stores this is called torrent_file and a call is given to torrent file handler object which has file open method which takes in the hard coded path to the file. In the UNIX systems (Mac), where we developed this program, the path was from the root/top most directory possible for the filesystem. After you hard code the path, you are all set to run the program. Run it in an IDE like eclipse for convenience which can be downloaded for free from here: https://www.eclipse.org/downloads/
Also, USE loopback address i.e. localhost 127.0.0.1 by hard coding it to test the program faster because download can take a long time.
TEST FILE USED: It has been uploaded and is under testfile folder. We used dsl-4.4.10.iso.torrent downloaded from here to test it: http://www.osst.co.uk/Download/DamnSmallLinux/current/?id=2

## Capabilities of the light torrent
Currently, we are able to input the torrent file in the TorrentFileHandlerTester.java file. This file has the main where we are able to:
* Parse the torrent file.
* Contact the tracker (GET request).
* Perform handshake with the tracker.
* Get a list of peer IP address from the tracker.
* Connect to peers.
* Perform the handshake with the peers successfully.
* Verify if the handshake is successful.
* Send and receive messages from the peer.
* Successfully construct the messages.
* Perform message passing between client and peer.
* Parse the messages successfully.
* Able to get requested blocks for a piece from the peer.
* Able to recombine blocks to get the whole piece.
* Able to download the entire file by recombining the pieces.
* Getting the list from tracker.
* Multiple peers are used to get the file and make sure download goes through. It is not multi-threaded though.

Bibliography
Research Links Used
1. http://www.research.rutgers.edu/~atjang/teaching/cs352/project.html 2. http://bnrg.cs.berkeley.edu/~adj/cs16x/ 3. http://www.kristenwidman.com/blog/how-to-write-a-bittorrent-client-part-1/ 4. To test Bencode files: http://tools.rosinstrument.com/cgi-bin/pte.pl 5. Example Bit-torrent Implementations
* http://sourceforge.net/projects/bittorrent/
* https://github.com/rakshasa/libtorrent
* https://github.com/rakshasa/rtorrent 6. Specification: https://wiki.theory.org/index.php/BitTorrentSpecification 7. RFC: http://jonas.nitro.dk/bittorrent/bittorrent-rfc.html 8. The BitTorrent Protocol Specification: http://www.bittorrent.org/beps/bep_0003.html 9. Stack Overflow Links 
* http://stackoverflow.com/questions/990677/implementing-bittorrent-protocol
* https://tools.ietf.org/html/rfc5694