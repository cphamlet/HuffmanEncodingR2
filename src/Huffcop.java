import java.io.*;


public class Huffcop {

  private static String         HUFF_SUFFIX   = ".hf";
  private static String         UNHUFF_SUFFIX = ".unhf";
  private static IHuffProcessor myModel;
  private static File           myFile;
  public static boolean         myForce = true; 

  public static void main(String[] args) throws IOException {

    // args[0] true means compression, false for uncompress
    // args[1] true for forced compression
    // args[2] filename

    IHuffProcessor proc = new SimpleHuffProcessor();
    myModel = proc;
    myForce = Boolean.valueOf(args[1]);
    if (Boolean.valueOf(args[0])) {
      doRead(args);
      doSave(args);
    } else {
      doDecode(args);
    }

  }

  protected static File doRead(String[] args) {

    myFile = new File(args[2]);
    myForce = Boolean.valueOf(args[1]);
    
    try {
    myModel.preprocessCompress(new FileInputStream(myFile));
    } catch (IOException e) {
      e.printStackTrace();
    }

    return myFile;
  }

  private static void doSave(String[] args) {
    if (myFile == null) {
      return;
    }

    String name = myFile.getName();

    String newName = name + HUFF_SUFFIX;


    String path = null;
    try {
      path = myFile.getCanonicalPath();
    } catch (IOException e) {
      return;
    }
     int pos = path.lastIndexOf(name);
     newName = path.substring(0, pos) + newName;
    final File file = new File(newName);
    try {
      final FileOutputStream out = new FileOutputStream(file);
      FileInputStream tempInput = new FileInputStream(myFile);

      try {

        myModel.compress(tempInput, out, myForce);
      } catch (IOException e) {

        // cleanUp(file);
        e.printStackTrace();
      }

    } catch (FileNotFoundException e) {
      // Can't find file
      e.printStackTrace();
    }
    myFile = null;
  }

  
    private static void doDecode(String[] args) {
      File file = new File(args[2]);
      //uncompressing
      try {
          
          
          String name = file.getName();
          String uname = name;
          System.out.println("Compressed filesize is "+file.length()*8+" bits");
          if (name.endsWith(HUFF_SUFFIX)) {
              uname = name.substring(0,name.length() - HUFF_SUFFIX.length()) + UNHUFF_SUFFIX;
          }
          else {
              uname = name + UNHUFF_SUFFIX;
          }
          String newName = uname;

          String path = file.getCanonicalPath();
  
          int pos = path.lastIndexOf(name);
          newName = path.substring(0, pos) + newName;
          final File newFile = new File(newName);
          FileInputStream tempStream = new FileInputStream(file);
          
          final FileInputStream stream = tempStream;
              
          final OutputStream out = new FileOutputStream(newFile);

                  try {
                      myModel.uncompress(stream, out);
                  } catch (IOException e) {
                                  
                      e.printStackTrace();
                  }

      } catch (FileNotFoundException e) {
          e.printStackTrace();
      } catch (IOException e) {
          e.printStackTrace();
      }
  }
}
