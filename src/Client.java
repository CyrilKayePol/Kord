import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Scanner;

@SuppressWarnings("unused")
public class Client extends Thread{
	
	private Node myNode;
	private int myPort, hostPort;
	private String myIP, hostIP;
	
	private DatagramSocket clientSocket;
	private Protocol pro;
	private Object input, output;
	private String ip;
	private int port;
	private byte[] buf;
	private boolean largest;
	private Node idNode, newSuccessor;
	private Scanner scan;
	private Node fileIDSuccessor, fileIDNode;
	private BigInteger fileID;
	
	private SendFile sendFile;
	private StoreFile storeFile;
	private String path, fileName;
	private FileEvent event;
	private boolean isDownload;
	
	private Node pred, suc;
	
	private Finger[] fingers;
	private int univIndex, localIndex;
	private Node fingerNode, fingerSuccessor;
	
	public Client() {

		initialize();
		joinRing();
		start();
		waitForMessage();
	}
	
	private void initialize() {
		univIndex = 0;
		localIndex = 0;
		fingerNode = null;
		fingerSuccessor = null;
		fingers = new Finger[3];
		input = null;
		fileIDSuccessor = null;
		fileID = null;
		fileIDNode = null;
		storeFile = new StoreFile();
		scan = new Scanner(System.in);
		hostPort = 8888;
		hostIP = "localhost";
		myPort = 5100;
		try {
			myIP = InetAddress.getLocalHost().getHostAddress();
			clientSocket = new DatagramSocket(myPort);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		myNode = new Node(myIP, myPort);
		myNode.setID(new BigInteger("180"));
		//myNode.setID(computeID(myIP+""+myPort));
		pro = new Protocol(clientSocket, hostIP, hostPort);
	}
	
	public void run() {
		boolean isContinue = true;
		while(isContinue) {
			menu();
		}
	}
	
	private void menu() {
		System.out.println("----CHORD----");
		System.out.println("[1] view information");
		System.out.println("[2] download file");
		System.out.println("[3] upload file");
		System.out.println("[4] exit");
		System.out.println("enter choice:");
		String choice = scan.nextLine();
		
		System.out.println();
		switch(choice) {
		case "1":
			viewInfo();
			break;
		case "2":
			downloadFile();
			break;
		case "3":
			uploadFile();
			break;
		case "4":
			exitRing();
			break;
		default:
			wrongInput();
			break;
		}
	}
	
	private void viewInfo() {
		System.out.println("============node info=============");
		System.out.println("Predecessor: "+myNode.getPredecessor());
		System.out.println("ID:          "+myNode.getID());
		System.out.println("Successor:   "+myNode.getSuccessor());
		System.out.println("==================================");
	}
	
	private void downloadFile() {
		isDownload = true;
		System.out.println(">>>>>>>download a file<<<<<<<");
		System.out.print("Enter file name: ");
		fileName = scan.nextLine();
		System.out.println();
		fileID = computeID(fileName);
		System.out.println("file id: "+fileID);
		fileID = new BigInteger("12");
		fileIDNode = new Node(fileID, myNode.getIP(), myNode.getPort());
		findSuccessor(fileID, "found fileID successor");
		System.out.println(">>>>>>>>>>>>><<<<<<<<<<<<<<");
	}
	
	private void uploadFile() {
		isDownload = false;
		System.out.println(">>>>>>>upload a file<<<<<<<");
		System.out.print("Enter file's path: ");
		path = scan.nextLine();
		System.out.println();
		String fileName = extractFileName(path);
		System.out.println("file to upload: "+fileName);
		fileID = computeID(fileName);
		System.out.println("file id: "+fileID);
		fileID = new BigInteger("12");
		fileIDNode = new Node(fileID, myNode.getIP(), myNode.getPort());
		findSuccessor(fileID, "found fileID successor");
		System.out.println(">>>>>>>>>>>>><<<<<<<<<<<<<<");
	}
	
	private String extractFileName(String path) {
		int index = path.lastIndexOf("/");
		String fileName = path.substring(index+1);
		return fileName;
	}
	
	private BigInteger computeID(String text){
		MessageDigest digest;
		byte[] hash = null;
		try {
			digest = MessageDigest.getInstance("SHA-1");
			hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
		} catch (Exception e) {
			
			e.printStackTrace();
		}
		
		StringBuffer hex = new StringBuffer();
		
		for(int i = 0; i< hash.length; i++) {
			String s = Integer.toHexString(0xff & hash[i]);
			if(hex.length() == 1) hex.append('0');
			hex.append(s);
		}
		
		return (new BigInteger(hex.toString(),16));
		
	}
	
	private void exitRing() {
		System.out.println("[exit ring]");
		findNode(myNode.getPredecessor(), "my predecessor");
		invokeHostNotify("bye");
	}
	
	private void wrongInput() {
		System.out.println("[Invalid input! Please try again.]");
	}
	public void waitForMessage() {
		while(true) {
			receive();
			
			if(input instanceof Message) {
					Message m = (Message) input;
					Node n = m.getNode();
					String[] s = m.getMsg().split("_");
					//System.out.println("s[0] = "+s[0]);
					if(m.getMsg().equals("update predecessor")) {

						BigInteger old = myNode.getPredecessor();
						myNode.setPredecessor(n.getID());

						if(old != null) {
							newSuccessor = n;
						
							findNode(old, "successor");
							
						}

						
					}else if(m.getMsg().equals("node of id-successor")){
						Node oldNode = m.getNode();
						sendSetup(oldNode, newSuccessor, "update successor");
					}else if(m.getMsg().equals("update successor")) {
						
						BigInteger old = myNode.getSuccessor();
						myNode.setSuccessor(n.getID());

						if(old != null) {
							sendSetup(n, myNode, "update predecessor");
						}
						
						invokeHostNotify("increment");
					}else if(m.getMsg().equals("real node")) {
						n = m.getNode();
						sendSetup(n, myNode, "find my successor");
						
					}else if(m.getMsg().equals("find my successor") ||
							m.getMsg().equals("your turn to find successor")){
						n = m.getNode();
						System.out.println("askjd");
						findSuccessor(n.getID(), "found successor");
					}else if(m.getMsg().equals("node of id-found successor")){
						n = m.getNode();
	
						notifySuccessor(n);
						notifyNewNode(n);
					}else if(m.getMsg().equals("node of id-find successor")){
						n = m.getNode();
						sendSetup(n, idNode, "your turn to find successor");
					}else if(m.getMsg().equals("node of id-for idNode")){
						n = m.getNode();
						idNode = n;
					}else if(m.getMsg().equals("node of id-get successor of successor")){
						n = m.getNode();
						findNode(n.getSuccessor(),"found successor");
					}else if(m.getMsg().equals("node of id-found fileID successor")){
						fileIDSuccessor = m.getNode();
						sendSetup(fileIDNode, fileIDSuccessor, "the successor of fileID");
					}else if(m.getMsg().equals("node of id-find fileID successor")){
						n = m.getNode();
						sendSetup(n, fileIDNode, "your turn to find fileID successor");
					}else if(m.getMsg().equals("your turn to find fileID successor")){
						n = m.getNode();
						fileIDNode = n;
						fileID = n.getID();
						findSuccessor(fileID, "found fileID successor");
					}else if(m.getMsg().equals("the successor of fileID")){
						fileIDSuccessor = m.getNode();
						if(!isDownload) {
							sendFile = new SendFile(path, "files/");
							event = sendFile.getFileEvent();
							sendFileSetup(fileIDSuccessor, event);
						}else {
							sendSetup(fileIDSuccessor, myNode, "download file_"+fileName);
						}
					}else if(s[0].equals("download file")){
						n = m.getNode();
						String name = s[1];
						System.out.println("file neym = "+name);
						sendFile = new SendFile("files/"+name, "C:/Users/Kaye/Desktop/");
						event = sendFile.getFileEvent();
						sendFileSetup(n, event);
					}else if(m.getMsg().equals("node of id-my predecessor")){
						pred = m.getNode();
						findNode(myNode.getSuccessor(), "my successor");
					}else if(m.getMsg().equals("node of id-my successor")){
						suc = m.getNode();
						sendSetup(pred, suc, "your new successor after i exit");
						sendSetup(suc, pred, "your new predecessor after i exit");
						System.out.println("exiting ...");
						System.exit(0);
					}else if(m.getMsg().equals("your new successor after i exit")){
						n = m.getNode();
						myNode.setSuccessor(n.getID());
						invokeHostNotify("set exitCon");
					}else if(m.getMsg().equals("your new predecessor after i exit")){
						n = m.getNode();
						myNode.setPredecessor(n.getID());
					}else if(m.getMsg().equals("node of id-found finger's successor")){
						fingerSuccessor = m.getNode();
						//System.out.println("HERE inside node of id-found finger's successor");
						univIndex = 0;
						sendSetup(fingerNode, fingerSuccessor, "the successor of finger");
					}else if(s[0].equals("node of id-find finger's successor")){
						//System.out.println("L E T "+m.getNode().getID()+" find successor");
						n = m.getNode();
						sendSetup(n, fingerNode, "your turn to find finger's successor_"+univIndex);
					}else if(s[0].equals("your turn to find finger's successor")){
						fingerNode = m.getNode();
						univIndex = Integer.parseInt(s[1]);
						findSuccessor(fingerNode.getID(), "found finger's successor_"+univIndex);
					}else if(m.getMsg().equals("the successor of finger")){
						fingerSuccessor = m.getNode();
						fingers[localIndex] = new Finger(""+fingerNode.getID(), ""+fingerSuccessor.getID());
						System.out.println("localIndex = "+localIndex);
						fingers[localIndex].print();
						localIndex+=1;
						
						if(localIndex < fingers.length) {
							populateFingerTable();
						}else {
							doneUpdating();
							localIndex = 0;
						}
					}else if(m.getMsg().equals("update your finger table")){
						localIndex = 0;
						//System.out.println("FIXING FINGERS...");
						populateFingerTable();
					}else {
						pro.protocolProcedure(input);
					}
				}else if(input instanceof FileEvent) {
					FileEvent fileEvent = (FileEvent) input;
					if (fileEvent.getStatus().equalsIgnoreCase("Error")) {
						System.out.println("\n[Failed to download or upload file]");
						
					}else {
						storeFile.createAndWriteFile(fileEvent);
					}
					
				}
				
			}
			
		}
	
	private void sendSetup(Node n, Node nodeToSend, String msg) {
		setReceiverInfo(n);
		pro.setReceiverIP(ip);
		pro.setReceiverPort(port);
		output = new Message(nodeToSend, msg);
		pro.setOutput(output);
		pro.send();
	}
	
	private void sendFileSetup(Node n, FileEvent event) {
		setReceiverInfo(n);
		pro.setReceiverIP(ip);
		pro.setReceiverPort(port);
		output = event;
		pro.setOutput(output);
		pro.send();
	}
	private void joinRing() {
		pro.setReceiverIP(hostIP);
		pro.setReceiverPort(hostPort);
		pro.protocolProcedure(new Message(myNode, "start"));
		receive();
		pro.protocolProcedure(input);
	}
	
	private void receive() {
		buf = new byte[10240000];

		DatagramPacket packet = new DatagramPacket(buf, buf.length);
		try {
			clientSocket.receive(packet);
			byte[] data = packet.getData();
			ByteArrayInputStream in = new ByteArrayInputStream(data);
			ObjectInputStream is = new ObjectInputStream(in);
				
			input = is.readObject();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void setReceiverInfo(Node n) {
		ip = n.getIP();
		port = n.getPort();
	}
	
	public void findSuccessor(BigInteger idN, String msg) {
		System.out.println("the fuck?");
		String[] txt = msg.split("_");
		if(txt.length>1) {
			univIndex = Integer.parseInt(txt[1]);
		}
		
		if(txt[0].equals("found successor")) {
			findNode(idN, "for idNode");
		}
		
		String string = setParams(txt[0]);
		System.out.println("idN = "+idN);
		BigInteger idSuccessor = myNode.getSuccessor();
		
		System.out.println("idSuccessor = "+idSuccessor);
		System.out.println("id = "+myNode.getID());
		if(isIDNinRange(idN, idSuccessor)) {
			
			if(-1 == idSuccessor.compareTo(idN)) {
				System.out.println("idSuccessor < idN");
				
				if(largest) {

					System.out.println("found successor of "+idN);
					findNode(idSuccessor, txt[0]);
					System.out.println("asking for node of id of "+idSuccessor);
					
				}else {
					
					closestPreceedingNode(string, idN);
				}
			}else {
				System.out.println("idN >= idSuccessor");
				System.out.println("found successor of "+idN);
				findNode(idSuccessor, txt[0]);
				System.out.println("asking for node of id of "+idSuccessor);
			}
		}else {
			System.out.println("they UNDERSTAND");
			closestPreceedingNode(string, idN);
		}
	}

	private String setParams(String msg) {
		if(msg.equals("found successor")) {
			return "find successor";
		}else if(msg.equals("found fileID successor")) {
			return "find fileID successor";
		}else if(msg.equals("found finger's successor")){
			//System.out.println("HJSDJKFGAHKJSDGF");
			return "find finger's successor_"+univIndex;
		}else{
			return null;
		}
	}
	private boolean isIDNinRange (BigInteger idN, BigInteger idSuccessor){
	
		if(-1 == myNode.getID().compareTo(idSuccessor)) {
			System.out.println("id < idSuccessor");
			if(1== idN.compareTo(myNode.getID()) && (-1 == idN.compareTo(idSuccessor) ||
					0 == idN.compareTo(idSuccessor))) {
				return true;
			}
		}else if(1 == myNode.getID().compareTo(idSuccessor)) {
			System.out.println("id > idSuccessor");
			BigInteger temp = myNode.getID().mod(idN);
			System.out.println("temp = "+temp);
			if(-1 == temp.compareTo(idSuccessor) || 0 == temp.compareTo(idSuccessor)) {
				System.out.println("temp < idSuccessor");
				largest = false;
				return true;
			}else if( 0 == temp.compareTo(myNode.getID())){
				largest = true;
				return true;
			}else {
				System.out.println("set largest");
				largest = true;
				return false;
			}
			
		}else if(0 == myNode.getID().compareTo(idSuccessor)) {
			largest = true;
			return true;
		}
		return false;
	}

	private void closestPreceedingNode(String msg, BigInteger bigin) {
		System.out.println("ask my successor to find id instead = "+msg);
		int a=0;
		if(!msg.equals("your turn to find finger's successor") || (myNode.getSuccessor() != null && myNode.getPredecessor()!=null)) {
			
			for(a = 0;a<fingers.length;a++) {
				if(isNIDrange(fingers[a].getKey(), bigin)) {
					System.out.println("short cutu ===========");
					findNode(fingers[a].getKeySuccessor(), msg);
					break;
				}else {
					if(a == fingers.length-1) {
						System.out.println("short cut ************");
						findNode(myNode.getSuccessor(), msg);
					}
				}
			}
		}else {
			System.out.println("short cut >>>>>>>>>>>>>>>");
			findNode(myNode.getSuccessor(), msg);
		}
		
	}
	
	private boolean isNIDrange(BigInteger fing, BigInteger bigin) {
		if(-1 == myNode.getID().compareTo(fing)) {
			if(-1 == fing.compareTo(bigin) ) {
				return true;
			}
		}
		return false;
	}
	
	private void findNode(BigInteger big, String msg) {
		pro.setReceiverIP(hostIP);
		pro.setReceiverPort(hostPort);
		output = new Message(myNode, "asking for node of id_"+big+"_"+msg);
		pro.setOutput(output);
		pro.send();
	}
	
	private void invokeHostNotify(String msg) {
		pro.setReceiverIP(hostIP);
		pro.setReceiverPort(hostPort);
		output = new Message(myNode, msg);
		pro.setOutput(output);
		pro.send();
	}
	
	private void doneUpdating() {
		pro.setReceiverIP(hostIP);
		pro.setReceiverPort(hostPort);
		output = new Message(myNode, "done updating finger table");
		pro.setOutput(output);
		pro.send();
	}
	private void notifySuccessor(Node n) {	
		sendSetup(n, idNode, "update predecessor");
	}
	
	private void notifyNewNode(Node n) {
	
		sendSetup(idNode,n, "update successor");
	}
	
	private void populateFingerTable() {
		//System.out.println("havana");
		BigInteger temp = computeFinger(localIndex);
		//System.out.println("fingerID = "+temp);
		fingerNode = new Node(temp, myNode.getIP(), myNode.getPort());
		findSuccessor(temp, "found finger's successor_"+localIndex);
		
	} 
	
	private BigInteger computeFinger(int i) {
		BigInteger big = new BigInteger("0");
		BigInteger two = new BigInteger("2");
		big = big.add(myNode.getID().add(two.pow(i)));
		big = big.mod(two.pow(160));
		
		
		return big;
	}
	public static void main(String[] args) {
		new Client();
	}
	
	
}
