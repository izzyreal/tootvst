package uk.org.toot.misc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;

import com.synthbot.audioplugin.vst.vst2.*;

public class Vst
{
	// any more than 127 and dynamic automation won't work
	private final static int MAX_PLUGIN_ID = 127;
	
	private static List<File> paths;

	private static final int WINDOWS = 0;
	private static final int MAC_OS_X = 1;
	private static final int LINUX = 2;
	
	private static int os;
	
	static {
		os = decideOS();
		paths = VstSetup.getPaths();
	}																																		
	
	private static int decideOS() {
		String osname = System.getProperty("os.name").toLowerCase();
		if ( osname.startsWith("mac os x") ) {
			return MAC_OS_X;
		} else if ( osname.contains("windows") ) {
			return WINDOWS;
		} else if ( osname.contains("linux") ) {
			return LINUX;
		}
		return -1;
	}
	
	private static boolean possibleVST(String filename) {
		switch ( os ) {
		case WINDOWS:
			return filename.endsWith(".dll");
		case MAC_OS_X:
			// assume Mac uses Mach-O, not Carbon VSTs
			return filename.endsWith(".vst");
		case LINUX:
			// assume linux uses dlls with Wine or native shared objects
			return filename.endsWith(".dll") || filename.endsWith(".so");
		default:
			return false;
		}
	}
	
	/**
	 * Scan VST plugins and cache their details for service provision.
	 * If the cache already exists rescan() is called instead.
	 * @param cache
	 * @param synth
	 */
	public static void scan(File cache, boolean synth) {
		if ( cache.exists() ) {
			rescan(cache, synth);
			return;
		}
		
		scan(new VstPrintStreamScanner(cache), synth);
	}
	
