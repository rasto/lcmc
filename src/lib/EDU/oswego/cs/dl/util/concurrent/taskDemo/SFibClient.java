
import java.net.*;
import java.io.*;

public class SFibClient {

  public static void main(String[] args) {
    try {
      Socket s = new Socket("gee.cs.oswego.edu", 1618);
      DataInputStream i = new DataInputStream(s.getInputStream());
      DataOutputStream o = new DataOutputStream(s.getOutputStream());
      o.writeInt(34);
      int answer = i.readInt();
      System.out.println("Answer " + answer);
    }
    catch (Exception e) { e.printStackTrace(); }
  }
}
