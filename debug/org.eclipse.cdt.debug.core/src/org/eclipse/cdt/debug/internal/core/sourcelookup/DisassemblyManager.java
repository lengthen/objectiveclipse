/*
 *(c) Copyright QNX Software Systems Ltd. 2002.
 * All Rights Reserved.
 * 
 */

package org.eclipse.cdt.debug.internal.core.sourcelookup;

import org.eclipse.cdt.debug.core.CDebugCorePlugin;
import org.eclipse.cdt.debug.core.IStackFrameInfo;
import org.eclipse.cdt.debug.core.cdi.CDIException;
import org.eclipse.cdt.debug.core.cdi.ICDISourceManager;
import org.eclipse.cdt.debug.core.cdi.model.ICDIInstruction;
import org.eclipse.cdt.debug.internal.core.DisassemblyStorage;
import org.eclipse.cdt.debug.internal.core.model.CDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;

/**
 * Enter type comment.
 * 
 * @since: Oct 8, 2002
 */
public class DisassemblyManager
{
	// move to preferences
	final static private int DISASSEMBLY_BLOCK_SIZE = 100;
	
	private CDebugTarget fDebugTarget;
	private DisassemblyStorage fStorage = null;

	/**
	 * Constructor for DisassemblyManager.
	 */
	public DisassemblyManager( CDebugTarget target )
	{
		setDebugTarget( target );
	}

	public int getLineNumber( IStackFrameInfo frameInfo )
	{
		DisassemblyStorage storage = getSourceElement( frameInfo );
		if ( storage != null )
		{
			return storage.getLineNumber( frameInfo.getAddress() );
		}
		return 0;
	}

	public Object getSourceElement( IStackFrame stackFrame )
	{
		if ( stackFrame != null )
		{
			return getSourceElement( (IStackFrameInfo)stackFrame.getAdapter( IStackFrameInfo.class ) );
		}
		return null;
	}
	
	private void setDebugTarget( CDebugTarget target )
	{
		fDebugTarget = target;
	}
	
	public CDebugTarget getDebugTarget()
	{
		return fDebugTarget;
	}
	
	private void setDisassemblyStorage( DisassemblyStorage ds )
	{
		fStorage = ds;
	}
	
	protected DisassemblyStorage getDisassemblyStorage()
	{
		return fStorage;
	}
	
	private DisassemblyStorage getSourceElement( IStackFrameInfo frameInfo )
	{
		DisassemblyStorage storage = null;
		if ( frameInfo != null )
		{
			long address = frameInfo.getAddress();
			if ( getDisassemblyStorage() != null && getDisassemblyStorage().containsAddress( address ) )
			{
				storage = getDisassemblyStorage();
			}
			else
			{
				storage = loadDisassemblyStorage( frameInfo );
			}			
		}
		return storage;
	}
	
	private DisassemblyStorage loadDisassemblyStorage( IStackFrameInfo frameInfo )
	{
		setDisassemblyStorage( null );
		if ( frameInfo != null && getDebugTarget() != null && getDebugTarget().isSuspended() )
		{
			ICDISourceManager sm = getDebugTarget().getCDISession().getSourceManager();
			if ( sm != null )
			{
				String fileName = frameInfo.getFile();
				int lineNumber = frameInfo.getFrameLineNumber();
				ICDIInstruction[] instructions = new ICDIInstruction[0];
				try
				{
					instructions = sm.getInstructions( fileName, lineNumber );
				}
				catch( CDIException e )
				{
				}
				if ( instructions.length == 0 )
				{
					long address = frameInfo.getAddress();
					if ( address >= 0 )
					{
						try
						{
							instructions = sm.getInstructions( "0x" + Long.toHexString( address ), "0x" + Long.toHexString( address + DISASSEMBLY_BLOCK_SIZE ) );
						}
						catch( CDIException e )
						{
							CDebugCorePlugin.log( e );
						}
					}
				}
				if ( instructions.length > 0 )
				{
					setDisassemblyStorage( new DisassemblyStorage( getDebugTarget(), instructions ) );
				}
			}
		}
		return getDisassemblyStorage();
	}
}
