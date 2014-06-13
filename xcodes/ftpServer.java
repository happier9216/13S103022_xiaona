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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ftpServer {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new Thread()
		{
			public void run() {
				int port=8080;
				new httpServer().start(port);
			}
		}.start();
		new Thread()
		{
			public void run() {
				new ftpServer();
			}
		}.start();
		new Thread()
		{
			public void run() {
				new udpServer();
			}
		}.start();
	}

	public ftpServer() {
		//listen No.21 port,21port to control,20 to data trans
		ServerSocket s;
		try {
			s = new ServerSocket(21);

			int i = 0;
			for (;;) {
				Socket incoming = s.accept();
				BufferedReader in = new BufferedReader(new InputStreamReader(
						incoming.getInputStream()));
				PrintWriter out = new PrintWriter(incoming.getOutputStream(),
						true);
				out.println("220 welcome.." + ",You are the No.  " + counter + " User.");

				// server service thread.
				FtpHandler h = new FtpHandler(incoming, i);
				h.start();
				users.add(h); // add the user thread into the ArrayList
				counter++;
				i++;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		Socket ctrlSocket; // socket to contrl
		ServerSocket dataService; //pasvmodel socket
		Socket dataSocket; // data trans socket
		int port; //pasv listen port
		int id;
		String cmd = ""; // command
		String param = ""; // parameter
		String user;
		String remoteHost = " "; // client's IP
		int remotePort = 0; // client tcp port
		String dir = "/";// pwd
		String rootdir = "/home/char/javawork/ftp-train/ftp/"; 
		int state = 0; // user status
		String reply; 
		PrintWriter ctrlOutput;
		int type = 0;
		String requestfile = "";
		boolean isrest = false;

		// FtpHandler method
		public FtpHandler(Socket s, int i) {
			ctrlSocket = s;
			id = i;
			dir = "/";
		}

		// run()
		public void run() {
			String str = "";
			int parseResult; // No. of cmd

			try {
				BufferedReader ctrlInput = new BufferedReader(
						new InputStreamReader(ctrlSocket.getInputStream()));
				ctrlOutput = new PrintWriter(ctrlSocket.getOutputStream(), true);
				state = FtpState.FS_WAIT_LOGIN; 
				boolean finished = false;
				while (!finished) {
					str = ctrlInput.readLine(); // line with cmd param
					if (str == null)
						finished = true; // jump out
					else {
						parseResult = parseInput(str); // line trans to cmd and param
						System.out.println("Command : " + cmd + " Parameter : " + param);
						System.out.print("->");
						switch (state) 
						{
						case FtpState.FS_WAIT_LOGIN:
							finished = commandUSER();
							break;
						case FtpState.FS_WAIT_PASS:
							finished = commandPASS();
							break;
						case FtpState.FS_LOGIN: {
							switch (parseResult) 
							{
							case -1:
								errCMD(); 
								break;
							case 2:
								finished = commandPASV();
								break;
							case 3:
								finished = commandSYST();
								break;
							case 4:
								finished = commandCDUP(); 
								break;
							case 6:
								finished = commandCWD(); 
								break;
							case 7:
								finished = commandQUIT(); 
								break;
							case 9:
								finished = commandPORT(); 
								break;
							case 11:
								finished = commandTYPE(); 
								break;
							case 14:
								finished = commandRETR();
								break;
							case 15:
								finished = commandSTOR(); 
								break;
							case 22:
								finished = commandABOR(); 
								break;
							case 23:
								finished = commandDELE(); 
								break;
							case 25:
								finished = commandMKD(); 
								break;
							case 27:
								finished = commandLIST(); 
								break;
							case 26:
							case 33:
								finished = commandPWD(); 
								break;
							case 32:
								finished = commandNOOP(); 
								break;

							}
						}
							break;

						}
					}
					System.out.println(reply);
					ctrlOutput.println(reply);
					ctrlOutput.flush();

				}
				ctrlSocket.close();
			} catch (Exception e) {
				System.out.println("connection reset!");
			} 
		}

		// parseInput method
		int parseInput(String s) {
			int p = 0;
			int i = -1;
			p = s.indexOf(" ");
			if (p == -1) 
				cmd = s;
			else
				cmd = s.substring(0, p); 

			if (p >= s.length() || p == -1)
				param = "";
			else
				param = s.substring(p + 1, s.length());
			cmd = cmd.toUpperCase(); 

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

		// validatePath
		int validatePath(String s) {
			File f = new File(s); 
			if (f.exists() && !f.isDirectory()) {
				String s1 = s.toLowerCase();
				String s2 = rootdir.toLowerCase();
				if (s1.startsWith(s2))
					return 1;
				else
					return 0;
			}
			f = new File(addTail(dir) + s);
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
			// TODO Auto-generated method stub
			try {
				dataService = new ServerSocket(0);
				port = dataService.getLocalPort();
				reply = "227 Entering Passive Mode ("+ServerIP+","+(port/256)+","+(port%256)+").";
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return false;
		}
		
		private boolean commandSYST() {
			reply = "215 UNIX Type: L8";
			return false;
		}
		
		// commandUSER
		boolean commandUSER() {
			if (cmd.equals("USER")) {
				reply = "331 welcome,please input the password.";
				user = param;
				state = FtpState.FS_WAIT_PASS;
				return false;
			} else {
				reply = "501 param error,there is no such user.";
				return true;
			}

		}

		// commandPASS 
		boolean commandPASS() {
			if (cmd.equals("PASS")) {
				reply = "230 User Login.";
				state = FtpState.FS_LOGIN;
				System.out.println("New Message : User :" + param + "From: "
							+ remoteHost + "Login");
				System.out.print("->");
				return false;
			} else {
				reply = "501 param error, password error.";
				return true;
			}

		}

		void errCMD() {
			reply = "500 param error.";
		}

		boolean commandCDUP()
		{
			File f = new File(dir);
			if (f.getParent() != null && (!dir.equals(rootdir)))
			{
				dir = f.getParent();
				reply = "200 correct.";
			} else {
				reply = "550 no father path.";
			}

			return false;
		}

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
			else
				s = dir + "/";
			File f1 = new File(s + param);

			if (f.isDirectory() && f.exists()) {
				if (param.equals("..") || param.equals("..\\")) {
					if (dir.compareToIgnoreCase(rootdir) == 0) {
						reply = "550 no such path.";
						// return false;
					} else {
						s1 = new File(dir).getParent();
						if (s1 != null) {
							dir = s1;
							reply = "250 dir change to : " + dir;
						} else
							reply = "550 no such path.";
					}
				} else if (param.equals(".") || param.equals(".\\")) {
				} else {
					dir = param;
					reply = "250 dir change to:" + dir;
				}
			} else if (f1.isDirectory() && f1.exists()) {
				dir = s + param;
				reply = "250 dir change to:" + dir;
			} else
				reply = "501 param error.";

			return false;
		} // commandCDW() end

		boolean commandQUIT() {
			reply = "221 connection close.";
			return true;
		}// commandQuit() end

		boolean commandPORT() {
			int p1 = 0;
			int p2 = 0;
			int[] a = new int[6];
			int i = 0; //
			try {
				while ((p2 = param.indexOf(",", p1)) != -1)
				{
					a[i] = Integer.parseInt(param.substring(p1, p2));
					p2 = p2 + 1;
					p1 = p2;
					i++;
				}
				a[i] = Integer.parseInt(param.substring(p1, param.length()));
			} catch (NumberFormatException e) {
				reply = "501 param error";
				return false;
			}

			remoteHost = a[0] + "." + a[1] + "." + a[2] + "." + a[3];
			remotePort = a[4] * 256 + a[5];
			reply = "200 correct.";
			return false;
		}// commandPort() end

		boolean commandLIST()
		{
			try {
//				class listThread extends Thread{
//					public void run() {
//						try {
				ctrlOutput.println("150 everything normal,ls works as  ASCII style.");
				ctrlOutput.flush();
							dataSocket = dataService.accept();
							PrintWriter dout = new PrintWriter(
									dataSocket.getOutputStream(), true);
							
							File f = new File(dir);
							String[] dirStructure = f.list();
							String fileType;
							String out="";
							for (int i = 0; i < dirStructure.length; i++) {
								if (dirStructure[i].indexOf(".") != -1) {
									fileType = "- "; 
								} else {
									fileType = "d ";
								}
								out = out + dirStructure[i] + "\r\n";
							}
							dout.print("\b\r\n");
							dout.print(out);
							dout.close();
							dataSocket.close();
							reply = "226 data trans over.";
			} catch (Exception e) {
				e.printStackTrace();
				reply = "451 Requested action aborted: local error in processing";
				return false;
			}

			return false;
		}// commandLIST() end

		boolean commandTYPE() 
		{
			if (param.equals("A")) {
				type = FtpState.FTYPE_ASCII;// 0
				reply = "200 turn to ASCII model.";
			} else if (param.equals("I")) {
				type = FtpState.FTYPE_IMAGE;// 1
				reply = "200 turn to BINARY model.";
			} else
				reply = "504 param strange.";

			return false;
		}

		// connamdRETR 
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
					reply = "550 no such file.";
					return false;
				}
			}
			if (f.isDirectory()) {

			} else {
				if (type == FtpState.FTYPE_IMAGE) // bin
				{
					try {
						ctrlOutput.println("150 open as binary model.  "
								+ requestfile);
						dataSocket = dataService.accept();
						BufferedInputStream fin = new BufferedInputStream(
								new FileInputStream(requestfile));
						PrintStream dataOutput = new PrintStream(
								dataSocket.getOutputStream(), true);
						byte[] buf = new byte[1024]; 
						int l = 0;
						while ((l = fin.read(buf, 0, 1024)) != -1) 
						{
							dataOutput.write(buf, 0, l); 
						}
						fin.close();
						dataOutput.close();
						dataSocket.close();
						reply = "226 data transport over.";

					} catch (Exception e) {
						e.printStackTrace();
						reply = "451 request failed.";
						return false;
					}

				}
				if (type == FtpState.FTYPE_ASCII)// ascII
				{
					try {
						ctrlOutput
								.println("150 Opening ASCII mode data connection for "
										+ requestfile);
						dataSocket = dataService.accept();
						BufferedReader fin = new BufferedReader(new FileReader(
								requestfile));
						PrintWriter dataOutput = new PrintWriter(
								dataSocket.getOutputStream(), true);
						String s;
						while ((s = fin.readLine()) != null) {
							dataOutput.println(s); // /???
						}
						fin.close();
						dataOutput.close();
						dataSocket.close();
						reply = "226 data transport over.";
					} catch (Exception e) {
						e.printStackTrace();
						reply = "451 request failed.";
						return false;
					}
				}
			}
			return false;

		}

		// commandSTOR 
		boolean commandSTOR() {
			if (param.equals("")) {
				reply = "501 param error.";
				return false;
			}
			requestfile = addTail(dir) + param;
			if (type == FtpState.FTYPE_IMAGE)// bin
			{
				try {
					ctrlOutput
							.println("150 Opening Binary mode data connection for "
									+ requestfile);
					dataSocket = new Socket(remoteHost, remotePort,
							InetAddress.getLocalHost(), 20);
					BufferedOutputStream fout = new BufferedOutputStream(
							new FileOutputStream(requestfile));
					BufferedInputStream dataInput = new BufferedInputStream(
							dataSocket.getInputStream());
					byte[] buf = new byte[1024];
					int l = 0;
					while ((l = dataInput.read(buf, 0, 1024)) != -1) {
						fout.write(buf, 0, l);
					}
					dataInput.close();
					fout.close();
					dataSocket.close();
					reply = "226 data transport over.";
				} catch (Exception e) {
					e.printStackTrace();
					reply = "451 request failed.";
					return false;
				}
			}
			if (type == FtpState.FTYPE_ASCII)// ascII
			{
				try {
					ctrlOutput
							.println("150 Opening ASCII mode data connection for "
									+ requestfile);
					dataSocket = new Socket(remoteHost, remotePort,
							InetAddress.getLocalHost(), 20);
					PrintWriter fout = new PrintWriter(new FileOutputStream(
							requestfile));
					BufferedReader dataInput = new BufferedReader(
							new InputStreamReader(dataSocket.getInputStream()));
					String line;
					while ((line = dataInput.readLine()) != null) {
						fout.println(line);
					}
					dataInput.close();
					fout.close();
					dataSocket.close();
					reply = "226 data transport over.";
				} catch (Exception e) {
					e.printStackTrace();
					reply = "451 request failed.";
					return false;
				}
			}
			return false;
		}

		boolean commandPWD() {
			reply = "257 " + dir + "is the pwd.";
			return false;
		}

		boolean commandNOOP() {
			reply = "200 correct.";
			return false;
		}

		boolean commandABOR() {
			try {
				dataSocket.close();
			} catch (Exception e) {
				e.printStackTrace();
				reply = "451 request failed.";
				return false;
			}
			reply = "421 out of service.";
			return false;
		}

		boolean commandDELE() {
			int i = validatePath(param);
			if (i == 0) {
				reply = "550 no such file,or dir,or something.";
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

			reply = "250 deleted the file.";
			return false;

		}

		boolean commandMKD() {
			String s1 = param.toLowerCase();
			String s2 = rootdir.toLowerCase();
			if (s1.startsWith(s2)) {
				File f = new File(param);
				if (f.exists()) {
					reply = "550 already has that dir.";
					return false;
				} else {
					f.mkdirs();
					reply = "250 dir build succsessfully.";
				}
			} else {
				File f = new File(addTail(dir) + param);
				if (f.exists()) {
					reply = "550 already has that dir.";
					return false;
				} else {
					f.mkdirs();
					reply = "250 dir build successfully.";
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
		final static int FS_WAIT_LOGIN = 0;
		final static int FS_WAIT_PASS = 1;
		final static int FS_LOGIN = 2; 

		final static int FTYPE_ASCII = 0;
		final static int FTYPE_IMAGE = 1;
		final static int FMODE_STREAM = 0;
		final static int FMODE_COMPRESSED = 1;
		final static int FSTRU_FILE = 0;
		final static int FSTRU_PAGE = 1;
	}

}
