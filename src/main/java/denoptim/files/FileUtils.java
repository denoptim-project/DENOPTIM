/*
 *   DENOPTIM
 *   Copyright (C) 2022 Marco Foscato <marco.foscato@uib.no>
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package denoptim.files;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.google.gson.Gson;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.json.DENOPTIMgson;
import denoptim.logging.StaticLogger;

public class FileUtils
{

//------------------------------------------------------------------------------

    /**
     * Appends an entry to the list of recent files.
     * @param fileName the file to record.
     * @param ff the declared format of file.
     */
    public static void addToRecentFiles(String fileName, FileFormat ff)
    {
        FileUtils.addToRecentFiles(new File(fileName), ff);
    }

//------------------------------------------------------------------------------

    /**
     * Appends an entry to the list of recent files. If the  current list is 
     * reaching the max length, then this method will append the new entry and 
     * remove the oldest one.
     * @param file the file to record.
     * @param ff the declared format of file.
     */
    public static void addToRecentFiles(File file, FileFormat ff)
    {
        String text = "";
        Map<File, FileFormat> existingEntries = DenoptimIO.readRecentFilesMap();
        int maxSize = 20;
        int toIgnore = existingEntries.size() + 1 - maxSize;
        int ignored = 0;
        for (Entry<File, FileFormat> e : existingEntries.entrySet())
        {
            if (ignored < toIgnore)
            {
                ignored++;
                continue;
            }
            text = text + e.getValue() + " " + e.getKey()
                + DENOPTIMConstants.EOL;
        }
        text = text + ff + " " + file.getAbsolutePath();
        try
        {
            DenoptimIO.writeData(
                    DENOPTIMConstants.RECENTFILESLIST.getAbsolutePath(), text, 
                    false);
        } catch (DENOPTIMException e)
        {
            StaticLogger.appLogger.log(Level.WARNING, "WARNING: unable to "
                    + "write list of recent files.", e);
        }
    }
    
//------------------------------------------------------------------------------

    /**
     * Search in a file for a line matching the given string query. If the query
     * is not found in the file, then it is added to it. Note that the query 
     * is compared in a case insensitive manner, and ignoring heading/trailing
     * spaces, and with the content of each 
     * line in the file, so this is rather slow.
     * @param query the string to search for and possibly add to the file.
     * @param file the text file to analyse.
     * @param add if <code>true</code> then we add the query if it was not found.
     * @return <code>true</code> is the query was found in the file. Note that 
     * when <code>add</code> is <code>true</code>
     * we return <code>false</code> when the match is not found in the file, but
     * the moment we return the file has been already updated by this method as
     * to add the query. So, independently on the return value, the file will 
     * contain the query string when this method returns. 
     * @throws IOException when handling of the memory written on disk returns
     * exception.
     */
    
    public static boolean isLineInTxtFile(String query, File file, boolean add) 
            throws IOException
    {
        boolean found = false;
        
        RandomAccessFile rafile = null; // The file we'll lock
        FileChannel channel = null; // The channel to the file
        FileLock lock = null; // The lock object we hold
        
        try
        {
            rafile = new RandomAccessFile(file, "rw");
            channel = rafile.getChannel();
            lock = channel.lock();
            
            for (String line; (line = rafile.readLine()) != null; )
            {
                if (line.trim().length() == 0)
                    continue;
                if (line.trim().equalsIgnoreCase(query.trim()))
                {
                    found = true;
                    break;
                }
            }
            
            if (!found && add)
            {
                rafile.seek(channel.position());
                rafile.writeBytes(query.trim() + DenoptimIO.NL);
                channel.force(true);
            }
        }
        finally
        {
            if (channel != null)
                channel.close();
            if (rafile!= null)
                rafile.close();
            if (lock != null && lock.isValid())
                lock.release();
        }
        return found;
    }
     
//------------------------------------------------------------------------------
    
    /**
     * Check whether we can write and read to a given pathname
     *
     * @param pathName
     * @return <code>true</code> if we can write and read in that pathname
     */
    public static boolean canWriteAndReadTo(String pathName) {
        boolean res = true;
        try {
            DenoptimIO.writeData(pathName, "TEST", false);
            DenoptimIO.readList(pathName);
        } catch (DENOPTIMException e) {
            res = false;
        }
        return res;
    }
  
//------------------------------------------------------------------------------

    /**
     * Looks for a writable location where to put temporary files and returns
     * an absolute pathname to the folder where tmp files can be created.
     *
     * @return a  writable absolute path
     */
    public static String getTempFolder() {
    
        ArrayList<String> tmpFolders = new ArrayList<String>();
        tmpFolders.add(System.getProperty("file.separator") + "tmp");
        tmpFolders.add(System.getProperty("file.separator") + "scratch");
        tmpFolders.add(System.getProperty("java.io.tmpdir"));
    
        String tmpPathName = "";
        String tmpFolder = "";
        for (String t : tmpFolders) {
            tmpFolder = t;
            tmpPathName = tmpFolder + System.getProperty("file.separator")
                    + "Denoptim_tmpFile";
            if (canWriteAndReadTo(tmpPathName)) {
                break;
            }
        }
        return tmpFolder;
    }

