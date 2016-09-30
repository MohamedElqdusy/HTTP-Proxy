import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class HttpProxy {
    static Socket browser;

    public static void main(String[] args) {
        ServerSocket browserProxy;
        try {
            browserProxy = new ServerSocket(12345);
            System.out.println("Proxy is waiting for connection...");
            while (true) {
                browser = browserProxy.accept();
                /*
                 * System.out.println("Connection received from " +
                 * browser.getInetAddress().getHostName() + " : " +
                 * browser.getPort());
                 */

                PerConnection con = new PerConnection();
                Thread th = new Thread(con);
                th.run();

            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }

    static class PerConnection implements Runnable {

        PerConnection() {
            if (browser == null)
                throw new IllegalArgumentException("SOCKET IS NULL");
        }

        @Override
        public void run() {
            try {
                int port = 80; // browse default port
                String requestLine;
                String host = null;
                // Get request data from browser socket
                BufferedReader inputData = new BufferedReader(
                        new InputStreamReader(browser.getInputStream()));
                // server reply.
                DataOutputStream reply = new DataOutputStream(
                        browser.getOutputStream());
                // buffer to store client request
                StringBuffer outputBuffer = new StringBuffer();
                int lineNumber = 1;
                while ((requestLine = inputData.readLine()) != null
                        && requestLine.length() >= 1) {

                    // print firs line in request .
                    if (lineNumber == 1) {
                        // change HTTP version 1.1 to version 1.0 .
                        // int strlength = requestLine.length() - 8;
                        // requestLine = requestLine.substring(0, strlength)+
                        // "HTTP/1.0";
                        System.out.println(">>> " + requestLine);

                    } else if (requestLine.startsWith("Host: ")) {
                        // get host port number .
                        String hostString[] = requestLine.split(":");
                        if (hostString.length == 2) {

                            host = hostString[1].trim();// delete spaces
                            // System.out.printf("1-host

                        } else if (hostString.length == 3) {

                            host = hostString[1].trim();// delete spaces
                            port = Integer.valueOf(hostString[2]);
                            // System.out.printf("2-host

                        }

                    } else if (requestLine.equals("Connection: keep-alive")) {
                        // Turning off keep-alive .
                        requestLine = "Connection: close";
                    }

                    else if (requestLine
                            .equals("Proxy-Connection: keep-alive")) {
                        // Turning off keep-alive .
                        requestLine = "Proxy-Connection: close";
                    }
                    outputBuffer.append(requestLine + "\r\n");
                    lineNumber++;
                }
                outputBuffer.append("\r\n"); // append HTTP end .

                // System.out.println(outputBuffer);// Debugging purpose.
                // System.out.println("DONE");// Debugging purpose.
                processRequest(host, port, outputBuffer, reply);
                inputData.close();

            } catch (IOException e) {
                System.out.println(e.getMessage());
            }

        }

        private void processRequest(String host, int port, StringBuffer s,
                DataOutputStream out) {
            try {
                int bytesNumber;
                Socket server = new Socket(host, port);
                InputStream serverReply = server.getInputStream();
                PrintWriter browserRequest = new PrintWriter(
                        new OutputStreamWriter(server.getOutputStream()));
                browserRequest.print(s.toString());

                browserRequest.flush();// flush writer .
                // Buffer to handel server reply and send it to Browser.
                byte[] buff = new byte[1024];

                // reading Buffer
                while ((bytesNumber = serverReply.read(buff)) != -1) {
                    out.write(buff, 0, bytesNumber);//write the reply to client.
                    out.flush();
                }

                // close I/O for this connection.
                browserRequest.close();
                serverReply.close();
                server.close();
                out.close();
            } catch (UnknownHostException e) {
                System.out.println("UnknownHost: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("IO: " + e.getMessage());
            }

        }
    }

}
