# Lunar Rover

## Goal:
*   Need to come up with a layer 4 protocol that is in theory better than TCP
*   Protocol needed to transfer packets back to earth from Moon
*   Protocol should guarantee delivery of packet
*   Packet should be able to move through the internet
*   Not necessary that packets arrive in order

## How this protocol will be used:
*   Rover(on Mooon) will send a file (say an image file) to NASA (Earth)
*   NASA(Earth) will send some confirmation back to Rover(on Moon). This needs to be a small packet

## Questions
1.   Should the rovers send the file to the lander and the lander send it to earth?
2.   If we are coming up with a new protocol to transfer the data then how will Wireshark identify it?

### TCP features:
1. Three way handshake - ensures server and client are talking to each other and comms is 2 way
2. Maintains ordering of segments
3. Sender and Receiver buffers are maintained
4. Sender and receiver exchange SEQ and ACK
5. It will retransmit if a packet is lost
6. Has a timeout. If the timeout is over and the ACK is not received, that packet is retransmitted.
7. Implements congestion control mechanism
8. Provides checksum