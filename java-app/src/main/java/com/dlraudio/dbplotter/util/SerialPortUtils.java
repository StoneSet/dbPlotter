package com.dlraudio.dbplotter.util;

import com.fazecast.jSerialComm.SerialPort;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

public class SerialPortUtils {

    private static final int BAUD_RATE = 115200;
    private static String currentPort = null;
    private static SerialPort serialPort;
    private static Thread listenerThread;
    private static Consumer<String> messageListener;
    private static boolean isListening = false; //check if listener is running

    /**
     * Lister les ports série disponibles (listAvailablePorts)
     */
    public static String[] listAvailablePorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        String[] portNames = new String[ports.length];
        for (int i = 0; i < ports.length; i++) {
            portNames[i] = ports[i].getSystemPortName();
        }
        return portNames;
    }

    /**
     * Connecter au port série et attendre READY via le listener
     */
    public static boolean connect(String portName) {
        if (serialPort != null && serialPort.isOpen()) {
            System.out.println("Port already open: " + portName);
            return true;
        }

        serialPort = SerialPort.getCommPort(portName);
        serialPort.setComPortParameters(BAUD_RATE, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 1000, 1000);

        if (!serialPort.openPort()) {
            System.err.println("Failed to connect to " + portName);
            return false;
        }

        currentPort = portName;
        System.out.println("Connected to " + portName + ". Waiting for Arduino reboot...");

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Now waiting for READY signal...");

        if (!isListening) {
            startListening();
        }

        // Attendre READY via le listener
        final boolean[] isReady = {false};
        setMessageListener(message -> {
            if ("READY".equalsIgnoreCase(message.trim())) {
                System.out.println("Arduino is READY!");
                isReady[0] = true;
            }
        });

        // Timeout après 10 sec si "READY" non reçu
        long startTime = System.currentTimeMillis();
        while (!isReady[0] && (System.currentTimeMillis() - startTime) < 10000) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (!isReady[0]) {
            System.err.println("Timeout: Arduino did not send READY.");
            disconnect();
            return false;
        }

        return true;
    }

    /**
     * Déconnecter du port série (disconnect)
     */
    public static void disconnect() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
            currentPort = null;
            System.out.println("Disconnected from serial port.");
        }
        isListening = false;
    }

    /**
     * Envoyer des données au port série (writeToPort)
     */
    public static void writeToPort(String data) {
        if (serialPort != null && serialPort.isOpen()) {
            try {
                OutputStream outputStream = serialPort.getOutputStream();
                outputStream.write(data.getBytes());
                outputStream.flush();
                System.out.println("Data sent: " + data);
            } catch (Exception e) {
                System.err.println("Error sending data: " + e.getMessage());
            }
        } else {
            System.err.println("Port not open.");
        }
    }

    /**
     * Vérifier si le port est connecté (isConnected)
     */
    public static boolean isConnected() {
        //System.out.println("Port is open: " + (serialPort != null && serialPort.isOpen()));
        return serialPort != null && serialPort.isOpen();
    }

    /**
     * Ajoute un listener qui sera appelé dès qu'un message est reçu.
     */
    public static void setMessageListener(Consumer<String> listener) {
        messageListener = listener;
    }

    /**
     * Écoute en continu les messages du port série.
     */
    private static void startListening() {
        if (isListening) return;

        isListening = true;
        listenerThread = new Thread(() -> {
            try {
                InputStream inputStream = serialPort.getInputStream();
                byte[] buffer = new byte[1024];

                while (serialPort != null && serialPort.isOpen()) {
                    int numBytes = inputStream.read(buffer);
                    if (numBytes > 0) {
                        String received = new String(buffer, 0, numBytes).trim();
                        System.out.println("Received: " + received);

                        if (messageListener != null) {
                            messageListener.accept(received);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error in serial listening: " + e.getMessage());
            } finally {
                isListening = false; // Assurer que l'état est correct si le thread s'arrête
            }
        });

        listenerThread.setDaemon(true);
        listenerThread.start();
    }
}