	private static void scan(VstScanner scanner, boolean synth) {
		try {
			scanner.begin();
			for ( File path : paths ) {
				scan(path, synth, scanner);
			}
			scanner.end();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}
	
	private static void scan(File path, boolean synth, VstScanner scanner) {
		// when a vst is written to a directory, the last modified time for that
		// directory will be updated to newer than the cache file so the whole directory 
		// must be scanned then but not otherwise.
		if ( path.lastModified() < scanner.lastModified() ) return;
//		System.out.println(path.getPath()+","+path.lastModified()+">"+scanner.lastModified());
		File[] files = path.listFiles();
		for ( int i = 0; i < files.length; i++ ) {
			File file = files[i];
			if ( file.isDirectory() ) {
				scan(file, synth, scanner);
				continue;
			}
			String filename = file.getPath();
			if ( possibleVST(filename) ) {
				JVstHost2 vst = null;
				try {
					System.out.print(filename+" creating... ");
					vst = JVstHost2.newInstance(file);
					vst.setSampleRate(44100f);
					vst.setBlockSize(8800);
					if ( vst.isSynth() == synth ) {
						String uid = vst.getUniqueId();
						String effectName = vst.getEffectName().replace(',', ' ');
						String vendorName = vst.getVendorName().replace(',', ' ');
						System.out.print(effectName+", "+vendorName+", "+
							vst.numInputs()+"/"+vst.numOutputs()+"/"+vst.numParameters()+
							", uid="+uid+", "+vst.getVstVersion());
						scanner.each(uid, filename, effectName, vendorName);
					}
					System.out.print(", off and unload... ");
					vst.turnOffAndUnloadPlugin();
					System.out.println("unloaded");
					vst = null;
				} catch ( Exception e ) {
					System.out.println("\n"+filename+" "+e.getMessage());
				}
			}
		}
	}
	
	/**
	 * Rescan VST plugins and merge details into an existing cache.
	 * ids must be preserved.
	 * Names must be preserved to maintain user changes.
	 * Disables must be preserved.
	 * Plugins may have been added, deleted or moved.
	 * @param cache the file to use as the plugin cache
	 * @param synth true for synth plugins, false for effects plugins
	 */
	private static void rescan(File cache, boolean synth) {
		final long lastModified = cache.lastModified();
		final VstPluginInfo[] infos = new VstPluginInfo[MAX_PLUGIN_ID+1];
		final HashMap<String, VstPluginInfo> map = new HashMap<String, VstPluginInfo>();
		
		VstPluginInfo info;
		
		// read in existing cache
		try {
			BufferedReader br = new BufferedReader(new FileReader(cache));
			String line;
			while ((line = br.readLine()) != null) {
				String[] parts = line.split(", ");
				if ( parts.length >= 5 ) {
					int id = Integer.parseInt(parts[0]);
					int pos = Math.abs(id);
					info = new VstPluginInfo(id, parts[1], parts[2], parts[3], parts[4]);
					File file = new File(info.getPath());
					if ( !file.exists() ) info.disable();
					// we keep the info in case plugin reappears
					// then it will keep its id
					infos[pos] = info; 				// array indexed by abs(id)
					map.put(info.getUid(), info); 	// map keyed by uid
				}
			}
			br.close();
		} catch ( Exception e ) {
			System.err.println("Failed to read "+cache.getPath());
			e.printStackTrace();
		}
		
		// update preserving pertinent details
		VstScanner scanner = new VstScanner() {
			public void begin() {}

			public void each(String uid, String filename, String effectName, String vendorName) {
				VstPluginInfo info = map.get(uid);
				if ( info == null ) { 	// not found in cache
					for ( int i = 1; i <= MAX_PLUGIN_ID; i++) {
						if ( infos[i] == null ) {
							VstPluginInfo newInfo = new VstPluginInfo(i, uid, filename, effectName, vendorName);
							infos[i] = newInfo;					// plugin added
							map.put(newInfo.getUid(), info); 	// map keyed by uid
							return;
						}
					}
					System.err.println("Too many VST plugins, failed to cache "+effectName);
				} else {				// found in cache
					info.setPath(filename); // in case plugin has moved
				}
			}

			public void end() {}
			
			public long lastModified() { 
				return lastModified; // each() only called for files more recent than cache 
			}
		};
		scan(scanner, synth);

		// backup cache
		File backup = new File(cache.getPath()+".backup");
		if ( backup.exists() ) backup.delete();
		cache.renameTo(backup);

		// write out updated cache
		try {
			PrintStream ps = new PrintStream(cache);
			for ( int i = 1; i <= MAX_PLUGIN_ID; i++) {
				info = infos[i];
				if ( info == null ) continue;
				ps.println(info.getId()+", "+info.getUid()+", "+info.getPath()+", "+
						info.getEffectName()+", "+info.getVendorName());			
			}
			ps.close();
		} catch ( Exception e ) {
			e.printStackTrace();
			if ( cache.exists() ) cache.delete();
			backup.renameTo(cache);
			System.out.println("Restored "+cache.getName()+" from backup");
		}
	}
	
	protected static class VstPluginInfo
	{
		private int id;
		private String uid;
		private String path;
		private String effectName;
		private String vendorName;
		
		public VstPluginInfo(int id, String uid, String path, String effectName, String vendorName) {
			this.id = id;
			this.uid = uid;
			this.path = path;
			this.effectName = effectName;
			this.vendorName = vendorName;
		}

		public void disable() {
			if ( id > 0 ) id = -id;
		}
		
		/**
		 * @return the path
		 */
		public String getPath() {
			return path;
		}

		/**
		 * @param path the path to set
		 */
		public void setPath(String path) {
			this.path = path;
		}

		/**
		 * @return the effectName
		 */
		public String getEffectName() {
			return effectName;
		}

		/**
		 * @return the id
		 */
		public int getId() {
			return id;
		}

		/**
		 * @return the uid
		 */
		public String getUid() {
			return uid;
		}

		/**
		 * @return the vendorName
		 */
		public String getVendorName() {
			return vendorName;
		}
	}
	
	public interface VstScanner
	{
		public void begin() throws Exception;
		public void each(String uid, String filename, String effectName, String vendorName);
		public void end();
		public long lastModified();
	}
	
	protected static class VstPrintStreamScanner implements VstScanner
	{
		private File file;
		private PrintStream ps;
		private int id = 1;
		
		public VstPrintStreamScanner(File file) {
			this.file = file;
		}
		
		public void begin() throws Exception {
			System.out.println("Writing "+file.getName());
			ps = new PrintStream(file);
		}

		public void each(String uid, String filename, String effectName, String vendorName) {
			ps.println((id++)+", "+uid+", "+filename+", "+effectName+", "+vendorName);			
		}

		public void end() {
			ps.close();		
			System.out.println("Wrote "+file.getName());
		}
		
		public long lastModified() { 
			return 0L; // each() will called for all dirs, regardless of last modified time
		}
	}
	
}