//------------------------------------------------------------------------------

    /**
     * Creates a directory.
     * @param fileName
     * @return <code>true</code> if directory is successfully created
     */
    public static boolean createDirectory(String fileName) {
        return (new File(fileName)).mkdir();
    }

//------------------------------------------------------------------------------

    /**
     * @param fileName
     * @return <code>true</code> if file exists
     */
    public static boolean checkExists(String fileName) {
        if (fileName.length() > 0) {
            return (new File(fileName)).exists();
        }
        return false;
    }

//------------------------------------------------------------------------------

    /**
     * Delete the file
     *
     * @param fileName
     * @throws DENOPTIMException
     */
    public static void deleteFile(String fileName) throws DENOPTIMException {
        File f = new File(fileName);
        // Make sure the file or directory exists and isn't write protected
        if (!f.exists()) {
            return;
        }
    
        if (!f.canWrite()) {
            return;
        }
    
        // If it is a directory, make sure it is empty
        if (f.isDirectory()) {
            return;
        }
    
        // Attempt to delete it
        boolean success = f.delete();
    
        if (!success) {
            throw new DENOPTIMException("Deletion of " + fileName + " failed.");
        }
    }

//------------------------------------------------------------------------------

    /**
     * Delete all files with pathname containing a given string
     *
     * @param path
     * @param pattern
     * @throws DENOPTIMException
     */
    public static void deleteFilesContaining(String path, String pattern)
            throws DENOPTIMException {
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String name = listOfFiles[i].getName();
                if (name.contains(pattern)) {
                    deleteFile(listOfFiles[i].getAbsolutePath());
                }
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Count the number of lines in the file
     *
     * @param fileName
     * @return number of lines in the file
     * @throws DENOPTIMException
     */
    public static int countLinesInFile(String fileName) throws DENOPTIMException {
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(fileName));
            byte[] c = new byte[1024];
            int count = 0;
            int readChars = 0;
            while ((readChars = bis.read(c)) != -1) {
                for (int i = 0; i < readChars; ++i) {
                    if (c[i] == '\n') {
                        ++count;
                    }
                }
            }
            return count;
        } catch (IOException ioe) {
            throw new DENOPTIMException(ioe);
        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }
            } catch (IOException ioe) {
                throw new DENOPTIMException(ioe);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Creates a zip file
     *
     * @param zipOutputFileName
     * @param filesToZip
     * @throws Exception
     */
    public static void createZipFile(String zipOutputFileName,
                                     String[] filesToZip) throws Exception {
        FileOutputStream fos = new FileOutputStream(zipOutputFileName);
        ZipOutputStream zos = new ZipOutputStream(fos);
        int bytesRead;
        byte[] buffer = new byte[1024];
        CRC32 crc = new CRC32();
        for (int i = 0, n = filesToZip.length; i < n; i++) {
            String fname = filesToZip[i];
            File cFile = new File(fname);
            if (!cFile.exists()) {
                continue;
            }
    
            BufferedInputStream bis = new BufferedInputStream(
                    new FileInputStream(cFile));
            crc.reset();
            while ((bytesRead = bis.read(buffer)) != -1) {
                crc.update(buffer, 0, bytesRead);
            }
            bis.close();
            // Reset to beginning of input stream
            bis = new BufferedInputStream(new FileInputStream(cFile));
            ZipEntry ze = new ZipEntry(fname);
            // DEFLATED setting for a compressed version
            ze.setMethod(ZipEntry.DEFLATED);
            ze.setCompressedSize(cFile.length());
            ze.setSize(cFile.length());
            ze.setCrc(crc.getValue());
            zos.putNextEntry(ze);
            while ((bytesRead = bis.read(buffer)) != -1) {
                zos.write(buffer, 0, bytesRead);
            }
            bis.close();
        }
        zos.close();
    }

//------------------------------------------------------------------------------

    /**
     * Inspects a file/folder and tries to detect if the it is one of
     * the data sources that is recognised by DENOPTIM.
     * @param inFile the file to inspect
     * @return a string informing on the detected file format, or null.
     * @throws UndetectedFileFormatException when the format of the file could
     * not be detected.
     * @throws IOException when the the file could not be read properly.
     */
    
    public static FileFormat detectFileFormat(File inFile) 
            throws UndetectedFileFormatException, IOException 
    {
        FileFormat ff = null;
    	String ext = FilenameUtils.getExtension(inFile.getAbsolutePath());
    	// Folders are presumed to contain output kind of data
    	if (inFile.isDirectory())
    	{
    		
    		// This is to distinguish GA from FSE runs
    		for(File folder : inFile.listFiles(new FileFilter() {
    			
    			@Override
    			public boolean accept(File pathname) {
    				if (pathname.isDirectory())
    				{
    					return true;
    				}
    				return false;
    			}
    		}))
    		{
    			if (folder.getName().startsWith(
    					DENOPTIMConstants.FSEIDXNAMEROOT))
    			{
    				ff = FileFormat.FSE_RUN;
    				return ff;
    			}
    			else if (folder.getName().startsWith(
    					DENOPTIMConstants.GAGENDIRNAMEROOT))
    			{
    				ff = FileFormat.GA_RUN;
    				return ff;
    			}
    		}
    		
    		throw new UndetectedFileFormatException(inFile);
    	}
    	
    	// Files are first distinguished first by extension
    	switch (ext.toUpperCase())
    	{		
    		case "SDF":
    			//Either graphs or fragment
    			ff = FileUtils.detectKindOfSDFFile(inFile.getAbsolutePath());
    			break;
    			
    		case "JSON":
    		    ff = FileUtils.detectKindOfJSONFile(inFile.getAbsolutePath());
    		    break;
    		
    		case "PAR":
    			//Parameters for any DENOPTIM module
    			ff = FileUtils.detectKindOfParameterFile(inFile.getAbsolutePath());
    		    break;
    		    
            case "PARAMS":
                //Parameters for any DENOPTIM module
                ff = FileUtils.detectKindOfParameterFile(inFile.getAbsolutePath());
                break;
    		
    		case "TXT":
                ff = FileFormat.GRAPHTXT;
                break;
                
    		case "":
                //Parameters for any DENOPTIM module
                ff = FileUtils.detectKindOfParameterFile(inFile.getAbsolutePath());
                break;
    		    
    		default:
    		    throw new UndetectedFileFormatException(inFile);
    	}
    	if (ff == null)
        {
            throw new UndetectedFileFormatException(inFile);
        }
    	return ff;
    }

//------------------------------------------------------------------------------

    /**
     * Detect the content of a json file.
     * @param fileName file to analyse.
     * @return the format of that file or null.
     * @throws IOException
     */
    public static FileFormat detectKindOfJSONFile(String fileName) 
            throws IOException
    {
        Gson reader = DENOPTIMgson.getReader();
    
        FileFormat ff = null;
        
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(fileName));
            Object o = reader.fromJson(br,Object.class);
            Object oneObj = null;
            if (o instanceof ArrayList)
            {
                oneObj = ((ArrayList) o).get(0);
            } else {
                oneObj = o;
            }
            if (oneObj instanceof Map)
            {
                if (((Map)oneObj).keySet().contains("gVertices"))
                {
                    br.close();
                    return FileFormat.GRAPHJSON;
                } else {
                    return FileFormat.VRTXJSON;
                }
            }
        }
        catch (IOException ioe)
        {
            throw new IOException("Unable to read file '"+fileName + "'", ioe);
        }
        finally
        {
            try
            {
                if (br != null)
                {
                    br.close();
                    br = null;
                }
            }
            catch (IOException ioe)
            {
                
                throw new IOException("Unable to close file '" + fileName + "'",
                        ioe);
            }
        }
        
        return ff;
    }

