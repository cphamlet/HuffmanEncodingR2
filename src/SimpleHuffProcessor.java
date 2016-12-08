

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Map;

public class SimpleHuffProcessor implements IHuffProcessor {
  private HashMap<Integer, Integer> encodingTable    = new HashMap<Integer, Integer>();
  private HashMap<Integer, String>  encodingTableStr = new HashMap<Integer, String>();
  private int[]                     binCounts        = new int[ALPH_SIZE + 1];
  //private HuffViewer                myViewer;
  private BitInputStream            input;
  private BitOutputStream           output;
  TreeNode                          root             = null;
  private HashMap<String, Integer>  uncompressTable = new HashMap<String, Integer>();
  private PriorityQueue<TreeNode>   huffQueue        = new PriorityQueue<TreeNode>();
  private int originalfileSizeinBits = 0;
  private int compressedfileSizeinBits = 0;
  public int compress(InputStream in, OutputStream out, boolean force)
      throws IOException {
    
    input = new BitInputStream(in);
    output = new BitOutputStream(out);

    // Writes magic number
    output.writeBits(BITS_PER_INT, MAGIC_NUMBER);
    compressedfileSizeinBits += 32;
  //  System.out.println("Writing Magic num");
   
    // Preorder Traversal
    writeHeaderTree(root, output);
    
    // Writes the data
    int current;
    while ((current = input.readBits(BITS_PER_WORD)) != -1) {

      String binary = encodingTableStr.get(current);
      compressedfileSizeinBits += binary.length();
    //  System.out.println("Writing Encodings: " + binary);
      for (int i = 0; i < binary.length(); i++) {
        output.writeBits(1, Integer.parseInt(binary.substring(i, i + 1)));
        
      }

    }
    
    // Writes the EOF
    String binary = encodingTableStr.get(ALPH_SIZE);
    compressedfileSizeinBits += binary.length();
  //  System.out.println("EOF Writing Encodings: " + binary);
    for (int i = 0; i < binary.length(); i++) {
      output.writeBits(1, Integer.parseInt(binary.substring(i, i + 1)));
    }

    output.close();
    input.close();
    System.out.println("Original file size was "+  originalfileSizeinBits + " bits");
    System.out.println("Compressed file size is "+  compressedfileSizeinBits+ " bits");
    
    if(compressedfileSizeinBits > originalfileSizeinBits && Huffcop.myForce == false){
      throw new IOException("Compressed file is larger than original. ");
    }
    return compressedfileSizeinBits;
  }

  public int preprocessCompress(InputStream in) throws IOException {
    encodingTable    = new HashMap<Integer, Integer>();
    encodingTableStr = new HashMap<Integer, String>();
    binCounts        = new int[ALPH_SIZE + 1];
    root             = null;
    huffQueue        = new PriorityQueue<TreeNode>();
    originalfileSizeinBits = 0;
    compressedfileSizeinBits = 0;
    input = new BitInputStream(in);
    for (int i = 0; i < ALPH_SIZE; i++) {
      binCounts[i] = 0;
    }

    // Sets EOF frequency to 1
    binCounts[ALPH_SIZE] = 1;

    // Adds counts
    int i = input.readBits(8);
    while (i != -1) {
      binCounts[i] = binCounts[i] + 1;
      i = input.readBits(8);
      originalfileSizeinBits += 8;
    }
    // Adding to Priority Queue
    // adds EOF character frequency 1
    for (i = 0; i < binCounts.length; i++) {
      int binCounti = binCounts[i];
      if (binCounts[i] != 0) {
        huffQueue.add(new TreeNode(i, binCounti));
      }
    }

    // The below code forms the huffman encoding tree, preparing for traversal.

    while (hasAtLeastTwo(huffQueue)) {
      TreeNode min1 = huffQueue.remove().getNode();
      TreeNode min2 = huffQueue.remove().getNode();
      // Forms a parent node

      root = new TreeNode(0, min1.myWeight + min2.myWeight, min1.getNode(),
          min2.getNode());
      huffQueue.add(root.getNode());
    }
    // Sets heights
    assignHeights(root);
    //Traverses tree recursively
    traverseInOrder(root, "");
    printTree(root);
    setEncodingTable();

    input.close();
    return originalfileSizeinBits;
  }

  // The code below counts the number of nodes in the priority queue. Returns
  // false if less than 2 nodes in it.
  // It is lengthy because the priority queue structure doesn't allow us to view
  // the second element so easily, or determine the size
  private boolean hasAtLeastTwo(PriorityQueue<TreeNode> huffQueue) {
    int counter = 0;
    for (TreeNode n : huffQueue) {
      counter++;
      if (counter == 2) {
        return true;
      }
    }
    return false;
    // Ends counting two nodes

  }

  // the below functions takes the string from encodingTableStr, which is a
  // binary number (e.g) "10101"
  // converts it into an int, and places it in encodingTable.
  private void setEncodingTable() {
    for (Map.Entry<Integer, String> entry : encodingTableStr.entrySet()) {
      // System.out.printf("Key : %s and Value: %s %n", entry.getKey(),
      // entry.getValue());
      for (int i = 0; i < entry.getValue().length(); i++) {
        // converts binary to
        if (Integer.parseInt(entry.getValue().substring(i, i + 1)) == 1) {
          encodingTable.put(entry.getKey(), encodingTable.get(entry.getKey())
              + (int) Math.pow(2, entry.getValue().length() - 1 - i));
        }
      }
    }
  }

