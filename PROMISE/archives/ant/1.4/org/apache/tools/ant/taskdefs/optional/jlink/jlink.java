package org.apache.tools.ant.taskdefs.optional.jlink;

import java.io .*;
import java.util .Enumeration;
import java.util .Vector;
import java.util.zip .*;

public class jlink extends Object{

    /**
     * The file that will be created by this instance of jlink.
     */
    public  void setOutfile( String outfile ) {
        if ( outfile == null ) {
            return ;
        }
        this .outfile = outfile;
    }

    /**
     * Adds a file to be merged into the output.
     */
    public  void addMergeFile( String mergefile ) {
        if ( mergefile == null ) {
            return ;
        }
        mergefiles .addElement( mergefile );
    }

    /**
     * Adds a file to be added into the output.
     */
    public  void addAddFile( String addfile ) {
        if ( addfile == null ) {
            return ;
        }
        addfiles .addElement( addfile );
    }

    /**
     * Adds several files to be merged into the output.
     */
    public  void addMergeFiles( String[] mergefiles ) {
        if ( mergefiles == null ) {
            return ;
        }
        for ( int i = 0; i < mergefiles .length; i++ ) {
            addMergeFile( mergefiles[i] );
        }
    }

    /**
     * Adds several file to be added into the output.
     */
    public  void addAddFiles( String[] addfiles ) {
        if ( addfiles == null ) {
            return ;
        }
        for ( int i = 0; i < addfiles .length; i++ ) {
            addAddFile( addfiles[i] );
        }
    }

    /**
     * Determines whether output will be compressed.
     */
    public  void setCompression( boolean compress ) {
        this .compression = compress;
    }

    /**
     * Performs the linking of files.
     * Addfiles are added to the output as-is. For example, a 
     * jar file is added to the output as a jar file.
     * However, mergefiles are first examined for their type.
     * If it is a jar or zip file, the contents will be extracted
     * from the mergefile and entered into the output.
     * If a zip or jar file is encountered in a subdirectory
     * it will be added, not merged.
     * If a directory is encountered, it becomes the root
     * entry of all the files below it.  Thus, you can
     * provide multiple, disjoint directories, as
     * addfiles: they will all be added in a rational 
     * manner to outfile.
     */
    public  void link() throws Exception {
        ZipOutputStream output = new ZipOutputStream( new FileOutputStream( outfile ) );
        if ( compression ) {
            output .setMethod( ZipOutputStream .DEFLATED );
            output .setLevel( Deflater .DEFAULT_COMPRESSION );
        } else {
            output .setMethod( ZipOutputStream .STORED );
        }
        Enumeration merges = mergefiles .elements();
        while ( merges .hasMoreElements() ) {
            String path = (String) merges .nextElement();
            File f = new File( path );
            if ( f.getName().endsWith( ".jar" ) || f.getName().endsWith( ".zip" ) ) {
                mergeZipJarContents( output, f );
            }
            else {
                addAddFile( path );
            }
        }
        Enumeration adds = addfiles .elements();
        while ( adds .hasMoreElements() ) {
            String name = (String) adds .nextElement();
            File f = new File( name );
            if ( f .isDirectory() ) {
                addDirContents( output, f, f.getName() + '/', compression );
            }
            else {
                addFile( output, f, "", compression );
            }
        }
        if ( output != null ) {
            try  {
                output .close();
            } catch( IOException ioe ) {}
        }
    }

    public static  void main( String[] args ) {
        if ( args .length < 2 ) {
            System .out .println( "usage: jlink output input1 ... inputN" );
            System .exit( 1 );
        }
        jlink linker = new jlink();
        linker .setOutfile( args[0] );
        for ( int i = 1; i < args .length; i++ ) {
            linker .addMergeFile( args[i] );
        }
        try  {
            linker .link();
        } catch( Exception ex ) {
            System .err .print( ex .getMessage() );
        }
    }

    /*
     * Actually performs the merging of f into the output.
     * f should be a zip or jar file.
     */
    private void mergeZipJarContents( ZipOutputStream output, File f ) throws IOException {
        if ( ! f .exists() ) {
            return ;
        }
        ZipFile zipf = new ZipFile( f );
        Enumeration entries = zipf.entries();
        while (entries.hasMoreElements()){
            ZipEntry inputEntry = (ZipEntry) entries.nextElement();
            String inputEntryName = inputEntry.getName();
            int index = inputEntryName.indexOf("META-INF");
            if (index < 0){
                try {
                    output.putNextEntry(processEntry(zipf, inputEntry));
                } catch (ZipException ex){
                    String mess = ex.getMessage();
                    if (mess.indexOf("duplicate") >= 0){
                        continue;
                    } else {
                        throw ex;
                    }
                }
                InputStream in = zipf.getInputStream(inputEntry);
                int len = buffer.length;
                int count = -1;
                while ((count = in.read(buffer, 0, len)) > 0){
                    output.write(buffer, 0, count);
                }
                in.close();
                output.closeEntry();
            }
        }
        zipf .close();
    }

