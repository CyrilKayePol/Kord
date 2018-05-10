import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

@SuppressWarnings("unused")
public class Client {
	
	private Node myNode;
	private int myPort, hostPort;
	private String myIP, hostIP;
	
	private DatagramSocket clientSocket;
	private Protocol pro;
	private Object input, output;
	private String ip;
	private int port;
	private byte[] buf;
	
	public Client() {
		initialize();
		joinRing();
		run();
	}
	
	private void initialize() {
		input = null;
		hostPort = 8888;
		hostIP = "localhost";
		myPort = 7844;
		try {
			myIP = InetAddress.getLocalHost().getHostAddress();
			clientSocket = new DatagramSocket(myPort);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		myNode = new Node(myIP, myPort);
		//myNode.setID(new BigInteger("45"));
		pro = new Protocol(clientSocket);
		
	}
	
	public void run() {
		while(true) {
			receive();
			
			if(input instanceof Message) {
				Message m = (Message) input;
				Node n = m.getNode();
				
				if(m.getMsg().equals("update predecessor")) {
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
		System.out.println("----------------");
		System.out.println("myID = "+myNode.getID());
		System.out.println("my successor = "+myNode.getSuccessor().getID());
		//System.out.println("my successor.successor = "+myNode.getSuccessor().getSuccessor().getID());
		if(myNode.getPredecessor()!=null) {
			System.out.println("my predecessor = "+myNode.getPredecessor().getID());
			//System.out.println("my predecessor.predecessor = "+myNode.getPredecessor().getPredecessor().getID());
		}
		System.out.println("----------------");
	}
	
	public static void main(String[] args) {
		new Client();
	}
	
	
}
