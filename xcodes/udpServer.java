import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;


public class udpServer {

	public static void main(String[] args) {
		new udpServer();
	}
	public udpServer() {
		
        try {
        	DatagramSocket  server = new DatagramSocket(5050);
        	byte[] recvBuf = new byte[1024];
        	DatagramPacket recvPacket 
            	= new DatagramPacket(recvBuf , recvBuf.length);
        	while(true){
        		server.receive(recvPacket);
                FtpHandler h = new FtpHandler(recvPacket);
				h.start();
        	}
		} catch (IOException e1) {
			e1.printStackTrace();
		}

	}

	public ArrayList<FtpHandler> users = new ArrayList<FtpHandler>();
	public static int counter = 0;
	public static String initDir = "ftp/";

	class UserInfo {
		String user;
		String password;
		String workDir;
		
		public UserInfo(String a, String b, String c)
		{
			user = a;
			password = b;
			workDir = c;
		}
	}

	class FtpHandler extends Thread {
		String ServerIP = "192.168.140.6";

		DatagramSocket server;
		int port; 
		InetAddress clientAddr = null;
		int id;
		String cmd = ""; //the command
		String param = ""; //parameters
		String user;
		String remoteHost = " "; // client's ip
		int remotePort = 0; // client's tcp port
		String dir = "/";//pwd
		String rootdir = "/home/char/javawork/ftp-train/ftp/"; //root dir.checkPASS to set
		int state = 0; //user's state
		String reply; //report of the command
		int type = 0; //file type
		String requestfile = "";
		boolean isrest = false;

		int sendPort;
        InetAddress addr;
		
		public FtpHandler(DatagramPacket reciPacket) {
			
			dir = "/";
			sendPort = reciPacket.getPort();
			addr = reciPacket.getAddress();
			port = getPort();
			System.out.println(port);
			try {
				server = new DatagramSocket(port);
			} catch (SocketException e) {
				e.printStackTrace();
			}
			udpSend(""+port);
		}

		public void run() {
			String str = "";
			int parseResult; //No. of command

			try {
				state = FtpState.FS_WAIT_LOGIN; //0
				boolean finished = false;
				while (!finished) {
					str = UdpRead(); // /
					if (str == null)
						finished = true; //jump out
					else {
						parseResult = parseInput(str); // cmd=>num
						System.out.println("Command : " + cmd + " Parameter : " + param);
						System.out.print("->");
						switch (state) // user's state to check what to do
						{
						case FtpState.FS_WAIT_LOGIN:
							finished = commandUSER();
							break;
						case FtpState.FS_WAIT_PASS:
							finished = commandPASS();
							break;
						case FtpState.FS_LOGIN: {
							switch (parseResult)// key point
							{
							case -1:
								errCMD(); // gramerror
								break;
							case 2:
								finished = commandPASV();
								break;
							case 3:
								finished = commandSYST();
								break;
							case 4:
								finished = commandCDUP(); // cd ..
								break;
							case 6:
								finished = commandCWD(); // cd path
								break;
							case 7:
								finished = commandQUIT(); // quit
								break;
							case 9:
								finished = commandPORT(); // client's ip tcp's port
								break;
							case 11:
								finished = commandTYPE(); // file type set
								break;
							case 14:
								finished = commandRETR(); // get a file from the server
								break;
							case 15:
								finished = commandSTOR(); // send a file to server
								break;
							case 22:
								finished = commandABOR(); // close the datasocket
								break;
							case 23:
								finished = commandDELE(); // delete the file in server
								break;
							case 25:
								finished = commandMKD(); // make dir
								break;
							case 27:
								finished = commandLIST(); // ls
								break;
							case 26:
							case 33:
								finished = commandPWD(); // pwd
								break;
							case 32:
								finished = commandNOOP(); // correct
								break;

							}
						}
							break;

						}
					}
					System.out.println(reply);
					udpSend(reply);

				}
			} catch (Exception e) {
				System.out.println("connection reset!");
			} 
		}

