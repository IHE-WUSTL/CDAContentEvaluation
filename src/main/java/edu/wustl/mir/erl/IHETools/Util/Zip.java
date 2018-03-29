package edu.wustl.mir.erl.IHETools.Util;

import java.io.Closeable;
import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Utility class handles creation of zip files and the downloading of those
 * files in a JSF 2 environment. General Usage:<ul>
 * <li/>Instantiate the class. 
 * <li/>Invoke {@link #startZip()} to begin creating a zip file.
 * <li/>Invoke {@link #addStringAsFile(String, String) addStringAsFile} and/or 
 * {@link #addFile(String) addFile} methods to add files to the zip file.
 * <li/>Invoke {@link #zip(String) zip} method to finish creating the zip file.
 * <li/>Invoke {@link #close() close} method to release resources used during 
 * the zip process.</ul>
 * Options:<ul>
 * <li/>A 'boilerplate' directory can be specified for a particular instance of
 * Zip using the setBoilerplateDirectory method. If specified, all the files in
 * that directory will be copied to the zip file. This copy is done when 
 * startZip is invoked. The directory can be 'de-specified' by invoking 
 * setBoilerplateDirectory with a null argument.
 * Notes:<ul>
 * <li/>Zip implements {@link java.io.Closeable}, and can therefore be used in a 
 * try with resources block.
 * <li/>For a given instance of Zip, only one zip file can be processed at a
 * time. Invoking startZip will first close any zip file which was already in
 * process at the time.  A created zip file will no longer be available after
 * invoking close or startZip on that instance of Zip.
 * <li/>Errors will be logged. By default, the log file is obtained by 
 * invoking Util.getSyslog(). This can be overridden by {@link #setLog(Logger)}
 * </ul>
 * @author rmoult01
 */
public class Zip implements Serializable, Closeable {
	private static final long serialVersionUID = 1L;
	
	/*******************************************************************
	 ** General properties
	 *******************************************************************/
	
	private static final FileAttribute<Set<PosixFilePermission>> attrs = 
			PosixFilePermissions
			.asFileAttribute(PosixFilePermissions.fromString("rwxrwxrwx"));
		
	private Logger log = Util.getLog();
	
	private static final String DEF_WORK_DIR = "ZipWorkingDirectory";
	
	/**
	 * Specify a specific {@link org.apache.log4j.Logger} for this 
	 * instance of Zip. Default Logger is obtained from {@link Util#getLog()}.
	 * @param log Logger to use for this instance of {@link Zip}, or null to 
	 * turn off logging.
	 */
	public void setLog(Logger log) {
		this.log = log;
	}
	
	private Path workDir = null;
	private Path boilerDir = null; 
	
	private Path tmpDir = null;
	private Path zipPath = null;
	
	/**
	 * Initializes the creation of a zip file. Creates a working directory,
	 * deletes any previous working directory (for this instance of Zip) and
	 * copies in all the files in the boilerplate directory, if one exists.
	 * @throws Exception on error. 
	 */
	public void startZip() throws Exception {
		try {
			// ------------- Create new temp directory to build zip file in
			close();
			if (workDir == null) 
				setWorkingDirectory(DEF_WORK_DIR, true);
			tmpDir = Files.createTempDirectory(workDir, "dir", attrs);

			// --------- Copy in everything in boilerplate directory
			if (boilerDir != null) {
				DirectoryStream<Path> bdir = Files
						.newDirectoryStream(boilerDir);
				for (Path sp : bdir) {
					if (sp.toFile().isFile()) {
						Path dp = tmpDir.resolve(sp.getFileName());
						Files.copy(sp, dp, StandardCopyOption.REPLACE_EXISTING,
								StandardCopyOption.COPY_ATTRIBUTES);
					}
				}
			}
		} catch (Exception e) {
			logError(e);
		}
	} // EO startZip method
	
	public void addStringAsFile(String contents, String fileName) 
		throws Exception {
		try {
			File f = tmpDir.resolve(fileName).toFile();
			FileUtils.writeStringToFile(f, contents);
		} catch (Exception e) {
			logError(e);
		}
	}
	
	/**
	 * Generates a zip file containing the files which have been added since 
	 * {@link #startZip()} was invoked and any files which were copied from the
	 * boilerplate directory, if one was defined.
	 * @param zipFileName The name to be given to the zip file, for example,
	 * "myfile.zip".
	 * @return String complete physical file name of the zip file, for example,
	 * "/opt/apache-tomcat/webapps/appdir/tmp/tmp23345/myfile.zip"
	 * @throws Exception on error
	 */
	public String zip(String zipFileName) throws Exception {
		try {
			zipPath = tmpDir.resolve(zipFileName);
			URI uri = URI.create("jar:file:" + zipPath.toUri().getPath());
			Map<String, String> env = new HashMap<String, String>();
			env.put("create", "true");
			FileSystem zipFileSystem = FileSystems.newFileSystem(uri, env);
			Path zipRoot = zipFileSystem.getPath("/");
			DirectoryStream<Path> dirStream = Files.newDirectoryStream(tmpDir);
			for (Path src : dirStream) {
				String fileName = src.getFileName().toString();
				if (fileName.equals(zipFileName))
					continue;
				Path dst = zipRoot.resolve(fileName);
				Files.copy(src, dst);
			}
			dirStream.close();
			zipFileSystem.close();
			return zipPath.toString();
		} catch (Exception e) {
			logError(e);
			zipPath = null;
		}
		return null;
	}
		
	public void close() {
		
		if (tmpDir != null) {
			FileUtils.deleteQuietly(tmpDir.toFile());
			tmpDir = null;
			zipPath = null;
		}
	}
		
	/**
	 * Set/reset the working directory used by this instance of {@link Zip}.
	 * NOTE: Any zip in process will be terminated when this method is invoked.
	 * If the working directory has not been set when @{link {@link #startZip()}
	 * is invoked, {@link Zip} will attempt to set it to "zipWorkingDirectory".
	 * @param dirName name of working directory, relative to application
	 * run directory. If dirName begins with "INI:" it is presumed to be the
	 * name of a parameter in the .ini file which contains the actual dirName
	 * and which defaults to "zipWorkingDirectory".
	 * @param create boolean, create directory if it does not exist.
	 * @throws Exception on error or if create is false and directory does not
	 * exist.
	 */
	public void setWorkingDirectory(String dirName, boolean create) 
			throws Exception {
		String name = dirName;
		if (name.startsWith("INI:")) {
			name = Util.getParameterString(name.substring(4), DEF_WORK_DIR);
		}
		try {
			close();
			workDir = Util.getRunDirectory().resolve(name);
			Files.createDirectories(workDir, attrs);
			Util.isValidPfn("zip working directory", workDir, true, "rx");
		} catch (Exception e) {
			logError(e);
		}
	}

	/**
	 * Set/reset/unset boiler plate directory for this instance of {@link Zip}.
	 * NOTE: Boiler plate files are copied to the zip file when
	 * {@link #startZip()} is invoked. changing the directory while a zip is in
	 * progress will have no effect on that zip. If dirName begins with "INI:"
	 * it is presumed to be the name of a parameter in the .ini file which
	 * contains the actual dirName and which defaults to null.
	 * 
	 * @param dirName
	 *            name of boilerplate directory relative to application run
	 *            directory. If null, boilerplate copying is 'unset'.
	 * @throws Exception
	 *             on error or if specified directory does not exist or is not
	 *             readable.
	 */
	public void setBoilerDirectory(String dirName) throws Exception {
		boilerDir = null;
		if (StringUtils.isBlank(dirName)) return;
		String name = dirName;
		if (name.startsWith("INI:")) {
			name = Util.getParameterString(name.substring(4));
		}
		try {
			boilerDir = Util.getRunDirectory().resolve(name);
			Util.isValidPfn("zip boilerplate directory", boilerDir, true, "r");
		} catch (Exception e) {
			boilerDir = null;
			logError(e);
		}
	}
	
	private void logError(Exception e) throws Exception {
		String em = Util.methodCallingMe() + e.getMessage();
		if (log != null) log.warn(em);
		throw new Exception(em);
	}
	
} // EO zip class
