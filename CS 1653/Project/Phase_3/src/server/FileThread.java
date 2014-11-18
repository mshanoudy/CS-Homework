package server;


import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.lang.Thread;
import java.math.BigInteger;
import java.net.Socket;
import java.security.*;
import java.util.ArrayList;
import java.util.List;
import java.io.*;

/**
 * FileServer's worker thread
 * Handles the business of upload, download, and removing files for clients with valid tokens
 */
public class FileThread extends Thread
{
    private final Socket     socket;           // The socket passed from FileServer
    private       FileServer my_fs;            // The FileServer
    private       Cipher     encryptionCipher; // Cipher for AES encryption
    private       Cipher     decryptionCipher; // Cipher for AES decryption

    /**
     * Constructor which accepts the socket passed from FileServer
     * @param _socket The socket
     */
    public FileThread(Socket _socket, FileServer _fs)
    {
        socket = _socket;
        my_fs  = _fs;
    }

    /**
     * Private method to handle serializing and encrypting Envelopes
     *
     * @param envelope The unencrypted envelope
     *
     * @return The encrypted data
     *
     * @throws IOException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    private byte[] encryptEnvelope(Envelope envelope) throws IOException, BadPaddingException, IllegalBlockSizeException
    {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream    o = new ObjectOutputStream(b);
        o.writeObject(envelope);

        return encryptionCipher.doFinal(b.toByteArray());
    }

    /**
     * Private method to handle decrypting and deserializing Envelopes
     *
     * @param envelope The encrypted envelope
     *
     * @return The unencrypted data
     *
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private Envelope decryptEnvelope(byte[] envelope) throws BadPaddingException, IllegalBlockSizeException, IOException, ClassNotFoundException
    {
        byte[] bytes = decryptionCipher.doFinal(envelope);

        ByteArrayInputStream b = new ByteArrayInputStream(bytes);
        ObjectInputStream    o = new ObjectInputStream(b);

        return (Envelope)o.readObject();
    }

    /**
     * Method that runs the thread that includes handlers for upload, download, and file removal
     */
    public void run()
    {
        boolean proceed = true;

        try
        {// Establish connection and input/output streams
            System.out.println("*** New connection from " + socket.getInetAddress() + ":" + socket.getPort() + " ***");
            final ObjectInputStream  input  = new ObjectInputStream(socket.getInputStream());
            final ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            Envelope response;  // The message to send back to client

            /* HANDSHAKE PROTOCOL */
            // Cipher stuff
            Security.addProvider(new BouncyCastleProvider());
            Cipher cipher = Cipher.getInstance("RSA", "BC");
            cipher.init(Cipher.DECRYPT_MODE, my_fs.privateKey);

            // Send Public Key to client
            output.writeObject(my_fs.publicKey);

            // Receive and decrypt challenge
            byte[] bytes = (byte[])input.readObject();
            bytes = cipher.doFinal(bytes);
            BigInteger RC = new BigInteger(bytes);
            RC = new BigInteger(String.valueOf(RC.intValue() + 1)); // RC + 1

            // Send back RC + 1
            output.writeObject(RC);

            // Receive and decrypt SecretKey and IV
            bytes = (byte[])input.readObject();
            bytes = cipher.doFinal(bytes);
            SecretKey sessionKey = new SecretKeySpec(bytes, "AES");
            byte[] IV = (byte[])input.readObject();
            IV = cipher.doFinal(IV);


            // Set up the AES ciphers for input/output
            IvParameterSpec IVPS = new IvParameterSpec(IV);
            encryptionCipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
            decryptionCipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
            encryptionCipher.init(Cipher.ENCRYPT_MODE, sessionKey, IVPS);
            decryptionCipher.init(Cipher.DECRYPT_MODE, sessionKey, IVPS);

            do
            {// Receive incoming message
                Envelope e = decryptEnvelope((byte[])input.readObject());
                System.out.println("Request received: " + e.getMessage());

                /* Client wants to list files the requester can see */
                if (e.getMessage().equals("LFILES"))
                {
                    // Check to make sure parameter is passed and != null
                    if (e.getObjContents().size() < 1 && e.getObjContents().get(0) == null)
                        response = new Envelope("FAIL-BADCONTENTS");
                    else
                    {// Extract token
                        UserToken yourToken = (UserToken)e.getObjContents().get(0);
                        // list to hold the files the requester can see
                        List<String> list = new ArrayList<>();
                        // Iterate over each file and check against requester groups
                        // TODO: probably a better way to do this and need to account for ADMIN group
                        for (int i = 0; i < FileServer.fileList.getFiles().size(); i++)
                            for (int j = 0; j < yourToken.getGroups().size(); j++)
                                if (FileServer.fileList.getFiles().get(i).getGroup().equals(yourToken.getGroups().get(j)))
                                    list.add(FileServer.fileList.getFiles().get(i).getPath());
                        // If list is created, change response to OK
                        response = new Envelope("OK");
                        response.addObject(list); // Add the list
                    }
                    // Send response
                    output.writeObject(encryptEnvelope(response));
                }
                /* Client wants to upload file */
                else if (e.getMessage().equals("UPLOADF"))
                {
                    // Check to make sure all parameters are passed
                    if (e.getObjContents().size() < 3)
                        response = new Envelope("FAIL-BADCONTENTS");
                    else
                    {// Check to make sure parameters != null
                        if (e.getObjContents().get(0) == null)
                            response = new Envelope("FAIL-BADPATH");
                        else if (e.getObjContents().get(1) == null)
                            response = new Envelope("FAIL-BADGROUP");
                        else if (e.getObjContents().get(2) == null)
                            response = new Envelope("FAIL-BADTOKEN");
                        else
                        {
                            String    remotePath  = (String)e.getObjContents().get(0);    // Extract filename
                            String    group       = (String)e.getObjContents().get(1);    // Extract groupname
                            UserToken yourToken   = (UserToken)e.getObjContents().get(2); // Extract requester token

                            if (FileServer.fileList.checkFile(remotePath))
                            {// File exists
                                System.out.printf("Error: file already exists at %s\n", remotePath);
                                response = new Envelope("FAIL-FILEEXISTS");
                            }
                            else if (!yourToken.getGroups().contains(group))
                            {// Not part of group
                                System.out.printf("Error: user missing valid token for group %s\n", group);
                                response = new Envelope("FAIL-UNAUTHORIZED");
                            }
                            else
                            {
                                // Create group directory
                                File file = new File("shared_files/" + group);
                                if (file.mkdir())
                                    System.out.println("Created new group directory");
                                else if (file.exists())
                                    System.out.println("Found group directory");
                                else
                                    System.out.println("Error creating group directory");

                                // Create file on server
                                file = new File("shared_files/" + group + "/" + remotePath.replace('/', '_'));
                                file.createNewFile();
                                FileOutputStream fos = new FileOutputStream(file);
                                System.out.printf("Successfully created file %s\n", remotePath.replace('/', '_'));

                                // Tell client that server is ready
                                response = new Envelope("READY");
                                output.writeObject(encryptEnvelope(response)); // Send response

                                // Receive message
                                e = decryptEnvelope((byte[])input.readObject());
                                while (e.getMessage().compareTo("CHUNK") == 0)
                                {// There is a chunk to read
                                    fos.write((byte[])e.getObjContents().get(0), 0, (Integer)e.getObjContents().get(1));
                                    response = new Envelope("READY"); // Tell client that server is ready
                                    output.writeObject(encryptEnvelope(response));   // Send response
                                    e = decryptEnvelope((byte[])input.readObject()); // Receive message
                                }

                                if (e.getMessage().compareTo("EOF") == 0)
                                {// Reached end of file
                                    System.out.printf("Transfer successful file %s\n", remotePath);
                                    FileServer.fileList.addFile(yourToken.getSubject(), group, file.getPath());
                                    response = new Envelope("OK");    // Tell client success
                                }
                                else
                                {// Error
                                    System.out.printf("Error reading file %s from client\n", remotePath);
                                    response = new Envelope("ERROR-TRANSFER");
                                }
                                fos.close();
                            }
                        }
                    }
                    // Send response
                    output.writeObject(encryptEnvelope(response));
                }
                /* Client wants to download file */
                else if (e.getMessage().compareTo("DOWNLOADF") == 0)
                {
                    String    remotePath = (String)e.getObjContents().get(0);       // Extract filename
                    Token     t          = (Token)e.getObjContents().get(1);        // Extract requester token
                    ShareFile sf         = FileServer.fileList.getFile(remotePath); // Get the file

                    // Check parameters
                    if (e.getObjContents().size() < 2)
                    {
                        e = new Envelope("FAIL-BADCONTENTS");   // Tell client parameters are missing
                        output.writeObject(encryptEnvelope(e)); // Send response
                    }
                    else if (sf == null)
                    {// File does not exist in FileList
                        System.out.printf("Error: File %s doesn't exist\n", remotePath);
                        e = new Envelope("ERROR_FILEMISSING");  // Tell client file is missing from list
                        output.writeObject(encryptEnvelope(e)); // Send response
                    }
                    else if (!t.getGroups().contains(sf.getGroup()))
                    {// File not in client's groups
                        System.out.printf("Error user %s doesn't have permission\n", t.getSubject());
                        e = new Envelope("ERROR_PERMISSION");   // Tell client permission is wrong
                        output.writeObject(encryptEnvelope(e)); // Send response
                    }
                    else
                    {
                        try
                        {   // PROB GONNA BE ERROR RIGHT HERE
                            File f = new File(sf.getPath());

                            if (!f.exists())
                            {// File does not exist on server directory
                                System.out.printf("Error file %s missing from disk\n", "_"+remotePath.replace('/', '_'));
                                e = new Envelope("ERROR_NOTONDISK");    // Tell client file does not exist
                                output.writeObject(encryptEnvelope(e)); // Send response
                            }
                            else
                            {
                                FileInputStream fis = new FileInputStream(f);

                                do
                                {// Send file in chunks
                                    byte[] buf = new byte[4096];

                                    if (e.getMessage().compareTo("DOWNLOADF") != 0)
                                    {// Message is wrong
                                        System.out.printf("Server error: %s\n", e.getMessage());
                                        break;
                                    }
                                    // Tell client a chunk is coming
                                    e = new Envelope("CHUNK");
                                    int n = fis.read(buf); // Can throw an IOException
                                    if (n > 0)             // Download bar
                                        System.out.printf(".");
                                    else if (n < 0)        // Didn't read in
                                        System.out.println("Read error");

                                    e.addObject(buf);                       // Add chunk to be sent
                                    e.addObject(new Integer(n));            // Add number of bytes in chunk
                                    output.writeObject(encryptEnvelope(e)); // Send response
                                    // Get message from client
                                    e = decryptEnvelope((byte[])input.readObject());
                                }
                                while (fis.available() > 0);

                                // If server indicates success, return the member list
                                if (e.getMessage().compareTo("DOWNLOADF") == 0)
                                {
                                    e = new Envelope("EOF");                // Tell client end of file
                                    output.writeObject(encryptEnvelope(e)); // Send response

                                    // Receive message
                                    e = decryptEnvelope((byte[])input.readObject());
                                    if (e.getMessage().compareTo("OK") == 0)
                                        System.out.printf("File data upload successful\n");
                                    else
                                        System.out.printf("Upload failed: %s\n", e.getMessage());
                                }
                                else
                                    System.out.printf("Upload failed: %s\n", e.getMessage());
                            }
                        }
                        catch (Exception e1)
                        {
                            System.err.println("Error: " + e.getMessage());
                            e1.printStackTrace(System.err);
                        }
                    }
                }
                /* Client wants to delete file */
                else if (e.getMessage().compareTo("DELETEF") == 0)
                {
                    String    remotePath = (String)e.getObjContents().get(0);           // Extract filename
                    Token     t          = (Token)e.getObjContents().get(1);            // Extract requester token
                    ShareFile sf         = FileServer.fileList.getFile(remotePath);     // Get the file

                    // Check parameters
                    if (e.getObjContents().size() < 2)
                        e = new Envelope("FAIL-BADCONTENTS");   // Tell client parameters are missing
                    else if (sf == null)
                    {// File does not exist in FileList
                        System.out.printf("Error: File %s doesn't exist\n", remotePath);
                        e = new Envelope("ERROR_DOESNTEXIST");  // Tell client file is missing
                    }
                    else if (!t.getGroups().contains(sf.getGroup()))
                    {// File not in requester's groups
                        System.out.printf("Error user %s doesn't have permission\n", t.getSubject());
                        e = new Envelope("ERROR_PERMISSION");   // Tell client permission is wrong
                    }
                    else
                    {
                        try
                        {
                            File f = new File(sf.getPath());

                            if (!f.exists())
                            {// File does not exist on server directory
                                System.out.printf("Error file %s missing from disk\n", "_"+remotePath.replace('/', '_'));
                                e = new Envelope("ERROR_FILEMISSING"); // Tell client file does not exist
                            }
                            else if (f.delete())
                            {
                                System.out.printf("File %s deleted from disk\n", "_"+remotePath.replace('/', '_'));
                                FileServer.fileList.removeFile(remotePath);
                                e = new Envelope("OK"); // Tell client delete successful
                            }
                            else
                            {
                                System.out.printf("Error deleting file %s from disk\n", "_"+remotePath.replace('/', '_'));
                                e = new Envelope("ERROR_DELETE");
                            }
                        }
                        catch (Exception e1)
                        {
                            System.err.println("Error: " + e1.getMessage());
                            e1.printStackTrace(System.err);
                            e = new Envelope(e1.getMessage());
                        }
                    }
                    // Send response
                    output.writeObject(encryptEnvelope(e));
                }
                /* Client wants to disconnect */
                else if (e.getMessage().equals("DISCONNECT"))
                {
                    socket.close();
                    proceed = false;
                }
                else
                {
                    response = new Envelope("FAIL-BADMSG");        // Server does not understand client request
                    output.writeObject(encryptEnvelope(response)); // Send response
                }
            } while (proceed);
        }
        catch (Exception e)
        {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }
}
