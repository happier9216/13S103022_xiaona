import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class httpServer {

	public static void main(String[] args) {
		int port = 8080;
		if (args.length > 0)
			port = Integer.valueOf(args[0]);
		new httpServer().start(port);

	}

	public void start(int port) {
		ServerSocket server;
		try {
			server = new ServerSocket(port);

			System.out.println("server start at " + port + "...........");
			while (true) {
				Socket client = server.accept();
				ServerThread serverthread = new ServerThread(client);
				serverthread.start();

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	class ServerThread extends Thread {
		Socket client;

		public ServerThread(Socket client) {
			this.client = client;
		}

		public byte[] getFileByte(String filename) throws IOException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			File file = new File(filename);
			FileInputStream fis = new FileInputStream(file);
			byte[] b = new byte[1000];
			int read;
			while ((read = fis.read(b)) != -1) {
				baos.write(b, 0, read);
			}
			fis.close();
			baos.close();
			return baos.toByteArray();
		}

		private String getQueryResource(String queryurl) {
			String queryresource = null;
			int index = queryurl.indexOf('?');
			if (index != -1) {
				queryresource = queryurl.substring(0, queryurl.indexOf('?'));
			} else
				queryresource = queryurl;

			index = queryresource.lastIndexOf("/");
			if (index + 1 == queryresource.length()) {
				queryresource = queryresource + "index.html";
			} else {
				String filename = queryresource.substring(index + 1);
				if (!filename.contains("."))
					queryresource = queryresource + ".html";
			}
			queryresource = "/home/char/javawork/ftp-train/webapp" + queryresource;
			return queryresource;

		}

		private String getHead(String queryresource) {
			String filename = "";
			int index = queryresource.lastIndexOf("/");
			filename = queryresource.substring(index + 1);
			String[] filetypes = filename.split("\\.");
			String filetype = filetypes[filetypes.length - 1];
			if (filetype.equals("html")) {
				return "HTTP/1.0200OK\n" + "Content-Type:text/html\n"
						+ "Server:myserver\n" + "\n";
			} else if (filetype.equals("jpg") || filetype.equals("gif")
					|| filetype.equals("png")) {
				return "HTTP/1.0200OK\n" + "Content-Type:image/jpeg\n"
						+ "Server:myserver\n" + "\n";
			} else
				return null;

		}

		public void run() {
			InputStream is;
			try {
				is = client.getInputStream();
				BufferedReader br = new BufferedReader(
						new InputStreamReader(is));
				int readint;
				char c;
				byte[] buf = new byte[1000];
				OutputStream os = client.getOutputStream();
				client.setSoTimeout(50);
				byte[] data = null;
				String cmd = "";
				String queryurl = "";
				int state = 0;
				String queryresource;
				String head;
				while (true) {
					readint = is.read();
					c = (char) readint;
					boolean space = Character.isWhitespace(readint);
					switch (state) {
					case 0:
						if (space)
							continue;
						state = 1;
					case 1:
						if (space) {
							state = 2;
							continue;
						}
						cmd += c;
						continue;
					case 2:
						if (space)
							continue;
						state = 3;
					case 3:
						if (space)
							break;
						queryurl += c;
						continue;
					}
					break;
				}

				queryresource = getQueryResource(queryurl);
				head = getHead(queryresource);
				
				while (true) {
					try {
						if ((readint = is.read(buf)) > 0) {
						} else if (readint < 0)
							break;
					} catch (InterruptedIOException e) {
						data = getFileByte(queryresource);
					}

					if (data != null) {
						os.write(data);
						os.close();
						break;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

}
