package ru.ifmo.rain.glukhov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class HelloUDPServer implements HelloServer {
    private DatagramSocket socket = null;
    private ExecutorService workers = null;
    private ExecutorService listener = null;
    private boolean closed = true;

    public HelloUDPServer() {
    }

    @Override
    public void start(int port, int threads) {
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            //System.err.println("Can't create socket in port = " + Integer.toString(port) + ".\n" + e.getMessage());
            return;
        }
        listener = Executors.newSingleThreadExecutor();
        workers = Executors.newFixedThreadPool(threads);
        closed = false;
        listener.submit(
            () -> {
                while (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
                    try {
                        final byte[] receiveBuffer = new byte[socket.getReceiveBufferSize()];
                        final DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                        socket.receive(receivePacket);
                        workers.submit(
                            () -> {
                                try {
                                    final String sendText = new String(receivePacket.getData(),
                                            receivePacket.getOffset(), receivePacket.getLength(), StandardCharsets.UTF_8);
                                    final byte[] sendBytes = new byte[0];
                                    final DatagramPacket sendPacket = new DatagramPacket(sendBytes, 0,
                                            receivePacket.getSocketAddress());
                                    sendPacket.setData(("Hello, " + sendText).getBytes(StandardCharsets.UTF_8));
                                    socket.send(sendPacket);
                                } catch (IOException e) {
                                    if (!closed) {
                                        //System.err.println("Runtime error: " + e.getMessage());
                                    }
                                }
                            }
                        );
                    } catch (IOException e) {
                        if (!closed) {
                            //System.err.println("Runtime error: " + e.getMessage());
                        }
                    }
                }
            }
        );
    }


    @Override
    public void close() {
        closed = true;
        socket.close();
        listener.shutdownNow();
        workers.shutdownNow();
    }
}