/**
 * 
 */
package pik.clminputdata.tools;

import java.io.File;
import java.io.FilenameFilter;

/**
 * @author Sebastian Schubert
 * 
 */
public class GMLFilenameFilter implements FilenameFilter {

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
	 */
	@Override
	public boolean accept(File folder, String s) {
		return new File(folder, s).isFile()
				&& (s.toLowerCase().endsWith(".gml") || s.toLowerCase()
						.endsWith(".xml"));
	}

}
