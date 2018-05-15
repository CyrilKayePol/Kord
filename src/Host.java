import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
	private static boolean flag = false;
	
	
	public Host() {
		initialize();
		createRing();
		start();
		waitForMessage();
	}
	
	public static void periodicallyNotify() {
			////System.out.println(oldSize+":"+newSize);
			if(oldSize < newSize) {
				/* notify every member to update their myNodeInfo
				 * and set oldSize = newSize*/
				//System.out.println("NOTIFYING MEMBERS");
				notifyMembers();
				
				oldSize = newSize;
			}
		
	}
	
	private static void notifyMembers() {
		
			for(int i = 0;i<oldSize;i++) {
				Node temp = connectedNodes.get(i);
				sendSetup2(temp, null, "update your myNode info");
				try {
					Thread.sleep(100*connectedNodes.size());
				}catch(Exception e) {
					e.printStackTrace();
				}
			}
		
	}
	
	private void initialize() {
		
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
	
	private void createRing() {
		myNode = new Node(myIP, myPort);
		myNode.setID(new BigInteger("17"));
		myNode.create();
		connectedNodes.add(myNode);
		oldSize = connectedNodes.size();
		newSize = oldSize;
		pro.setMyNode(myNode);
		//System.out.println("[created a ring with node "+myNode.getID()+"]");
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
		System.out.print("enter choice:");
		Scanner scan = new Scanner(System.in);
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
		System.out.println("ID: "+myNode.getID());
		System.out.println("Successor: "+myNode.getSuccessor());
		System.out.println("==================================");
	}
	
	private void downloadFile() {
		System.out.println("[download]");
	}
	
	private void uploadFile() {
		System.out.println("[upload]");
	}
	
	private void exitRing() {
		System.out.println("[exit ring]");
	}
	
	private void wrongInput() {
		System.out.println("[Invalid input! Please try again.]");
	}
	private void waitForMessage() {
		////System.out.println("hello");
		while(true) {
			receive();

				Message m = (Message) input;
				
				Node n;
				String[] s = m.getMsg().split("_");
				////System.out.println("s[0] = "+s[0]+" FROM "+packet.getPort());
				if(s[0].equals("asking for node of id")) {
					n = m.getNode();
					////System.out.println("sent asked node to "+n.getID());
					
					Node result = findNode(new BigInteger(s[1]));
					sendSetup(n,result,"node of id_"+s[2]);
				} else if(m.getMsg().equals("update predecessor")) {
					n = m.getNode();
					/* update my predecessor w/ node n
					 * tell my old predecessor that its new successor is my
					 * new predecessor*/
					BigInteger old = myNode.getPredecessor();
					myNode.setPredecessor(n.getID());
					//System.out.println("new predecessor: "+n.getID());
					
					/* send a notice to Node old telling it to
					 * update its successor*/
					if(old != null) {
						Node oldNode = findNode(old);
						//System.out.println("old predecessor: "+old);
						sendSetup(oldNode,n,"update successor");
					}
					//System.out.println("JUST UPDATED PREDECESSOR");
					printMyNodeInfo();
				}else if(m.getMsg().equals("update successor")) {
					n = m.getNode();
					/*update my successor with node n*/
					BigInteger old = myNode.getSuccessor();
					myNode.setSuccessor(n.getID());
					
					//System.out.println("new successor: "+n.getID());
					
					/* send a notice to Node n telling it to
					 * update its predecessor as myNode*/
					if(old != null) {
						//System.out.println("old successor: "+old);
						sendSetup(n, myNode, "update predecessor");
					}
					//System.out.println("JUST UPDATED SUCCESSOR");
					printMyNodeInfo();
				}else if(m.getMsg().equals("request for updated predecessor")){
					n = m.getNode();
					sendSetup(n, myNode, "your updated predecessor node");
				}else if(m.getMsg().equals("request for updated successor")){
					n = m.getNode();
					sendSetup(n, myNode, "your updated successor node");
				}else if(m.getMsg().equals("your updated predecessor node")){
					n = m.getNode();
					myNode.setPredecessor(n.getID());
					//System.out.println("i just updated my predecessor "+myNode.getPredecessor());
					
				}else if(m.getMsg().equals("your updated successor node")){
					n = m.getNode();
					myNode.setSuccessor(n.getID());
					//System.out.println("i just updated my successor "+myNode.getSuccessor());
				}else if(m.getMsg().equals("real node")) {
					n = m.getNode();
					//System.out.println("received the real node from "+n.getID());
					//System.out.println("***\nport = "+n.getPort()+"\nid ="+n.getID()+
					//		"\nsuc = "+n.getSuccessor()+"\npre = "+n.getPredecessor()+"\n***");
					/*tell node n to find successor of myNode*/
					sendSetup(n, myNode, "find my successor");
					
				}else if(m.getMsg().equals("find my successor") ||
						m.getMsg().equals("your turn to find successor")){
					n = m.getNode();
					findSuccessor(n.getID());
				}else if(m.getMsg().equals("node of id_found successor")){
					//System.out.println("received the asked node");
					n = m.getNode();
					
					//notifyMyNode(idNode);
					notifySuccessor(n);
					notifyNewNode(n);
				}else if(m.getMsg().equals("node of id_find successor")){
					n = m.getNode();
					setReceiverInfo(n);
					//System.out.println("idnode is ---"+idNode.getID());
					sendSetup(n, idNode, "your turn to find successor");
				}else if(m.getMsg().equals("node of id_get successor of successor")){
					n = m.getNode();
					
					sendSetup(myNode, myNode, "asking for node of id_"+n.getSuccessor()+"_found successor");
					//System.out.println("asking for node of id of "+n.getSuccessor());
				}else {
					pro.setReceiverIP(packet.getAddress().getHostAddress());
					pro.setReceiverPort(packet.getPort());
					pro.protocolProcedure(input);
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
	private void printMyNodeInfo() {
		//System.out.println("---myNodeInfo---");
		//System.out.println("myID = "+myNode.getID());
		//System.out.println("my successor = "+myNode.getSuccessor());
		//System.out.println("my predecessor = "+myNode.getPredecessor());
		//System.out.println("----------------");
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
		flag = false;
		connectedNodes.add(newMember);
		printMembers();
		newSize = connectedNodes.size();
		periodicallyNotify();
	}
	
	
	private static void printMembers() {
		for(int a = 0;a<connectedNodes.size();a++) {
			//System.out.println(""+connectedNodes.get(a).getPort() +"/"+connectedNodes.get(a).getID());
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
	
	public void findSuccessor(BigInteger idN) {
		idNode = findNode(idN);
		//System.out.println("idN = "+idN);
		BigInteger idSuccessor = myNode.getSuccessor();
		
		//System.out.println("idSuccessor = "+idSuccessor);
		//System.out.println("id = "+myNode.getID());
		if(isIDNinRange(idN, idSuccessor)) {
			
			if(-1 == idSuccessor.compareTo(idN)) {
				//System.out.println("idSuccessor < idN");
				
				if(largest) {
					
					//System.out.println("found successor of "+idN);
					sendSetup(myNode, myNode, "asking for node of id_"+idSuccessor+"_found successor");
					//System.out.println("asking for node of id of "+idSuccessor);
				}else {
					closestPreceedingNode();
				}
			}else {
				//System.out.println("idN >= idSuccessor");
				//System.out.println("found successor of "+idN);
				sendSetup(myNode, myNode, "asking for node of id_"+idSuccessor+"_found successor");
				//System.out.println("asking for node of id of "+idSuccessor);
			}
		}else {
			closestPreceedingNode();
		}
	}

	private boolean isIDNinRange (BigInteger idN, BigInteger idSuccessor){
	
		if(-1 == myNode.getID().compareTo(idSuccessor)) {
			//System.out.println("id < idSuccessor");
			if(1== idN.compareTo(myNode.getID()) && (-1 == idN.compareTo(idSuccessor) ||
					0 == idN.compareTo(idSuccessor))) {
				return true;
			}
		}else if(1 == myNode.getID().compareTo(idSuccessor)) {
			//System.out.println("id > idSuccessor");
			BigInteger temp = myNode.getID().mod(idN);
			//System.out.println("temp = "+temp);
			if(-1 == temp.compareTo(idSuccessor)) {
				//System.out.println("temp < idSuccessor");
				largest = false;
				return true;
			}else {
				//System.out.println("set largest");
				largest = true;
				return true;
			}
			
		}else if(0 == myNode.getID().compareTo(idSuccessor)) {
			return true;
		}
		return false;
	}

	private void closestPreceedingNode() {
		//System.out.println("ask my successor to find id instead");
		sendSetup(myNode, myNode, "asking for node of id_"+myNode.getSuccessor()+"_find successor");
		
	}
	
	private void notifySuccessor(Node n) {
		//System.out.println("I notified the fuond successor to change its predecessor to idNode");
		sendSetup(n, idNode, "update predecessor");
	}

	private void notifyMyNode(Node n) {
		//System.out.println("I notified my node to update successor");
		sendSetup(myNode, n, "update successor");
		
	}
	
	private void notifyNewNode(Node n) {
		
		//System.out.println("I notified the new node to change its successor to idSuccessor");
		sendSetup(idNode,n, "update successor");
	}
	public static void main(String[] args) {
		new Host();
	}
	
	
}