//------------------------------------------------------------------------------

    /**
     * Looks into a text file and tries to understand if the file is a 
     * collection of parameters for any specific DENOPTIM module.
     * 
     * @param fileName The pathname of the file to analyze
     * @return a string that defined the kind of parameters
     * @throws IOException 
     * @throws Exception
     */
    public static FileFormat detectKindOfSDFFile(String fileName) 
            throws IOException 
    {
        return FileUtils.detectKindFile(fileName, FileFormat.getSDFFormats());
    }

//------------------------------------------------------------------------------

    /**
     * Looks into a text file and tries to understand is the file is a 
     * collection of parameters for any specific DENOPTIM module.
     * @param fileName The pathname of the file to analyze
     * @return a the format of the parameter file or null.
     * @throws IOException 
     * @throws Exception
     */
    public static FileFormat detectKindOfParameterFile(String fileName) 
            throws IOException
    {
    	return FileUtils.detectKindFile(fileName, FileFormat.getParameterFormats());
    }

//------------------------------------------------------------------------------

    /**
     * Looks into a text file and tries to understand what format it is among 
     * the given formats.
     * 
     * @param fileName The pathname of the file to analyze.
     * @param ffs the file formats to consider.
     * @return a format of parameters, or null.
     * @throws Exception when something goes wrong handling the file
     */
    public static FileFormat detectKindFile(String fileName, FileFormat[] ffs) 
            throws IOException 
    {
        Map<String,FileFormat> regexToMatch = 
                new HashMap<String,FileFormat>();
        Map<String,List<FileFormat>> regexToNotMatch = 
                new HashMap<String,List<FileFormat>>();
        String endOfSample = null;
        for (FileFormat ff : ffs)
        {
           for (String regex : ff.getDefiningRegex())
           {
               regexToMatch.put(regex,ff);
               if (ff.getSampleEndRegex() != null)
               {
                   endOfSample = ff.getSampleEndRegex();
               }
           }
           for (String regex : ff.getNegatingRegex())
           {
               if (regexToNotMatch.containsKey(regex))
               {
                   regexToNotMatch.get(regex).add(ff);
               } else {
                   List<FileFormat> lst = new ArrayList<FileFormat>();
                   lst.add(ff);
                   regexToNotMatch.put(regex, lst);
               }
           }
        }
        
        Map<FileFormat,Boolean> foundDefiningRegex = new HashMap<FileFormat,Boolean>();
        for (FileFormat f : ffs)
            foundDefiningRegex.put(f, false);
        
        Map<FileFormat,Boolean> foundNegatingRegex = new HashMap<FileFormat,Boolean>();
        for (FileFormat f : ffs)
            foundNegatingRegex.put(f, false);
        
        String line;
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(fileName));
            lineReadingLoop:
                while ((line = br.readLine()) != null)
                {
                	if (endOfSample != null && line.matches(endOfSample))
                	{
                		break lineReadingLoop;
                	}
                	
                    if ((line.trim()).length() == 0)
                    {
                        continue;
                    }
                    
                    for (String key : regexToNotMatch.keySet())
                    {
                        if (line.toUpperCase().matches(key.toUpperCase()))
                        {
                            for (FileFormat ff : regexToNotMatch.get(key))
                            {
                                foundNegatingRegex.put(ff,true);
                            }
                        }
                    }
                    
                    for (String keyRoot : regexToMatch.keySet())
                    {
                        if (line.toUpperCase().matches(keyRoot.toUpperCase()))
                        {
                            foundDefiningRegex.put(regexToMatch.get(keyRoot), true);
                        }
                    }
                }
        }
        catch (IOException ioe)
        {
        	throw new IOException("Unable to read file '"+fileName + "'", ioe);
        }
        finally
        {
            try
            {
                if (br != null)
                {
                    br.close();
                    br = null;
                }
            }
            catch (IOException ioe)
            {
                
            	throw new IOException("Unable to close file '" + fileName + "'",
            			ioe);
            }
        }
        
        FileFormat result = null;
        for (FileFormat ff : ffs)
        {
            if (foundDefiningRegex.get(ff) && !foundNegatingRegex.get(ff))
            {
                result = ff;
                break;
            }
        }
    	return result;
    }

