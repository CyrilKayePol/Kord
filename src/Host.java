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

@SuppressWarnings("unused")
public class Host{
	
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
	
	private static int oldSize, newSize;
	private static boolean flag = false;
	
	public Host() {
		initialize();
		createRing();
		waitForMessage();
	}
	
	public static void periodicallyNotify() {
			//System.out.println(oldSize+":"+newSize);
			if(oldSize < newSize) {
				/* notify every member to update their myNodeInfo
				 * and set oldSize = newSize*/
				System.out.println("NOTIFYING MEMBERS");
				notifyMembers();
				
				oldSize = newSize;
			}
		
	}
	
	private static void notifyMembers() {
		for(int j = 0;j<2;j++) {
			for(int i = 0;i<oldSize;i++) {
				Node temp = connectedNodes.get(i);
				sendSetup2(temp, null, "update your myNode info");
				try {
					Thread.sleep(500);
				}catch(Exception e) {
					e.printStackTrace();
				}
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
		
		pro = new Protocol(hostSocket);
		
	}
	
	private void createRing() {
		myNode = new Node(myIP, myPort);
		//myNode.setID(new BigInteger("17"));
		myNode.create();
		connectedNodes.add(myNode);
		oldSize = connectedNodes.size();
		newSize = oldSize;
		pro.setMyNode(myNode);
		System.out.println("[created a ring with node "+myNode.getID()+"]");
	}
	
	private void waitForMessage() {
		//System.out.println("hello");
		while(true) {
			receive();
			
			if(input instanceof Message) {
				Message m = (Message) input;
				
				Node n;
				
				if(m.getMsg().equals("update predecessor")) {
					n = m.getNode();
					/* update my predecessor w/ node n
					 * tell my old predecessor that its new successor is my
					 * new predecessor*/
					Node old = myNode.getPredecessor();
					myNode.setPredecessor(n);
					System.out.println("new predecessor: "+n.getID());
					
					/* send a notice to Node old telling it to
					 * update its successor*/
					if(old != null) {
						System.out.println("old predecessor: "+old.getID());
						sendSetup(old,n,"update successor");
					}
					printMyNodeInfo();
				}else if(m.getMsg().equals("update successor")) {
					n = m.getNode();
					/*update my successor with node n*/
					Node old = myNode.getSuccessor();
					myNode.setSuccessor(n);
					
					System.out.println("new successor: "+n.getID());
					
					/* send a notice to Node n telling it to
					 * update its predecessor as myNode*/
					if(old != null) {
						System.out.println("old successor: "+old.getID());
						sendSetup(n, myNode, "update predecessor");
					}
					printMyNodeInfo();
				}else if(m.getMsg().equals("request for updated predecessor")){
					n = m.getNode();
					sendSetup(n, myNode, "your updated predecessor node");
				}else if(m.getMsg().equals("request for updated successor")){
					n = m.getNode();
					sendSetup(n, myNode, "your updated successor node");
				}else if(m.getMsg().equals("your updated predecessor node")){
					n = m.getNode();
					myNode.setPredecessor(n);
					System.out.println("i just updated my predecessor "+myNode.getPredecessor().getID());
					if(myNode.getSuccessor().getSuccessor()!=null) {
						System.out.println("pred.pred "+myNode.getPredecessor().getPredecessor().getID());
					}else{
						System.out.println("pred.pred is null");
					}
				}else if(m.getMsg().equals("your updated successor node")){
					n = m.getNode();
					myNode.setSuccessor(n);
					System.out.println("i just updated my successor "+myNode.getSuccessor().getID());
					if(myNode.getSuccessor().getSuccessor()!=null) {
						System.out.println("my successor.suc "+myNode.getSuccessor().getSuccessor().getID());
					}else{
						System.out.println("suc.suc is null");
					}
				}else {
					pro.setReceiverIP(packet.getAddress().getHostAddress());
					pro.setReceiverPort(packet.getPort());
					pro.protocolProcedure(input);
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
		System.out.println("---myNodeInfo---");
		System.out.println("myID = "+myNode.getID());
		System.out.println("my successor = "+myNode.getSuccessor().getID());
		if(myNode.getPredecessor()!=null)
			System.out.println("my predecessor = "+myNode.getPredecessor().getID());
		System.out.println("----------------");
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
			System.out.println(""+connectedNodes.get(a).getPort() +"/"+connectedNodes.get(a).getID());
		}
	}
	public static ArrayList<Node> getConnectedNodes(){
		return connectedNodes;
	}
	
	public static Node getRandomNode() {
		Random rand = new Random();
		int index = rand.nextInt(connectedNodes.size()-1);
		return connectedNodes.get(index);
		
	}
	
	public static void main(String[] args) {
		new Host();
	}
	
	
}