		void udpSend(String sendStr)
		{
			try {

		        byte[] sendBuf;
		        sendBuf = sendStr.getBytes();
		        DatagramPacket sendPacket 
		            = new DatagramPacket(sendBuf , sendBuf.length , addr , sendPort );
		        
				server.send(sendPacket);
			} catch (SocketException e) {
				e.printStackTrace();
			}catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		String UdpRead()
		{
			String reciStr=null;
	        try {
				byte[] recvBuf = new byte[1024];
		        DatagramPacket recvPacket 
		            = new DatagramPacket(recvBuf , recvBuf.length);
	        	server.receive(recvPacket);
				reciStr = new String(recvPacket.getData() , 0 , recvPacket.getLength());
				
			} catch (IOException e) {
				e.printStackTrace();
			}
	        return reciStr;
		}
		
		public int getPort() {   
		    DatagramSocket s = null;//udp's socket just can figure out the port udp occupy
		    // test tht ports between two value  
		    int MINPORT = 10000;  
		    int MAXPORT = 65000;  
		  
		    for (; MINPORT < MAXPORT; MINPORT++) {  
		  
		        try {  
		            s = new DatagramSocket(MINPORT);  
		            s.close();  
		            return MINPORT;  
		        } catch (IOException e) {  
		            continue;  
		        }  
		  
		    }  
		    return -1;  
		}  
		
		int parseInput(String s) {
			int p = 0;
			int i = -1;
			p = s.indexOf(" ");
			if (p == -1) // no parameter
				cmd = s;
			else
				cmd = s.substring(0, p); // get the param

			if (p >= s.length() || p == -1)
				param = "";
			else
				param = s.substring(p + 1, s.length());
			cmd = cmd.toUpperCase(); // Upper

			if(cmd.equals("PASV"))
				i = 2;
			if(cmd.equals("SYST"))
				i = 3;
			if (cmd.equals("CDUP"))
				i = 4;
			if (cmd.equals("CWD"))
				i = 6;
			if (cmd.equals("QUIT"))
				i = 7;
			if (cmd.equals("PORT"))
				i = 9;
			if (cmd.equals("TYPE"))
				i = 11;
			if (cmd.equals("RETR"))
				i = 14;
			if (cmd.equals("STOR"))
				i = 15;
			if (cmd.equals("ABOR"))
				i = 22;
			if (cmd.equals("DELE"))
				i = 23;
			if (cmd.equals("MKD"))
				i = 25;
			if (cmd.equals("PWD"))
				i = 26;
			if (cmd.equals("LIST"))
				i = 27;
			if (cmd.equals("NOOP"))
				i = 32;
			if (cmd.equals("XPWD"))
				i = 33;
			return i;
		}

		int validatePath(String s) {
			File f = new File(s); // /...
			if (f.exists() && !f.isDirectory()) {
				String s1 = s.toLowerCase();
				String s2 = rootdir.toLowerCase();
				if (s1.startsWith(s2))
					return 1; //the file is existed begin with rootdir
				else
					return 0; //the file.....without the rootdir
			}
			f = new File(addTail(dir) + s);// .../...
			if (f.exists() && !f.isDirectory()) {
				String s1 = (addTail(dir) + s).toLowerCase();
				String s2 = rootdir.toLowerCase();
				if (s1.startsWith(s2))
					return 2;  
				else
					return 0; 
			}
			return 0; 
		}

		private boolean commandPASV() {
			reply = "227 Entering Passive Mode ("+ServerIP+","+(port/256)+","+(port%256)+").";
			return false;
		}
		
		private boolean commandSYST() {
			reply = "215 UNIX Type: L8";
			return false;
		}
		
		boolean commandUSER() {
			if (cmd.equals("USER")) {
				reply = "User name correct,Please input the password..";
				user = param;
				state = FtpState.FS_WAIT_PASS;
				return false;
			} else {
				reply = "Param error,user name not match..";
				return true;
			}

		}

		boolean commandPASS() {
			if (cmd.equals("PASS")) {
				reply = "User login..";
				state = FtpState.FS_LOGIN;
				System.out.println("User : " + param +  "Login..");
				System.out.print("->");
				return false;
			} else {
				reply = "Param error,password is wrong..";
				return true;
			}

		}

		void errCMD() {
			reply = "Param error";
		}

		boolean commandCDUP()// cd ..
		{
			File f = new File(dir);
			if (f.getParent() != null && (!dir.equals(rootdir)))//have father path but is not root path
			{
				dir = f.getParent();
				reply = "Correct..";
			} else {
				reply = "There is no such father path..";
			}

			return false;
		}// commandCDUP() end

		boolean commandCWD()// CWD (CHANGE WORKING DIRECTORY)
		{ 
			if(param.equals("/"))
				param = rootdir;
			else if(param.startsWith("/"))
				param = rootdir+param.substring(1, param.length());
			File f = new File(param);
			String s = "";
			String s1 = "";
			if (dir.endsWith("/"))
				s = dir;
			else{
				s = dir + "/";
			}
			File f1 = new File(s + param);

			if (f.isDirectory() && f.exists()) {
				if (param.equals("..") || param.equals("..\\")) {
					if (dir.compareToIgnoreCase(rootdir) == 0) {
						reply = "There is no such directory..";
						// return false;
					} else {
						s1 = new File(dir).getParent();
						if (s1 != null) {
							dir = s1;
							reply = "Directory change to : " + dir;
						} else
							reply = "There is no such path..";
					}
				} else if (param.equals(".") || param.equals(".\\")) {
				} else {
					dir = param;
					reply = "The directory change to : " + dir;
				}
			} else if (f1.isDirectory() && f1.exists()) {
				dir = s + param;
				reply = "The directory change to : " + dir;
			} else
				reply = "Param error..";

			return false;
		} // commandCDW() end

		boolean commandQUIT() {
			reply = "Service close the connection..";
			return true;
		}// commandQuit() end

		boolean commandPORT() {
			int p1 = 0;
			int p2 = 0;
			int[] a = new int[6];// store ip+tcp
			int i = 0; //
			try {
				while ((p2 = param.indexOf(",", p1)) != -1)// first 5 bit
				{
					a[i] = Integer.parseInt(param.substring(p1, p2));
					p2 = p2 + 1;
					p1 = p2;
					i++;
				}
				a[i] = Integer.parseInt(param.substring(p1, param.length()));// last bit
			} catch (NumberFormatException e) {
				reply = "Param error..";
				return false;
			}

			remoteHost = a[0] + "." + a[1] + "." + a[2] + "." + a[3];
			remotePort = a[4] * 256 + a[5];
			reply = "Correct..";
			return false;
		}// commandPort() end

		boolean commandLIST()
		{

				udpSend("Correct,ls works as ASCII..");
							
							File f = new File(dir);
							System.out.println(dir);
							String[] dirStructure = f.list();
							String fileType;
							String out="";
							for (int i = 0; i < dirStructure.length; i++) {
								if (dirStructure[i].indexOf(".") != -1) {
									fileType = "- "; 
								} else {
									fileType = "d "; 
								}
								out = out + fileType + dirStructure[i] + "\r\n";
							}
							udpSend(out);
							reply = "Data transport over...";

			return false;
		}// commandLIST() end

		boolean commandTYPE() 
		{
			if (param.equals("A")) {
				type = FtpState.FTYPE_ASCII;// 0
				reply = "Correct,turn to ASCII Model";
			} else if (param.equals("I")) {
				type = FtpState.FTYPE_IMAGE;// 1
				reply = "Correct,turn to BINARY Model";
			} else
				reply = "The command can not be done..";

			return false;
		}

		boolean commandRETR() {
			String fillname = dir;
			if(param.startsWith("/"))
				fillname += param.substring(1);
			else
				fillname += param;
			System.out.println(dir);
			System.out.println(addTail(dir.substring(1)));
			System.out.println(fillname);
			requestfile = fillname;
			File f = new File(requestfile);
			if (!f.exists()) {
				f = new File(fillname);
				if (!f.exists()) {
					reply = "The file is not existed..";
					return false;
				}
			}
			if (f.isDirectory()) {

			} else {
				if (type == FtpState.FTYPE_IMAGE) // bin
				{
					try {
						udpSend("Correct.open it as binary file."
								+ requestfile);
						BufferedInputStream fin = new BufferedInputStream(
								new FileInputStream(requestfile));
						byte[] buf = new byte[1024]; // target buff
						int l = 0;
						String sendStr="";
						while ((l = fin.read(buf, 0, 1024)) != -1) // buff still not full
						{
							sendStr = sendStr + new String(buf, 0, l);
						}
						udpSend(sendStr);
						fin.close();
						reply = "Data transport over..";

					} catch (Exception e) {
						e.printStackTrace();
						reply = "Request failed...";
						return false;
					}

				}
				if (type == FtpState.FTYPE_ASCII)// ascII
				{
					try {
						udpSend("Opening ASCII mode data connection for "
										+ requestfile);
						BufferedReader fin = new BufferedReader(new FileReader(
								requestfile));
						String s;
						String sendStr="";
						while ((s = fin.readLine()) != null) {
							sendStr+=s; // /???
						}
						fin.close();
						udpSend(sendStr);
						reply = "The data tansport over...";
					} catch (Exception e) {
						e.printStackTrace();
						reply = "The request failed..";
						return false;
					}
				}
			}
			return false;

		}

		boolean commandSTOR() {
			if (param.equals("")) {
				reply = "Param error..";
				return false;
			}
			requestfile = addTail(dir) + param;
			if (type == FtpState.FTYPE_IMAGE)// bin
			{
				try {
					udpSend("Opening Binary mode data connection for "
									+ requestfile);

					BufferedOutputStream fout = new BufferedOutputStream(
							new FileOutputStream(requestfile));
					byte[] buf = new byte[1024];
					int l = 0;
					String tmp = null;
					/*while ((tmp = UdpRead()) != null) {
						fout.write(tmp.getBytes(), 0, tmp.getBytes().length);
					}*/
					tmp = UdpRead();
					fout.write(tmp.getBytes(), 0, tmp.getBytes().length);
					fout.close();
					reply = "Data transport over..";
				} catch (Exception e) {
					e.printStackTrace();
					reply = "Request failed..";
					return false;
				}
			}
			if (type == FtpState.FTYPE_ASCII)// ascII
			{
				try {
					udpSend("Opening ASCII mode data connection for "
									+ requestfile);
					PrintWriter fout = new PrintWriter(new FileOutputStream(
							requestfile));
					String line = UdpRead();
					/*while ((line = UdpRead()) != null) {
						fout.println(line);
					}*/
					fout.println(line);
					fout.close();
					reply = "The data transport over...";
				} catch (Exception e) {
					e.printStackTrace();
					reply = "Request failed..";
					return false;
				}
			}
			return false;
		}

		boolean commandPWD() {
			reply = " " + dir + "  is the pwd..";
			return false;
		}

		boolean commandNOOP() {
			reply = "Correct..";
			return false;
		}

		boolean commandABOR() {
			try {
			} catch (Exception e) {
				e.printStackTrace();
				reply = "Request failed..";
				return false;
			}
			reply = "Out of service,connection closed..";
			return false;
		}

		boolean commandDELE() {
			int i = validatePath(param);
			if (i == 0) {
				reply = "Request failed.No such file or dir.";
				return false;
			}
			if (i == 1) {
				File f = new File(param);
				f.delete();
			}
			if (i == 2) {
				File f = new File(addTail(dir) + param);
				f.delete();
			}

			reply = "Request done.file deleted..";
			return false;

		}

		boolean commandMKD() {
			String s1 = param.toLowerCase();
			String s2 = rootdir.toLowerCase();
			if (s1.startsWith(s2)) {
				File f = new File(param);
				if (f.exists()) {
					reply = "Dir is existed..";
					return false;
				} else {
					f.mkdirs();
					reply = "Dir build done..";
				}
			} else {
				File f = new File(addTail(dir) + param);
				if (f.exists()) {
					reply = "Dir is existed..";
					return false;
				} else {
					f.mkdirs();
					reply = "Dir building done..";
				}
			}

			return false;
		}

		String addTail(String s) {
			if (!s.endsWith("/"))
				s = s + "/";
			return s;
		}

	}

	class FtpState {
		final static int FS_WAIT_LOGIN = 0; // wait for user name.
		final static int FS_WAIT_PASS = 1; // wait for the password.
		final static int FS_LOGIN = 2; // Login in..

		final static int FTYPE_ASCII = 0;
		final static int FTYPE_IMAGE = 1;
		final static int FMODE_STREAM = 0;
		final static int FMODE_COMPRESSED = 1;
		final static int FSTRU_FILE = 0;
		final static int FSTRU_PAGE = 1;
	}
}
