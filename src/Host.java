import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

@SuppressWarnings("unused")
public class Host extends Thread{
	
	private static ArrayList<Node> connectedNodes;
	private Node myNode;
	private String myIP;
	private int myPort;
	private Protocol pro;
	
	private static DatagramSocket hostSocket;
	private byte[] buf;
	private Object input,output;
	
	private String ip;
	private int port;
	private DatagramPacket packet;
	private Node idNode;
	private boolean largest;
	
	private static int oldSize, newSize;
	private Scanner scan;
	private BigInteger fileID;
	private Node fileIDSuccessor, fileIDNode;
	
	private SendFile sendFile;
	private StoreFile storeFile;
	private String path, fileName;
	private FileEvent event;
	private boolean isDownload;
	private Node pred;
	private Node suc;
	
	private Finger[] fingers;
	private int univIndex, localIndex;
	private Node fingerNode, fingerSuccessor;
	
	private int val;
	private static int ind;
	
	public Host() {
		initialize();
		createRing();
		populateFingerTable();
		start();
		waitForMessage();
	}
	
	
	private void initialize() {
		val = 0;
		univIndex = 0;
		localIndex = 0;
		fingerNode = null;
		fingerSuccessor = null;
		fingers = new Finger[3];
		fileID = null;
		fileIDSuccessor = null;
		fileIDNode = null;
		storeFile = new StoreFile();
		scan = new Scanner(System.in);
		connectedNodes = new ArrayList<Node>();
		myPort = 8888;	
		try {
			myIP = InetAddress.getLocalHost().getHostAddress();
			hostSocket = new DatagramSocket(myPort);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		buf = new byte[100000];
		
		input = null;
		
		pro = new Protocol(hostSocket, ip, port);
		
	}
	
	public static void periodicallyNotify() {
		if(oldSize < newSize) {
			notifyMembers();		
			oldSize = newSize;
		}else if(oldSize > newSize) {
			oldSize = newSize-1;
			notifyMembers();
			oldSize = newSize;
			
		}
	}

	private static void notifyMembers() {
		
				ind = oldSize;
				System.out.println("\nIND -->"+ind);
				Node temp = connectedNodes.get(ind);
				System.out.println("xxxxxxxxxx"+temp.getID());
				sendSetup2(temp, null, "update your finger table");
	}
	
	private static void sendSetup2(Node n, Node nodeToSend, String msg) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			ObjectOutputStream os = new ObjectOutputStream(outputStream);
			os.writeObject(new Message(nodeToSend, msg));
		} catch (Exception e) {
			e.printStackTrace();
		}
		byte[] data = outputStream.toByteArray();
		
