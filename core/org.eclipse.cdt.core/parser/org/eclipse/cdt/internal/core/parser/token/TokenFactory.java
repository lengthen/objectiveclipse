/**********************************************************************
 * Copyright (c) 2002-2004 IBM Canada and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors: 
 * IBM Rational Software - Initial API and implementation
***********************************************************************/
package org.eclipse.cdt.internal.core.parser.token;

import org.eclipse.cdt.core.parser.IToken;
import org.eclipse.cdt.internal.core.parser.scanner.IScannerData;

/**
 * @author johnc
 */
public class TokenFactory {
	
	public static IToken createToken( int tokenType, IScannerData scannerData )
	{
		if( scannerData.getContextStack().getCurrentContext().getMacroOffset() >= 0 )
			return new SimpleExpansionToken( tokenType, scannerData.getContextStack() );
		
		return new SimpleToken(	tokenType, scannerData.getContextStack() );
	}

	/**
	 * @param type
	 * @param image
	 * @param scannerData
	 * @return
	 */
	public static IToken createToken(int type, String image, IScannerData scannerData) {
		if( scannerData.getContextStack().getCurrentContext().getMacroOffset() >= 0 )
			return new ImagedExpansionToken( type, scannerData.getContextStack(), image );

		return new ImagedToken(type, scannerData.getContextStack(), image );
	}
	
	
	public static IToken createToken( int type, String image )
	{
		return new ImagedToken( type, image);
	}
}
