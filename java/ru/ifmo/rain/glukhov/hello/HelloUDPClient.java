package ru.ifmo.rain.glukhov.hello;


import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.*;
import java.util.concurrent.*;

public class HelloUDPClient implements HelloClient {
    public HelloUDPClient() {}
    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        InetAddress address = null;
        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            System.err.println("Can't find host: " + host);
        }

        final SocketAddress socketAddress = new InetSocketAddress(address, port);
        final ExecutorService workersThreadPool = Executors.newFixedThreadPool(threads);

        for (int ind = 0; ind < threads; ind++) {
            final int id = ind;
            workersThreadPool.submit(() -> {
                try (DatagramSocket socket = new DatagramSocket()) {
                    socket.setSoTimeout(50);

                    final byte[] sendBuffer = new byte[0];
                    final DatagramPacket datagramPacketSend = new DatagramPacket(sendBuffer, 0, socketAddress);

                    final byte[] receiveBuffer = new byte[socket.getReceiveBufferSize()];
                    final DatagramPacket datagramPacketReceive = new DatagramPacket(receiveBuffer, receiveBuffer.length);

                    for (int i = 0; i < requests; i++) {
                        final String sendText = prefix + id + "_" + i;
                        while (!socket.isClosed() || Thread.currentThread().isInterrupted()) {
                            try {
                                datagramPacketSend.setData(sendText.getBytes(StandardCharsets.UTF_8));
                                socket.send(datagramPacketSend);
                                socket.receive(datagramPacketReceive);
                                final String receiveText = new String(datagramPacketReceive.getData(),
                                        datagramPacketReceive.getOffset(), datagramPacketReceive.getLength(), StandardCharsets.UTF_8);
                                if (receiveText.equals("Hello, " + sendText)) {
                                    break;
                                }
                            } catch (IOException ignored) {
                            }
                        }
                    }
                } catch (SocketException ignored) {
                }
            });
        }
        workersThreadPool.shutdown();

        try {
            workersThreadPool.awaitTermination(threads * requests, TimeUnit.MINUTES);
        } catch (InterruptedException ignored) {
        }
    }
}
