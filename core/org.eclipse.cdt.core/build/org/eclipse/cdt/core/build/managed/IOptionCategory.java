/**********************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.cdt.core.build.managed;

/**
 * 
 */
public interface IOptionCategory extends IBuildObject {

	/**
	 * Returns the list of children of this node in the option category tree
	 * 
	 * @return
	 */
	public IOptionCategory[] getChildCategories();
	
	/**
	 * Returns a new child category for this category.
	 * 
	 * @return
	 */
	public IOptionCategory createChildCategory();
	
	/**
	 * Returns the options in this category for a given tool.
	 * 
	 * @param tool
	 * @return
	 */
	public IOption[] getOptions(ITool tool);
	
	/**
	 * Returns the category that owns this category, or null if this is the
	 * top category for a tool.
	 * 
	 * @return
	 */
	public IOptionCategory getOwner();
	
	/**
	 * Returns the tool that ultimately owns this category.
	 * 
	 * @return
	 */
	public ITool getTool();
}
