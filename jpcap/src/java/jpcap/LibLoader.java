package jpcap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
/**
 * load jpcap lib
 *
 */
public class LibLoader {
	
	private static boolean isLoaded = false;
	/**
	 * initialize jpcap library
	 */
	private static void init() {
		File libFile = null;
		InputStream is = null;
		OutputStream os = null;
		boolean isWindows = System.getProperty("os.name").toLowerCase().indexOf("win") >= 0;
		try {
			String libraryName = "Jpcap-ia32";
			if ("64".equals(System.getProperties().getProperty("sun.arch.data.model"))) {
				libraryName = "Jpcap-x86_64";
			}
			libFile = File.createTempFile(libraryName, isWindows ? ".dll" : ".so");
			is = JpcapCaptor.class.getClass().getResourceAsStream("/" + libraryName + (isWindows ? ".dll" : ".so"));
			os = new FileOutputStream(libFile);
			int len;
			byte[] buf = new byte[1024];
			while ((len = is.read(buf)) != -1) {
				os.write(buf, 0, len);
			}
			is.close();
			is = null;
			os.close();
			os = null;
			System.load(libFile.getAbsolutePath());
		} catch (IOException e) {
			throw new RuntimeException("cannot extract/initialize Jpcap native lib", e);
		} catch (Exception e) {
			throw new RuntimeException("cannot initialize Jpcap, please make sure you have installed "
					+ (isWindows ? "wincap" : "libcap"), e);
		} finally {
			if (libFile != null) {
				//a known issue here, the temp file cannot be removed in windows
				libFile.deleteOnExit();
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	public static synchronized void load(){
		if(!isLoaded){
			init();
			isLoaded = true;
		}
	}
}