    /*
     * Adds contents of a directory to the output.
     */
    private void addDirContents( ZipOutputStream output, File dir, String prefix, boolean compress ) throws IOException {
        String[] contents = dir .list();
        for ( int i = 0; i < contents .length; ++i ) {
            String name = contents[i];
            File file = new File( dir, name );
            if ( file .isDirectory() ) {
                addDirContents( output, file, prefix + name + '/', compress );
            }
            else {
                addFile( output, file, prefix, compress );
            }
        }
    }

    /*
     * Gets the name of an entry in the file.  This is the real name
     * which for a class is the name of the package with the class
     * name appended.
     */
    private String getEntryName( File file, String prefix ) {
        String name = file .getName();
        if ( ! name .endsWith( ".class" ) ) {
            try  {
                InputStream input = new FileInputStream( file );
                String className = ClassNameReader .getClassName( input );
                input .close();
                if ( className != null ) {
                    return className .replace( '.', '/' ) + ".class";
                }
            } catch( IOException ioe ) {}
        }
        System.out.println("From " + file.getPath() + " and prefix " + prefix + ", creating entry " + prefix+name);
        return (prefix + name);
    }

    /*
     * Adds a file to the output stream.
     */
    private void addFile( ZipOutputStream output, File file, String prefix, boolean compress) throws IOException {
        long checksum = 0;
        if ( ! file .exists() ) {
            return ;
        }
        ZipEntry entry = new ZipEntry( getEntryName( file, prefix ) );
        entry .setTime( file .lastModified() );
        entry .setSize( file .length() );
        if (! compress){
            entry.setCrc(calcChecksum(file));
        }
        FileInputStream input = new FileInputStream( file );
        addToOutputStream(output, input, entry);
    }
        
    /*
     * A convenience method that several other methods might call.
     */
    private void addToOutputStream(ZipOutputStream output, InputStream input, ZipEntry ze) throws IOException{
        try {
            output.putNextEntry(ze);            
        } catch (ZipException zipEx) {
            input.close();
            return;
        }
        int numBytes = -1;
        while((numBytes = input.read(buffer)) > 0){
            output.write(buffer, 0, numBytes);
        }
        output.closeEntry();
        input.close();
    }

    /*
     * A method that does the work on a given entry in a mergefile.
     * The big deal is to set the right parameters in the ZipEntry 
     * on the output stream.
     */
    private ZipEntry processEntry( ZipFile zip, ZipEntry inputEntry ) throws IOException{
        /*
          First, some notes.
          On MRJ 2.2.2, getting the size, compressed size, and CRC32 from the
          ZipInputStream does not work for compressed (deflated) files.  Those calls return -1.
          For uncompressed (stored) files, those calls do work.
          However, using ZipFile.getEntries() works for both compressed and 
          uncompressed files.
            
          Now, from some simple testing I did, it seems that the value of CRC-32 is
          independent of the compression setting. So, it should be easy to pass this 
          information on to the output entry.
        */
        String name = inputEntry .getName();
        if ( ! (inputEntry .isDirectory() || name .endsWith( ".class" )) ) {
            try  {
                InputStream input = zip.getInputStream( zip .getEntry( name ) );
                String className = ClassNameReader .getClassName( input );
                input .close();
                if ( className != null ) {
                    name = className .replace( '.', '/' ) + ".class";
                }
            } catch( IOException ioe ) {}
        }
        ZipEntry outputEntry = new ZipEntry( name );
        outputEntry.setTime(inputEntry .getTime() );
        outputEntry.setExtra(inputEntry.getExtra());
        outputEntry.setComment(inputEntry.getComment());
        outputEntry.setTime(inputEntry.getTime());
        if (compression){
            outputEntry.setMethod(ZipEntry.DEFLATED);
        } else {
            outputEntry.setMethod(ZipEntry.STORED);
            outputEntry.setCrc(inputEntry.getCrc());
            outputEntry.setSize(inputEntry.getSize());
        }
        return outputEntry;
    }
        
    /*
     * Necessary in the case where you add a entry that
     * is not compressed.
     */
    private long calcChecksum(File f) throws IOException {
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(f));
        return calcChecksum(in, f.length());
    }

    /*
     * Necessary in the case where you add a entry that
     * is not compressed.
     */
    private long calcChecksum(InputStream in, long size) throws IOException{
        CRC32 crc = new CRC32();
        int len = buffer.length;
        int count = -1;
        int haveRead = 0; 
        while((count=in.read(buffer, 0, len)) > 0){
            haveRead += count;
            crc.update(buffer, 0, count);
        }
        in.close();
        return crc.getValue();
    }

    private  String outfile = null;

    private  Vector mergefiles = new Vector( 10 );

    private  Vector addfiles = new Vector( 10 );

    private  boolean compression = false;
        
    byte[] buffer = new byte[8192];

}


