# Reliable Data Transfer Protocol

This is a protocol that combines UDP and TCP to provide a reliable yet theoretically faster Transport Layer Protocol than TCP.
## Headers
1. **SEQ (4 bytes)** : A sequence number indicating the sequence of the packet being sent from the sender to the receiver. All sequence numbers begin with 1 (for ease of implementation)
2. **SOURCE_ID (1 byte):** The sender ID who is sending the command
3. **DESTINATION_ID (1 byte):** The destinatio id of the rover receiving the command
4. **ACK (1 byte):** An Acknowledgement Flag which is set to true when an Ack is sent to the sender by the receiver in intervals
5. **NAK (1 byte):** A Negative Acknowledgement Flag which is set to true when some packets did not arrive at the receiver's end
6. **FIN (1 byte):**  A Finish Flag that is sent by the sender before terminating the process of sending
8. **COMMAND (1 byte):** A command byte that indicates if the sender is sending a file or a command
9. **ACK_NUMBER (4 bytes):** An acknowledgement number that is sent with the ack packet
10. **DATA (Upto 2048 bytes):** Data to be delivered to the application layer of the receiver

## Working of the protocol:
Let's assume one rover (NASA) wants to ask a Lunar Rover for an image file. Here is what happens:
1. NASA creates a RDT with SEQ, SOURCE_ID as its own id, DESTINATION_ID as the rover's id, ACK = 0, NAK = 0, FIN = 0, COMMAND = 5, ACK_NUMBER = 0 and DATA = the image's byte stream upto 2048 bytes.
2. It then sends this packet encapsulated in a UDP package. The benefit of doing so is that UDP does establish a connection before sending data and so we save the time required to perform the 3 way handshake.
3. The receiver is listening on its port receives the data and checks for any missing packets
4. If packets are missing, it sends a NAK by setting the NAK flag to true and then sending the list of all the missing sequences in the DATA portio n of the packet. It then sends this to the sender.
5. The sender will then send those missing packets to the receiver and wait for acknowledgement
6. The receiver continues receiving packets till its buffer is full i.e. 20 packets arrive. After that, the receiver will arrange the packets in order and then write the data to a file.
7. It then cleans its buffer and is ready to get more packets for writing to file.
8. The receiver sends an ACK to the sender with the ack number corresponding to the next expected sequence number
9. The sender upon receiving this verifies to check if the ack and seq are matching and will send the next set of packets
10. This process continues until all the packets are delivered to the receiver.
11. When the last packet is sent, the sender will set the FIN to true. The receiver will then acknowledge the FIN and then the sender terminates the connection.
12. The file created is stored on disk as "received_<random_number>.jpg"

## How to use the protocol:
**Navigate to the src folder and follow the commands below:**
### Syntaxes:
*To compile the program*
```
make nasa - This will 
```
OR
```
javac -d byteCode *.java
```

*To setup the listener*
```
make rover id=<this_rover_id> receiver=<RECEIVER_OPTIONS> listen=520 multicastIP=224.0.0.1 role=receiver command=0
```
OR
```
cd byteCode && java Main <this_rover_id> <RECEIVER_OPTION> 520 224.0.0.1 receiver 0
```
Repeat the command for multiple times with different values of <this_rover_id> and <RECEIVER_OPTIONS>

*To setup the sender*
```
 make rover id=<this_rover_id> receiver=<receiver_id> listen=520 multicastIP=224.0.0.1 role=sender command=<COMMAND_OPTIONS> path=../file_name.jpg
```
OR
```
cd byteCode && java Main <this_rover_id> <receiver_id> 520 224.0.0.1 sender <COMMAND_OPTIONS> ../file_name.jpg
```

### Points to Note:
1. Start the receiver before starting the sender
2. All sequence number start with 1 for ease of implementation
3. You can execute multiple commands to move to rover in succession without restarting the receiver but after a file is send the sender will close the connection. So please restart the receiver after a file transfer is complete and then use the sender to perform any task
4. *Known issue:* There are two files being created right now for every file transfer. One is an empty file and the other is the actual file. I could not find a way to fix this issue on time. 
5. Keep the image files in the same directory as the other java files
6. The file exchange will only work for jpg out of the box.
7. If you want to exchange a different file type then please set the extension in Receiver.java (Line Number: 34) to the desired extension.

### COMMAND OPTIONS:
        1: "Move Forward"
        2: "Move Back"
        3: "Turn Left"
        4: "Turn Right"
        5: "Capture and Send Image"

### RECEIVER OPTIONS:
    <id> - to indicate that that it will only listen to that id
    "all" - to indicate that it will listen to all the traffic
