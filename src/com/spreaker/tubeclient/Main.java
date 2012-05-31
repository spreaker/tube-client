package com.spreaker.tubeclient;

import java.io.File;
import java.io.IOException;


/**
 * @author Marco Pracucci <marco.pracucci@spreaker.com>
 */
public class Main
{
    private static final String  TUBE_HOST = "tube.spreaker.com";
    private static final Integer TUBE_PORT = 80;
    
    
    public static void main(String[] args) throws Exception
    {
        // Check arguments
        if (args.length != 2) {
            printUsage();
            return;
        }
        
        // Get user id
        int userId = Integer.parseInt(args[0]);
        
        // Get filepath
        File audioFile = new File(args[1]);
        if (!audioFile.exists()) {
            System.err.println("The file " + args[1] + " does not exist");
            return;
        }
        
        // Get password
        String password = readPassword();
        
        // Run client
        Client client = new Client(TUBE_HOST, TUBE_PORT, audioFile, userId, password);
        client.run();
    }
    
     private static void printUsage()
     {
         System.out.println("Usage: tubeclient <user id> <file>");
         System.out.println();
         System.out.println("Broadcasts an audio file to Spreaker Tube Server");
     }
     
     private static String readPassword() throws IOException
     {
         // Ask for password
         System.out.print("Password: ");
         
         // Read password
         byte[] password = new byte[255];
         int passwordLength = System.in.read(password);
         
         return new String(password, 0, passwordLength - 1);
     }
     
}
