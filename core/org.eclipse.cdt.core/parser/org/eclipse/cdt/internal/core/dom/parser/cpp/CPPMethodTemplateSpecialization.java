/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM - Initial API and implementation
 *    Markus Schorn (Wind River Systems)
 *******************************************************************************/
package org.eclipse.cdt.internal.core.dom.parser.cpp;

import org.eclipse.cdt.core.dom.ast.DOMException;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.IScope;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassScope;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassType;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPMethod;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPScope;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPTemplateScope;
import org.eclipse.cdt.core.parser.util.ObjectMap;

/**
 * @author aniefer
 *
 */
public class CPPMethodTemplateSpecialization extends
		CPPFunctionTemplateSpecialization implements ICPPMethod {

	/**
	 * @param specialized
	 * @param scope
	 * @param argumentMap
	 */
	public CPPMethodTemplateSpecialization(IBinding specialized,
			ICPPScope scope, ObjectMap argumentMap) {
		super(specialized, scope, argumentMap);
	}

	public boolean isVirtual() {
		// TODO Auto-generated method stub
		return false;
	}

	public int getVisibility() throws DOMException {
		IBinding m = getSpecializedBinding();
		if( m instanceof ICPPMethod )
			return ((ICPPMethod)m).getVisibility();
		return 0;
	}
	
	public ICPPClassType getClassOwner() throws DOMException {
		IScope scope= getScope();
		if (scope instanceof ICPPTemplateScope) {
			scope= scope.getParent();
		}
		if( scope instanceof ICPPClassScope ){
			return ((ICPPClassScope)scope).getClassType();
		}
		return null;
	}

	public boolean isDestructor() {
		char[] name = getNameCharArray();
		if (name.length > 1 && name[0] == '~')
			return true;
		
		return false;
	}

	public boolean isImplicit() {
		return false;
	}

}
