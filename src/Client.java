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
	
	public Client() {
		initialize();
		joinRing();
		start();
		waitForMessage();
	}
	
	private void initialize() {
		input = null;
		hostPort = 8888;
		hostIP = "localhost";
		myPort = 7624;
		try {
			myIP = InetAddress.getLocalHost().getHostAddress();
			clientSocket = new DatagramSocket(myPort);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		myNode = new Node(myIP, myPort);
		myNode.setID(new BigInteger("25"));
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
	public void waitForMessage() {
		while(true) {
			receive();
			
				Message m = (Message) input;
				Node n = m.getNode();
				
				if(m.getMsg().equals("update predecessor")) {
					
					/* update my predecessor w/ node n
					 * tell my old predecessor that its new successor is my
					 * new predecessor*/
					BigInteger old = myNode.getPredecessor();
					myNode.setPredecessor(n.getID());
					//System.out.println("new predecessor: "+n.getID());
					
					/* send a notice to Node old telling it to
					 * update its successor*/
					if(old != null) {
						/*find the corresponding node of old*/
						newSuccessor = n;
						//System.out.println("old predecessor: "+old);
						findNode(old, "successor");
						
					}
					//System.out.println("JUST UPDATED PREDECESSOR");
					printMyNodeInfo();
					
				}else if(m.getMsg().equals("node of id_successor")){
					Node oldNode = m.getNode();
					sendSetup(oldNode, newSuccessor, "update successor");
				}else if(m.getMsg().equals("update successor")) {
					
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
						//	"\nsuc = "+n.getSuccessor()+"\npre = "+n.getPredecessor()+"\n***");
					/*tell node n to find successor of myNode*/
					sendSetup(n, myNode, "find my successor");
					
				}else if(m.getMsg().equals("find my successor") ||
						m.getMsg().equals("your turn to find successor")){
					n = m.getNode();
					findSuccessor(n.getID());
				}else if(m.getMsg().equals("node of id_found successor")){
					n = m.getNode();
					
					//notifyMyNode(idNode);
					notifySuccessor(n);
					notifyNewNode(n);
				}else if(m.getMsg().equals("node of id_find successor")){
					n = m.getNode();
					//System.out.println("idnode is ---"+idNode.getID());
					sendSetup(n, idNode, "your turn to find successor");
				}else if(m.getMsg().equals("node of id_for idNode")){
					//System.out.println("RECEIVED IDNODE");
					n = m.getNode();
					idNode = n;
					//System.out.println("idnode = "+idNode.getID());
				}else if(m.getMsg().equals("node of id_get successor of successor")){
					n = m.getNode();
					findNode(n.getSuccessor(),"found successor");
					//System.out.println("asking for node of id of "+n.getSuccessor());
				}else {
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
	
	private void printMyNodeInfo() {
		//System.out.println("----------------");
		//System.out.println("myID = "+myNode.getID());
		//System.out.println("my successor = "+myNode.getSuccessor());
		//System.out.println("my predecessor = "+myNode.getPredecessor());
		//System.out.println("----------------");
	}
	
	public void findSuccessor(BigInteger idN) {
		findNode(idN, "for idNode");
		//System.out.println("idN = "+idN);
		BigInteger idSuccessor = myNode.getSuccessor();
		
		//System.out.println("idSuccessor = "+idSuccessor);
		//System.out.println("id = "+myNode.getID());
		if(isIDNinRange(idN, idSuccessor)) {
			
			if(-1 == idSuccessor.compareTo(idN)) {
				//System.out.println("idSuccessor < idN");
				
				if(largest) {

					//System.out.println("found successor of "+idN);
					findNode(idSuccessor, "found successor");
					//System.out.println("asking for node of id of "+idSuccessor);
					
				}else {
					closestPreceedingNode();
				}
			}else {
				//System.out.println("idN >= idSuccessor");
				//System.out.println("found successor of "+idN);
				findNode(idSuccessor, "found successor");
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
		findNode(myNode.getSuccessor(), "find successor");
	}
	
	private void findNode(BigInteger big, String msg) {
		pro.setReceiverIP(hostIP);
		pro.setReceiverPort(hostPort);
		output = new Message(myNode, "asking for node of id_"+big+"_"+msg);
		pro.setOutput(output);
		pro.send();
	}
	private void notifySuccessor(Node n) {	
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
		new Client();
	}
	
	
}
