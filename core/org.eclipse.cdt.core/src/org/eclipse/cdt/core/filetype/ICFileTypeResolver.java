/**********************************************************************
 * Copyright (c) 2004 TimeSys Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * TimeSys Corporation - Initial API and implementation
***********************************************************************/
package org.eclipse.cdt.core.filetype;

/**
 * Class responsible for resolving a file name into the
 * associated file type.
 *
 * Accessed by ICFileTypeResolver and file type management UI.
 *  
 * The implementation checks for a match for the provided file name
 *   - By looking for an exact filename match ("iostream" == "iostream")
 *   - By looking for an extension match ("foo.c" == "*.c")
 *   - By looking for a pattern match ("libfoo.so.1.0" == "*.so*")
 */
public interface ICFileTypeResolver {
	/**
	 * Determine which file type corresponds to the given
	 * file name.
	 * 
	 * @param fileName file name to check.
	 * 
	 * @return file type for the provided file name
	 */
	public ICFileType getFileType(String fileName);  

	/**
	 * @return array containing all known file types.
	 */
	public ICFileType[] getFileTypes();  

	/**
	 * @return array containing all known file types.
	 */
	public ICFileTypeAssociation[] getFileTypeAssociations();  

	/**
	 * Add a new file type to the resolver's list.
	 * 
	 * @param type file type to add.
	 *
	 * @return true if the file type object was added.
	 */
	public boolean addFileType(ICFileType type);
	
	/**
	 * Remove the file specified file type object from the
	 * resolver's list.
	 *
	 * @param type file type to remove.
	 * 
	 * @return true if the file type was found and removed.
	 */
	public boolean removeFileType(ICFileType type);

	/**
	 * Add a new file type association to the resolver's list.
	 * 
	 * @param pattern file name pattern to add.
     * @param type file type associated with pattern.
	 *
	 * @return true if the file type association was added.
	 */
	public boolean addFileTypeAssociation(ICFileTypeAssociation assoc);

	/**
	 * Remove a file type association from the resolver's list.
	 * 
	 * @param pattern file name pattern to remove.
	 *
	 * @return true if the file type association was removed.
	 */
	public boolean removeFileTypeAssociation(ICFileTypeAssociation assoc);

}