		 try {
			 DatagramPacket sendPacket = 
					 new DatagramPacket(data, data.length, InetAddress.getByName(n.getIP()), n.getPort());
			 hostSocket.send(sendPacket);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	private void createRing() {
		myNode = new Node(myIP, myPort);
		myNode.setID(new BigInteger("17"));
		//myNode.setID(computeID(myIP+""+myPort));
		
		myNode.create();
		connectedNodes.add(myNode);
		oldSize = connectedNodes.size();
		newSize = oldSize;
		pro.setMyNode(myNode);
		
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
		fileID = new BigInteger("60");
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
		pred = findNode(myNode.getPredecessor());
		suc = findNode(myNode.getSuccessor());
		sendSetup(pred, suc, "your new successor after i exit");
		sendSetup(suc, pred, "your new predecessor after i exit");
		System.out.println("exiting ...");
		System.exit(0);
	}
	
	private void wrongInput() {
		System.out.println("[Invalid input! Please try again.]");
	}
	private void waitForMessage() {
		while(true) {
			receive();
				if(input instanceof Message) {
					Message m = (Message) input;
					
					Node n;
					String[] s = m.getMsg().split("_");
					if(s[0].equals("asking for node of id")) {
						n = m.getNode();
						
						Node result = findNode(new BigInteger(s[1]));
						//System.out.println("[[[node of id-"+s[2]);
						//System.out.println("send to = "+n.getPort());
						sendSetup(n,result,"node of id-"+s[2]);
					} else if(m.getMsg().equals("update predecessor")) {
						n = m.getNode();
						BigInteger old = myNode.getPredecessor();
						myNode.setPredecessor(n.getID());
						if(old != null) {
							Node oldNode = findNode(old);
							sendSetup(oldNode,n,"update successor");
						}

					}else if(m.getMsg().equals("update successor")) {
						n = m.getNode();
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
						findSuccessor(n.getID(), "found successor");
					}else if(m.getMsg().equals("node of id-found successor")){
						n = m.getNode();
						notifySuccessor(n);
						notifyNewNode(n);
					}else if(m.getMsg().equals("node of id-find successor")){
						n = m.getNode();
						setReceiverInfo(n);
						sendSetup(n, idNode, "your turn to find successor");
					}else if(m.getMsg().equals("node of id-get successor of successor")){
						n = m.getNode();
						
						sendSetup(myNode, myNode, "asking for node of id_"+n.getSuccessor()+"_found successor");
					}else if(m.getMsg().equals("node of id-found fileID successor")){
						fileIDSuccessor = m.getNode();
						sendSetup(fileIDNode, fileIDSuccessor, "the successor of fileID");
					}else if(m.getMsg().equals("node of id-find fileID successor")){
						n = m.getNode();
						System.out.println("find fileID successor");
						sendSetup(n, fileIDNode, "your turn to find fileID successor");
					}else if(m.getMsg().equals("your turn to find fileID successor")){
						n = m.getNode();
						fileIDNode = n;
						fileID = n.getID();
						findSuccessor(fileID, "found fileID successor");
					}else if(m.getMsg().equals("the successor of fileID")){
						fileIDSuccessor = m.getNode();
						System.out.println("found the successor of fileID="+fileID+": "+fileIDSuccessor.getID());
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
					}else if(m.getMsg().equals("your new successor after i exit")){
						n = m.getNode();
						myNode.setSuccessor(n.getID());
						invokeHostNotify("set exitCon");
					}else if(m.getMsg().equals("your new predecessor after i exit")){
						n = m.getNode();
						myNode.setPredecessor(n.getID());
					}else if(m.getMsg().equals("node of id-found finger's successor")){
						fingerSuccessor = m.getNode();
						univIndex = 0;
						sendSetup(fingerNode, fingerSuccessor, "the successor of finger");
					}else if(s[0].equals("node of id-find finger's successor")){
						n = m.getNode();
						sendSetup(n, fingerNode, "your turn to find finger's successor_"+univIndex);
					}else if(s[0].equals("your turn to find finger's successor")){
						//System.out.println("++++my turn to find successor of "+m.getNode());
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
							localIndex = 0;
						}
					}else if(m.getMsg().equals("update your finger table")){
						
						System.out.println("FIXING FINGERS...");
						populateFingerTable();
					}else if(m.getMsg().equals("increment")){
						val+=1;
						//System.out.println("VAL = "+val);
						if(val == 2) {
							
							periodicallyNotify();
							val = 0;
						}
					}else if(m.getMsg().equals("done updating finger table")){
						ind-=1;
						System.out.println("\nIND -->"+ind);
						if(ind>=0) {
							System.out.println("xxxxxxxxxx"+connectedNodes.get(ind).getID());
							Node temp = connectedNodes.get(ind);
							sendSetup2(temp, null, "update your finger table");
						}
					}else if(m.getMsg().equals("set exitCon")){
						System.out.println("ginpapa update na naman an finger table");
						periodicallyNotify();
					}else if(m.getMsg().equals("bye")){
						removeNode(m.getNode());
						newSize = connectedNodes.size();
					}else {
						pro.setReceiverIP(packet.getAddress().getHostAddress());
						pro.setReceiverPort(packet.getPort());
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
	
	private void removeNode(Node n) {
		for(int i = 0;i<connectedNodes.size();i++) {
			if(0==connectedNodes.get(i).getID().compareTo( n.getID())) {
				connectedNodes.remove(i);
			}else {
				System.out.println(connectedNodes.get(i).getID()+"///"+n.getID());
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
	
	private void receive() {
		buf = new byte[10240000];

		packet = new DatagramPacket(buf, buf.length);
		try {
			hostSocket.receive(packet);
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
	
	
	public static void addNewMember(Node newMember) {
		connectedNodes.add(newMember);
		//printMembers();
		newSize = connectedNodes.size();
		//periodicallyNotify();
	}
	
	private static void printMembers() {
		for(int a = 0;a<connectedNodes.size();a++) {
			System.out.println(""+connectedNodes.get(a).getPort() +"/"+connectedNodes.get(a).getID());
		}
	}
	
	public static ArrayList<Node> getConnectedNodes(){
		return connectedNodes;
	}
	
	public static Node getRandomNode() {
		Random rand = new Random();
		int index ;
		if(connectedNodes.size() <=2)
			index= rand.nextInt(connectedNodes.size()-1);
		else
			index=0;
		return connectedNodes.get(index);
		
	}
	
	private Node findNode(BigInteger id) {
		for(int a = 0;a<connectedNodes.size();a++) {
			if(0 == id.compareTo(connectedNodes.get(a).getID())) {
				return connectedNodes.get(a);
			}
		}
		return null;
	}
	
	public void findSuccessor(BigInteger idN, String msg) {
		System.out.println("the fuck?");
		String[] txt = msg.split("_");
		if(txt.length>1) {
			univIndex = Integer.parseInt(txt[1]);
		}
		
		if(txt[0].equals("found successor")) {
			idNode = findNode(idN);
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
					System.out.println("txt[0] = "+txt[0]);
					sendSetup(myNode, myNode, "asking for node of id_"+idSuccessor+"_"+txt[0]);
					System.out.println("asking for node of id of "+idSuccessor);
				}else {
					closestPreceedingNode(string, idN);
				}
			}else {
				System.out.println("idN >= idSuccessor");
				System.out.println("found successor of "+idN);
			//	System.out.println("txt[0] = "+txt[0]);
				sendSetup(myNode, myNode, "asking for node of id_"+idSuccessor+"_"+txt[0]);
				//System.out.println("asking for node of id of "+idSuccessor);
			}
		}else {
			closestPreceedingNode(string, idN);
		}
	}

	private String setParams(String msg) {
		if(msg.equals("found successor")) {
			return "find successor";
		}else if(msg.equals("found fileID successor")) {
			return "find fileID successor";
		}else if(msg.equals("found finger's successor")){
			return "find finger's successor_"+univIndex;
		}else {
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
		System.out.println("asko my successor instead to find id");
		int a = 0;
		if(!msg.equals("your turn to find finger's successor")) {
			for(a = 0;a<fingers.length;a++) {
				if(isNIDrange(fingers[a].getKey(), bigin)) {
					System.out.println("short cutu ===========");
					sendSetup(myNode, myNode, "asking for node of id_"+fingers[a].getKeySuccessor()+"_"+msg);
					break;
				}else {
					if(a == fingers.length-1) {
						System.out.println("short cutu *************");
						sendSetup(myNode, myNode, "asking for node of id_"+myNode.getSuccessor()+"_"+msg);
					}
				}
			}
		}else {
			System.out.println("short cutu >>>>>>>>>>>>>>>>>");
			sendSetup(myNode, myNode, "asking for node of id_"+myNode.getSuccessor()+"_"+msg);
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
			//temp =new BigInteger("20");
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
	
	private void invokeHostNotify(String msg) {
		pro.setReceiverIP(myIP);
		pro.setReceiverPort(myPort);
		output = new Message(myNode, msg);
		pro.setOutput(output);
		pro.send();
	}
	public static void main(String[] args) {
		new Host();
	}
	
	
}
