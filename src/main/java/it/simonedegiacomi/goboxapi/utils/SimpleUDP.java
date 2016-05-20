package it.simonedegiacomi.goboxapi.utils;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;

/**
 * Class that provides simple UDP utilities
 * Created on 21/03/16.
 * @author Degiacomi Simone
 */
public class SimpleUDP {

    /**
     * Datagram socket
     */
    private DatagramSocket socket;

    public SimpleUDP () {}

    /**
     * Initialize the udp socket
     * @throws SocketException
     */
    private void initSocket () throws SocketException {

        // Create a new UDP socket
        socket = new DatagramSocket();
        socket.setBroadcast(true);
    }

    /**
     * Send a broadcast udp packet
     * @param port Port to which send the broadcast packet
     * @param requestBytes Message to send with the packet
     * @throws IOException
     */
    public void sendBroadcastPacket(int port, byte[] requestBytes) throws IOException {

        // Check if is initialized
        if(socket == null)
            initSocket();

        // Send the request to the broadcast IP
        DatagramPacket request = new DatagramPacket(requestBytes, requestBytes.length, InetAddress.getByName("255.255.255.255"), port);
        socket.send(request);

        // Send the request to all the network interfaces
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {

            NetworkInterface netInterface = interfaces.nextElement();

            if (netInterface.isLoopback())
                continue;

            for(InterfaceAddress address : netInterface.getInterfaceAddresses()) {
                InetAddress broadcast = address.getBroadcast();
                if(broadcast != null) {
                    request.setAddress(broadcast);
                    socket.send(request);
                }
            }
        }
    }

    /**
     * Receive a udp packet. This method has a timeout of 2 seconds
     * @return Received udp packet
     * @throws SocketException
     * @throws IOException
     */
    public DatagramPacket receive () throws SocketException, IOException{

        // Check if is initialized
        if(socket == null)
            initSocket();

        // Listen for response
        byte[] responseBuffer = new byte[255];
        DatagramPacket response = new DatagramPacket(responseBuffer, responseBuffer.length);

        // Let's listen for 2 seconds
        socket.setSoTimeout(2000);
        socket.receive(response);

        return response;
    }
}
