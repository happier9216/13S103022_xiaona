import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class xnClient{
	public static void main(String[] args){
		new xnClient();
	}

	private String dir;
	private DatagramSocket client;
	private String serverip = "";
	private DatagramPacket initPacket;
	private int port;
	private InetAddress addr;

	public xnClient(){
		initData();
		serverip = "192.168.140.6";
		try {
			addr = InetAddress.getByName(serverip);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		String rec = null;
		udpsent("hello");
		rec = udpread();
		port = Integer.parseInt(rec);
		String infostr = "port is changed to :" + rec;
		System.out.println(infostr);
		udpsent("USER -XiaoNa-");
		rec = udpread();
		System.out.println(rec);
			
		udpsent("PASS -13S103022-");
		rec = udpread();
		System.out.println(rec);
		
		udpsent("cwd /");
		udpread();

		winput();

		udpsent("quit");
		System.out.println(udpread());
	}

	public void initData(){
		port = 5050;
		dir = "/";
		try{
			client = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	public void udpsent(String sendStr){
		try {
			byte[] sendBuf;
			sendBuf = sendStr.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendBuf,sendBuf.length,addr,port);
			client.send(sendPacket);
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String udpread(){
		String recStr = null;
		try {
			byte[] recBuf = new byte[1024];
			DatagramPacket recPacket = new DatagramPacket(recBuf,recBuf.length);
			client.receive(recPacket);
			recStr = new String(recPacket.getData(),0,recPacket.getLength());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return recStr;
	}

	public void winput(){
		String strin = "";
		String rec = null;
		while(!strin.equals("#")){
			System.out.println("Please Input the command line : ");
			Scanner input = new Scanner(System.in);
			strin = input.nextLine();
			strin = strin.toUpperCase();
			switch(strin) {
			case "TYPE":
				System.out.println("You've entered the TYPE cmdline.Please input the cmd like eg. type A/I.");
				strin = input.nextLine();
				udpsent(strin);
				rec = udpread();
				System.out.println(rec);
				break;
			case "LIST":
				udpsent(strin);
				udpread();
				rec = udpread();
				String[] items = rec.split("\r\n");
				for(int i = 0;i < items.length;i++){
					if(items[i].startsWith("d")){
						System.out.println("/"+items[i].substring(2)+"/");
					}else {
						System.out.println("/"+items[i].substring(2));
					}
				}
				rec = udpread();
				break;
			case "CWD":
				System.out.println("You've entered the cd cmdline.Please input the cmd like eg. cwd //..");
				strin = input.nextLine();
				udpsent(strin);
				rec = udpread();
				break;
			case "CDUP":
				udpsent(strin);
				rec = udpread();
			case "MKD":
				System.out.println("You've entered the mkdir commandline.please input the cmd like mkd dir..");
				strin = input.nextLine();
				udpsent(strin);
				rec = udpread();
				System.out.println(rec);
				break;
			case "STOR":
				System.out.println("You've entered the store file cmdline.please input the cmd like stor file..");
				strin = input.nextLine();
				udpsent(strin);
				rec = udpread();
				System.out.println(rec);
				System.out.println("Please input the context..");
				strin = input.nextLine();
				udpsent(strin);
				rec = udpread();
				System.out.println(rec);
				break;
			case "DELE":
				System.out.println("You've entered the del cmdline.please input the cmd like dele filename..");
				strin = input.nextLine();
				udpsent(strin);
				rec = udpread();
				System.out.println(rec);
				break;
			case "PWD":
				udpsent(strin);
				rec = udpread();
				System.out.println(rec);
				break;
			case "RETR":
				System.out.println("You've entered the get cmdline.please input the cmd like retr filename..");
				strin = input.nextLine();
				udpsent(strin);
				rec = udpread();
				System.out.println(rec);
				System.out.println(udpread());
				break;
			default:
				break;
			}
		}
	}
}
