import java.io.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.dizitart.no2.*;

import static org.dizitart.no2.Document.createDocument;
import static org.dizitart.no2.IndexOptions.indexOptions;
import static org.dizitart.no2.filters.Filters.*;


public class main {
    File localFolder = new File(System.getProperty("user.home")+"/.ws");
    File localfile = new File(localFolder.getPath()+"/datarahman.db");


    public static void main(String[] args){
        main getMyFiles = new main();
        getMyFiles.startFTP();

        getMyFiles.createDb();

    }

    public boolean startFTP(){

        try {

            String serverAddress = "139.99.2.52";
            String userId = "ws@beyonitysoftwares.cf";
            String password = "Mohan1990";
            //String remoteDirectory = "/public_html/ws";
            //String localDirectory = props.getProperty("localDirectory").trim();

            //new ftp client
            FTPClient ftp = new FTPClient();
            ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
            //try to connect
            ftp.connect(serverAddress);
            //login to server
            if(!ftp.login(userId, password))
            {
                ftp.logout();
                return false;
            }
            int reply = ftp.getReplyCode();
            //FTPReply stores a set of constants for FTP reply codes.
            if (!FTPReply.isPositiveCompletion(reply))
            {
                ftp.disconnect();
                return false;
            }

            //enter passive mode
            ftp.enterLocalPassiveMode();
            //get system name
            System.out.println("Remote system is " + ftp.getSystemType());
            //change current directory
            //ftp.changeWorkingDirectory(remoteDirectory);
            System.out.println("Current directory is " + ftp.printWorkingDirectory());

            //get list of filenames
           /* FTPFile[] ftpFiles = ftp.listFiles();

            if (ftpFiles != null && ftpFiles.length > 0) {
                //loop thru files
                for (FTPFile file : ftpFiles) {
                    if (!file.isFile()) {
                        continue;
                    }
                    System.out.println("File is " + file.getName());


                    //get output stream
                    OutputStream output;
                    output = new FileOutputStream(file.getName());
                    //get the file from the remote system
                    ftp.retrieveFile(file.getName(), output);
                    //close output stream
                    output.close();

                    //delete the file
                    ftp.deleteFile(file.getName());

                }
            }*/



            FTPFile datafile = ftp.mdtmFile("datarahman.db");
            System.out.println(datafile.getTimestamp().getTime().toString());

           if(localFolder.exists()){
                if(localfile.exists()){
                    Date date = new Date(localfile.lastModified());
                    System.out.println(datafile.getTimestamp().getTime().getTime()>date.getTime());
                    if(datafile.getTimestamp().getTime().getTime()>date.getTime()){
                        try (FileOutputStream fos = new FileOutputStream(localfile)) {
                            ftp.retrieveFile("datarahman.db", fos);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }else {
                        try(InputStream input = new FileInputStream(localfile)){
                            ftp.storeFile( "/datarahman.db", input);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    }else {
                    try (FileOutputStream fos = new FileOutputStream(localfile)) {
                        ftp.retrieveFile("datarahman.db", fos);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                    System.out.println("file exists");
            }else {
                localFolder.mkdirs();

                System.out.println("file not exists");
            }

            ftp.logout();
            ftp.disconnect();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    private void createDb(){

        Nitrite db = Nitrite.builder()
                .compressed()
                .filePath(localfile)
                .openOrCreate();

        NitriteCollection collection = db.getCollection("datarahman");
        Document doc = createDocument("firstName", "John")
                .put("lastName", "Doe")
                .put("birthDay", new Date())
                .put("data", new byte[] {1, 2, 3})
                .put("fruits", new ArrayList<String>() {{ add("apple"); add("orange"); add("banana"); }})
                .put("note", "a quick brown fox jump over the lazy dog");

// insert the document
        collection.insert(doc);
        collection.createIndex("firstName", indexOptions(IndexType.NonUnique));
        collection.createIndex("note", indexOptions(IndexType.Fulltext));
        Cursor cursor = collection.find(
                // and clause
                and(
                        // firstName == John
                        eq("firstName", "John"),
                        // elements of data array is less than 4
                        elemMatch("data", lt("$", 4)),
                        // elements of fruits list has one element matching orange
                        elemMatch("fruits", regex("$", "orange")),
                        // note field contains string 'quick' using full-text index
                        text("note", "quick")
                )
        );
        System.out.println(cursor.size());
        for (Document document : cursor) {
            System.out.println(document);
        }
    }


}