  // Give a binary number as a string, returns an int value
  private int convertBinaryStrtoInt(String binary) {
    int num = 0;
    for (int i = 0; i < binary.length(); i++) {
      // converts binary to int
      if (Integer.parseInt(binary.substring(i, i + 1)) == 1) {
        num = num + (int) Math.pow(2, binary.length() - 1 - i);
      }
    }

    return num;
  }

//  public void setViewer(HuffViewer viewer) {
//    myViewer = viewer;
//  }

  // writes header tree for compress function
  // writes like: 0 0 1 001100001 1 000100000 1 001110100
  // 0 stands for a non-leaf node, 1 for a leaf
 private void writeHeaderTree(TreeNode current, BitOutputStream output){
      if(current == null){
          
        }
        else {
          if(current.myLeft != null && current.myRight != null){
            //writes 1 0 bit
            output.writeBits(1, 0);
            compressedfileSizeinBits += 1;
     //       System.out.print(0);
            writeHeaderTree(current.myLeft, output);
            writeHeaderTree(current.myRight,output);
          }else{
            //Writes a "1" to denote a leaf node
            output.writeBits(1, 1);
      //      System.out.print(1);
            //Needs to write 9 bits, because EOF takes 9 bits long
            output.writeBits(BITS_PER_WORD+1, current.myValue);
            compressedfileSizeinBits += BITS_PER_WORD+2;
        //    System.out.print(" "+current.myValue+" ");
            
          }
          
        }
      
 }

 private void printTree(TreeNode current){
   if(current == null){
       
     }
     else {
       if(current.myLeft != null && current.myRight != null){
         
         
         printTree(current.myLeft);
         printTree(current.myRight);
         
       }else{
     //    System.out.print("Leaf Val: " + current.myValue+" ");       
       }
       
     }
   
}
 
 private void setUncompressTable(TreeNode current, String s){
   if(current == null){
       
     }
     else {
       if(current.myLeft != null && current.myRight != null){
         
         setUncompressTable(current.myLeft, s+"0");
         setUncompressTable(current.myRight, s+"1");
         
       }else{    
         uncompressTable.put(s, current.myValue);
       }
       
     }
   
}
  private TreeNode readHeaderTree() throws IOException {
    TreeNode current = new TreeNode(-1,-1);
    if(input.readBits(1) == 1){
        return new TreeNode(input.readBits(BITS_PER_WORD+1), -1, null, null);
      }else{ 
        current.myLeft = readHeaderTree();
        current.myRight = readHeaderTree();
      }
    return current;
  }
    

  private void traverseInOrder(TreeNode current, String s) {
    if(current == null){
      
    }
    else {
      if(current.myLeft != null && current.myRight != null){
        
        
        traverseInOrder(current.myLeft, s + "0");
        traverseInOrder(current.myRight, s + "1");
        
      }else{
        encodingTableStr.put(current.myValue, s);
        // adding 0 prevents null pointer below
         encodingTable.put(current.myValue, 0);   
      }
      
    }
      
  }
  
  
  
  
  // sets the height to each node
  int assignHeights(TreeNode curr) {
    if (curr == null) {
      return -1;
    }

    int leftht = assignHeights(curr.myLeft);
    int rightht = assignHeights(curr.myRight);

    if (leftht > rightht) {
      curr.myHeight = leftht + 1;
      return leftht + 1;
    } else {
      curr.myHeight = rightht + 1;
      return rightht + 1;
    }
  }

  public int uncompress(InputStream in, OutputStream out) throws IOException {
    uncompressTable = new HashMap<String, Integer>();
    input = new BitInputStream(in);
    output = new BitOutputStream(out);
    originalfileSizeinBits = 0;
    
    int magicNum = input.readBits(BITS_PER_INT);

    if (magicNum != MAGIC_NUMBER) {
      throw new IOException("invalid Filetype");
    }
    
    TreeNode header = readHeaderTree();

    // below code converts encodings to original file
    int bits;
    String binary = "";
    TreeNode temp = header;
    setUncompressTable(header, "");
    

    while (true) {

      if ((bits = input.readBits(1)) == -1) {
        System.err.println("should not happen! trouble reading bits");
      } else {
       
        if ((bits & 1) == 0) {
          binary = binary + "0";
        } else { // read a 1, go right in tree
          binary = binary + "1";
        }
        int unencoded = -1;
        if(uncompressTable.get(binary) != null){
          unencoded = uncompressTable.get(binary);
        }else{
          continue;
        }
        if(unencoded == ALPH_SIZE){
          //Pseudo EOF reached, end loop
          input.close();
          output.close();
          break;
        }
        if(unencoded != -1){
         output.writeBits(BITS_PER_WORD, unencoded);
         originalfileSizeinBits+=BITS_PER_WORD;
         binary = "";
        }

      }
    }
    System.out.println("Uncompressed filesize is "+ originalfileSizeinBits + " bits long");
    return 0;
  }

//  private void showString(String s) {
//    myViewer.update(s);
//  }

}
