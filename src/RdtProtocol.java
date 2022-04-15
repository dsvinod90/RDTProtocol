/**
 * This is the class that outlines the custom reliable data transfer protocol which sits on top of UDP.
 * @author Vinod Dalavai - vd1605
 */
public class RdtProtocol {
    // Static variables for this class
    // public static final int BUFFER_LENGTH             = 100;
    public static final int DATAGRAM_LENGTH           = 1_024 * 2;
    public static final int SEQ_END_POSITION          = 3;
    public static final int ACK_FLAG_POSITION         = 6;
    public static final int NAK_FLAG_POSITION         = 7;
    public static final int FIXED_HEADER_SIZE         = 12;
    public static final int SEQ_START_POSITION        = 0;
    public static final int SOURCE_ID_POSITION        = 4;
    public static final int DESTINATION_ID_POSITION   = 5;
    public static final int ACK_NUMBER_END_POSITION   = 11;
    public static final int ACK_NUMBER_START_POSITION = 8;
    // Declare class fields
    private int seq; // Sequence number
    private byte sourceRoverId; // ID of the rover sending the data
    private byte destinationRoverId; // ID of the rover receiving the data
    private boolean ack = false; // Acknowledgement flag
    private boolean nak = false; // Negative acknowledgement flag
    private int acknowledgementNumber = 0; // Acknowledgement or Negative acknowledgement based on the respective boolean flags
    private String checksum; // Checksum value of the current segment
    private byte[] byteSequence = new byte[DATAGRAM_LENGTH + FIXED_HEADER_SIZE];  // byte sequence that will be passed to the application at the listening port
    private byte[] inputData; // Data to be added to the RDT body
    private String data; // string data to be sent

    /**
     * Constructor for Rdt Protocol
     * @param byteData  byte[]
     */
    public RdtProtocol(byte[] byteData, byte sourceRoverId, byte destinationRoverId) {
        this.inputData = byteData;
        this.sourceRoverId= sourceRoverId;
        this.destinationRoverId = destinationRoverId;
    }

    /**
     * Getter method for Sequence number
     * @return  long
     */
    public int getSeq() {
        return this.seq;
    }

    /**
     * Setter method for Sequence number
     * @param seq   long
     */
    public void setSeq(int seq) {
        this.seq = seq;
    }

    /**
     * Getter method for Acknowledgement number
     * @return  long
     */
    public boolean getAck() {
        return this.ack;
    }

    /**
     * Setter method for Acknowledgement number
     * @param ack   long
     */
    public void setAck(boolean ack) {
        this.ack = ack;
    }

    /**
     * Getter method for Negative Acknowledgement number
     * @return  long
     */
    public boolean getNak() {
        return this.nak;
    }

    /**
     * Setter method for Negative Acknowledgement number
     * @param nak
     */
    public void setNak(boolean nak) {
        this.nak = nak;
    }

    /**
     * Getter method for Checksum
     * @return  String
     */
    public String getChecksum() {
        return this.checksum;
    }

    /**
     * Setter method for checksum
     * @param checksum  String
     */
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    /**
     * Getter method for the byte stream
     * @return  byte[]
     */
    public byte[] getByteStream() {
        return byteSequence;
    }

    /**
     * Getter method for offset
     * @return  int
     */
    // public int getOffset() {
    //     return this.offset;
    // }

    /**
     * Setter method for offset
     * @param offset    int
     */
    // public void setOffset(int offset) {
    //     this.offset = offset;
    // }

    /**
     * Getter for Acknowledgement number
     * @return  int
     */
    public int getAcknowledgementNumber() {
        return this.acknowledgementNumber;
    }

    /**
     * Setter for Acknowledgement numbers
     */
    public void setAcknowledgementNumber(int acknowledgementNumber) {
        this.acknowledgementNumber = acknowledgementNumber;
    }

    /**
     * Getter for data
     * @return  String
     */
    public String getData() {
        return this.data;
    }

    /**
     * Setter for data
     * @param data  String
     */
    public void setData(String data) {
        this.data = data;
    }

    /**
     * Method to prepare segment to be sent over the RDT
     */
    public void prepareSegment() {
        int index = 0;
        byteSequence[index++] = (byte) (this.seq >> 24);
        byteSequence[index++] = (byte) (this.seq >> 16);
        byteSequence[index++] = (byte) (this.seq >> 8);
        byteSequence[index++] = (byte) (this.seq);
        byteSequence[index++] = this.sourceRoverId;
        byteSequence[index++] = this.destinationRoverId;
        byteSequence[index++] = (this.ack) ? (byte)1 : (byte)0;
        byteSequence[index++] = (this.nak) ? (byte)1 : (byte)0;
        byteSequence[index++] = (byte) (this.acknowledgementNumber >> 24);
        byteSequence[index++] = (byte) (this.acknowledgementNumber >> 16);
        byteSequence[index++] = (byte) (this.acknowledgementNumber >> 8);
        byteSequence[index++] = (byte) (this.acknowledgementNumber);
        if (this.inputData != null) {
            for (int ind = 0; ind < this.inputData.length; ind++) {
                byteSequence[index++] = this.inputData[ind];
            }
        }
    }

    /**
     * Method to extract the data portion of the protocol
     * @param udpData
     * @return
     */
    public static byte[] extractData(byte[] udpData) {
        byte[] dataBytes = new byte[udpData.length];
        int ind = 0;
        int startIndex = 12;
        for (int index = startIndex; index < udpData.length; index++) {
            dataBytes[ind++] = udpData[index];
        }
        return dataBytes;
    }
}
