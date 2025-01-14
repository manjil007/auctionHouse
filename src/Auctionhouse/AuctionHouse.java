package Auctionhouse;

import Bank.ClientAdress;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;

public class AuctionHouse implements Runnable{
    private Socket socket;
    private ServerSocket server;
    private HashMap<Integer, ObjectOutputStream> agents = new HashMap<>();
    private Items items = new Items();
    private Salesman salesman;
    private Client client;
    private int bankId;
    private String currentBids;

    public AuctionHouse(String serverIp, int serverPort, String allItems){
        try{
            //File itemList = new File(allItems + ".txt");
//            File itemList = new File("resources/cricket.txt");
            InputStream itemList = AuctionHouse.class.getResourceAsStream("/"+allItems+".txt");
            Scanner scan = new Scanner(itemList);
            while (scan.hasNext()){
                AuctionBidManager auctionItems = new AuctionBidManager(scan.nextLine(), 50);
                items.add(auctionItems);
            }
            Collections.shuffle(items);
            scan.close();

            socket = new Socket(serverIp, serverPort);

            ObjectOutputStream bankWriter = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream bankReader = new ObjectInputStream(socket.getInputStream());

            bankWriter.writeObject("auction");
            server = new ServerSocket(5090);
            bankWriter.writeObject(new ClientAdress(InetAddress.getLocalHost().getHostName(), server.getLocalPort()));
//            bankWriter.writeObject(new ClientAdress("DEIMOS", 5090));//InetAddress.getLocalHost().getHostAddress(), server.getLocalPort()));
            this.bankId = (Integer) bankReader.readObject();

            client = new Client(agents, bankWriter, bankId);
            salesman = new Salesman(client, items);
            client.setSalesman(salesman);
            salesman.getSelleableItems();
            salesman.getSelleableItems();
            salesman.getSelleableItems();

            System.out.println(salesman);

            Thread thread = new Thread(client);
            thread.setName("Client");
            thread.start();

            Thread salesmanThread = new Thread(salesman);
            salesmanThread.setName("Salesman");
            salesmanThread.start();
        }catch (IOException | ClassNotFoundException e){
            e.printStackTrace();
        }
    }
    @Override
    public void run() {

        while (socket.isConnected()){
            try {
                Socket clientSocket = server.accept();
                System.out.println("Connected to Agent");

                ObjectOutputStream writer = new ObjectOutputStream(clientSocket.getOutputStream());
                ObjectInputStream reader = new ObjectInputStream(clientSocket.getInputStream());

                int id = (Integer) reader.readObject();
                System.out.println("ID#" + id);
                agents.put(id, writer);

                for (AuctionBidManager bidItem : salesman.values()) {
                    writer.writeObject(bidItem);
                    System.out.println(bidItem.getAuctionItemName() + "was sent to agent");
                }
                ClientHandler handler = new ClientHandler(clientSocket, salesman, client, reader, agents, id);
                (new Thread(handler)).start();
            }catch(IOException | ClassNotFoundException e){
                e.printStackTrace();
            }
        }
    }
    public int getBankId(){
        return this.bankId;
    }
}