//------------------------------------------------------------------------------

    /**
     * Define a filename that can be used, i.e., is still available, because no
     * other file with the same pathname exists. This method returns a string
     * like <code>File_3</code> when files <code>File</code>, 
     * <code>File_1</code>, and
     *  <code>File_2</code> exist already.
     * @param parent the folder where the file is meant to be.
     * @param baseName the prefix of the filename. This string will be postponed
     * by an integer number to identify an available filename.
     * @return the available filename.
     * @throws DENOPTIMException
     */
    public static File getAvailableFileName(File parent, String baseName)
    		throws DENOPTIMException
    {
    	File newName = null;
    	if (!parent.exists())
    	{
    		if (!createDirectory(parent.getAbsolutePath()))
    		{
    			throw new DENOPTIMException("Cannot make folder '"+parent+"'");
    		}
    	}
    	FileFilter fileFilter = new WildcardFileFilter(baseName+"*");
    	File[] cands = parent.listFiles(fileFilter);
    	int i=0;
    	boolean goon = true;
    	while (goon)
    	{
    		i++;
    		int idx = i + cands.length;
    		newName = new File(parent + DenoptimIO.FS + baseName + "_" + idx);
    		if (!newName.exists())
    		{
    			goon = false;
    			break;
    		}
    	}
    	return newName;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Copies the content of all the files specified in the list of sources and
     * places it into the destination file.
     * @param destinationPathname the destination of all content.
     * @param sourcePathnames the pathnames of the files to be copied into the
     * destination file.
     * @throws IOException 
     */
    public static void mergeIntoOneFile(String destinationPathname, 
            List<String> sourcePathnames) throws IOException
    {
        try(OutputStream out = Files.newOutputStream(
                Paths.get(destinationPathname), 
                StandardOpenOption.CREATE, 
                StandardOpenOption.WRITE)) 
        {
            for (String sourcePathname : sourcePathnames) {
                Files.copy(Paths.get(sourcePathname), out);
            }
        }
    }
    
//------------------------------------------------------------------------------

}
