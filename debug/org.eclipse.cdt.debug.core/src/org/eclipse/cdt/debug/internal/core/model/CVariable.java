/*
 *(c) Copyright QNX Software Systems Ltd. 2002.
 * All Rights Reserved.
 * 
 */
package org.eclipse.cdt.debug.internal.core.model;

import java.util.LinkedList;

import org.eclipse.cdt.debug.core.CDebugCorePlugin;
import org.eclipse.cdt.debug.core.ICDebugConstants;
import org.eclipse.cdt.debug.core.cdi.CDIException;
import org.eclipse.cdt.debug.core.cdi.ICDIFormat;
import org.eclipse.cdt.debug.core.cdi.event.ICDIChangedEvent;
import org.eclipse.cdt.debug.core.cdi.event.ICDIEvent;
import org.eclipse.cdt.debug.core.cdi.event.ICDIEventListener;
import org.eclipse.cdt.debug.core.cdi.model.ICDIObject;
import org.eclipse.cdt.debug.core.cdi.model.ICDIStackFrame;
import org.eclipse.cdt.debug.core.cdi.model.ICDIValue;
import org.eclipse.cdt.debug.core.cdi.model.ICDIVariable;
import org.eclipse.cdt.debug.core.cdi.model.ICDIVariableObject;
import org.eclipse.cdt.debug.core.model.ICType;
import org.eclipse.cdt.debug.core.model.ICValue;
import org.eclipse.cdt.debug.core.model.ICVariable;
import org.eclipse.cdt.debug.core.model.ICastToArray;
import org.eclipse.cdt.debug.core.model.ICastToType;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

/**
 * 
 * A superclass for all variable classes.
 * 
 * @since Aug 9, 2002
 */
public abstract class CVariable extends CDebugElement 
								implements ICVariable,
										   ICDIEventListener,
										   ICastToType,
										   ICastToArray
{
	/**
	 * The parent object this variable is contained in.
	 */
	private CDebugElement fParent;

	/**
	 * The underlying CDI variable.
	 */
	private ICDIVariable fCDIVariable;
	
	/**
	 * The shadow CDI variable used for casting.
	 */
	private ICDIVariable fShadow;
	
	/**
	 * Cache of current value - see #getValue().
	 */
	protected CValue fValue;

	/**
	 * The name of this variable.
	 */
	private String fName = null;

	/**
	 * The full name of this variable.
	 */
	private String fQualifiedName = null;

	private Boolean fEditable = null;

	/**
	 * Counter corresponding to this variable's debug target
	 * suspend count indicating the last time this value 
	 * changed. This variable's value has changed on the
	 * last suspend event if this counter is equal to the
	 * debug target's suspend count.
	 */
	private int fLastChangeIndex = -1;

	/**
	 * Change flag.
	 */
	protected boolean fChanged = false;

	/**
	 * The type of this variable.
	 */
	private ICType fType = null;

	/**
	 * The current format of this variable.
	 */
	protected int fFormat = ICDIFormat.NATURAL;

	/**
	 * Constructor for CVariable.
	 * @param target
	 */
	public CVariable( CDebugElement parent, ICDIVariable cdiVariable )
	{
		super( (CDebugTarget)parent.getDebugTarget() );
		fParent = parent;
		fCDIVariable = cdiVariable;
		fShadow = null;
		fFormat = CDebugCorePlugin.getDefault().getPluginPreferences().getInt( ICDebugConstants.PREF_DEFAULT_VARIABLE_FORMAT );
		getCDISession().getEventManager().addEventListener( this );
	}

	/**
	 * Returns the current value of this variable. The value
	 * is cached, but on each access we see if the value has changed
	 * and update if required.
	 * 
	 * @see org.eclipse.debug.core.model.IVariable#getValue()
	 */
	public IValue getValue() throws DebugException
	{
		if ( fValue == null )
		{
			fValue = CValueFactory.createValue( this, getCurrentValue() );
		}
		return fValue;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IVariable#hasValueChanged()
	 */
	public boolean hasValueChanged() throws DebugException
	{
		return fChanged;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IValueModification#setValue(String)
	 */
	public void setValue( String expression ) throws DebugException
	{
		notSupported( "Variable does not support value modification." );
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IValueModification#setValue(IValue)
	 */
	public void setValue( IValue value ) throws DebugException
	{
		notSupported( "Variable does not support value modification." );
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IValueModification#supportsValueModification()
	 */
	public boolean supportsValueModification()
	{
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IValueModification#verifyValue(String)
	 */
	public boolean verifyValue( String expression ) throws DebugException
	{
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IValueModification#verifyValue(IValue)
	 */
	public boolean verifyValue( IValue value ) throws DebugException
	{
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(Class)
	 */
	public Object getAdapter( Class adapter )
	{
		if ( adapter.equals( ICastToType.class ) )
			return this;
		if ( adapter.equals( IVariable.class ) )
			return this;
		if ( adapter.equals( ICVariable.class ) )
			return this;
		return super.getAdapter( adapter );
	}

	/**
	 * Returns this variable's current underlying CDI value.
	 * Subclasses must implement #retrieveValue() and do not
	 * need to guard against CDIException, as this method
	 * handles them.
	 *
	 * @exception DebugException if unable to access the value
	 */
	protected final ICDIValue getCurrentValue() throws DebugException
	{
		try
		{
			return retrieveValue();
		}
		catch( CDIException e )
		{
			targetRequestFailed( e.getMessage(), null );
		}
		return null;
	}

	/**
	 * Sets this variable's change counter to the specified value
	 * 
	 * @param count new value
	 */
	protected void setChangeCount( int count )
	{
		fLastChangeIndex = count;
	}

	/**
	 * Returns this variable's change counter. This corresponds to the
	 * last time this variable changed.
	 * 
	 * @return this variable's change counter
	 */
	protected int getChangeCount()
	{
		return fLastChangeIndex;
	}

	/**
	 * Returns the last known value for this variable
	 */
	protected ICDIValue getLastKnownValue()
	{
		if ( fValue == null )
		{
			return null;
		}
		else
		{
			return fValue.getUnderlyingValue();
		}
	}
	
	protected void dispose()
	{
		if ( fValue != null )
		{
			((CValue)fValue).dispose();
		}
		getCDISession().getEventManager().removeEventListener( this );
		try
		{
			if ( getShadow() != null )
				destroyShadow( getShadow() );
		}
		catch( DebugException e )
		{
			logError( e );
		}
	}
	
	protected synchronized void setChanged( boolean changed ) throws DebugException
	{
		if ( getValue() != null && getValue() instanceof ICValue )
		{
			fChanged = changed;
			((CValue)getValue()).setChanged( changed );
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.core.cdi.event.ICDIEventListener#handleDebugEvent(ICDIEvent)
	 */
	public void handleDebugEvent( ICDIEvent event )
	{
		ICDIObject source = event.getSource();
		if (source == null)
			return;

		if ( source.getTarget().equals( getCDITarget() ) )
		{
			if ( event instanceof ICDIChangedEvent )
			{
				if ( source instanceof ICDIVariable && source.equals( getCDIVariable() ) )
				{
					handleChangedEvent( (ICDIChangedEvent)event );
				}
			}
		}
	}

	private void handleChangedEvent( ICDIChangedEvent event )
	{
		try
		{
			setChanged( true );
			fireChangeEvent( DebugEvent.STATE );
		}
		catch( DebugException e )
		{
			logError( e );
		}
	}

	/**
	 * Returns the stack frame this variable is contained in.
	 * 
	 * @return the stack frame
	 */
	protected CDebugElement getParent()
	{
		return fParent;
	}

	/**
	 * Returns the underlying CDI variable.
	 * 
	 * @return the underlying CDI variable
	 */
	protected ICDIVariable getCDIVariable()
	{
		if ( fShadow != null )
			return fShadow;
		return getOriginalCDIVariable();
	}

	/**
	 * Returns this variable's underlying CDI value.
	 */
	protected ICDIValue retrieveValue() throws DebugException, CDIException
	{
		return ( ((IDebugTarget)getParent().getDebugTarget()).isSuspended() && getCDIVariable() != null ) ? 
											getCDIVariable().getValue() : getLastKnownValue();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IVariable#getName()
	 */
	public String getName() throws DebugException
	{
		if ( fName == null )
		{
			String cdiName = ( getOriginalCDIVariable() != null ) ? getOriginalCDIVariable().getName() : null;
			fName = cdiName;
			if ( cdiName != null && getParent() instanceof ICValue )
			{
				CVariable parent = getParentVariable();
				while( parent instanceof CArrayPartition )
				{
					parent = parent.getParentVariable();
				}
				if ( parent instanceof CVariable && parent.getType().isArray() )
				{
					fName = parent.getName() + '[' + cdiName + ']';
				}
			}
		}
		return fName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IVariable#getReferenceTypeName()
	 */
	public String getReferenceTypeName() throws DebugException
	{
		return getType().getName();
	}

	protected void updateParentVariable( CValue parentValue ) throws DebugException
	{
		parentValue.getParentVariable().setChanged( true );
		parentValue.getParentVariable().fireChangeEvent( DebugEvent.STATE );
	}

	/**
	 * @see org.eclipse.cdt.debug.core.model.ICVariable#getFormat()
	 */
	public int getFormat()
	{
		return fFormat;
	}

	/**
	 * @see org.eclipse.cdt.debug.core.model.ICVariable#setFormat(int)
	 */
	public void setFormat( int format ) throws DebugException
	{
		fFormat = format;
	}

	/**
	 * @see org.eclipse.cdt.debug.core.model.ICVariable#reset()
	 */
	public void reset() throws DebugException
	{
		((CValue)getValue()).reset();
		fireChangeEvent( DebugEvent.STATE );
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.core.model.ICastToType#cast(java.lang.String)
	 */
	public void cast( String type ) throws DebugException
	{
		try
		{
			ICDIVariable newVar = createShadow( getOriginalCDIVariable().getStackFrame(), type );
			ICDIVariable oldVar = getShadow();
			setShadow( newVar );
			if ( oldVar != null )
				destroyShadow( oldVar );
		}
		catch( CDIException e )
		{
			targetRequestFailed( e.getMessage(), null );
		}
		finally
		{
			if ( fValue != null )
			{
				((CValue)fValue).dispose();
				fValue = null;
			}
			fEditable = null;
			if ( fType != null )
				fType.dispose();
			fType = null;
			fireChangeEvent( DebugEvent.STATE );
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.core.model.ICastToType#getCurrentType()
	 */
	public String getCurrentType()
	{
		try
		{
			return getReferenceTypeName();
		}
		catch( DebugException e )
		{
			logError( e );
		}
		return "";
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.core.model.ICastToType#restoreDefault()
	 */
	public void restoreDefault() throws DebugException
	{
		ICDIVariable oldVar = getShadow();
		setShadow( null );
		if ( oldVar != null )
			destroyShadow( oldVar );
		if ( fValue != null )
		{
			((CValue)fValue).dispose();
			fValue = null;
		}
		fEditable = null;
		if ( fType != null )
			fType.dispose();
		fType = null;
		fireChangeEvent( DebugEvent.STATE );
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.core.model.ICastToType#supportsCasting()
	 */
	public boolean supportsCasting()
	{
		CDebugTarget target = (CDebugTarget)getDebugTarget().getAdapter( CDebugTarget.class );
		return ( target != null && isEditable() );
	}
	
	protected ICDIVariable getOriginalCDIVariable()
	{
		return fCDIVariable;
	}

	private ICDIVariable getShadow()
	{
		return fShadow;
	}

	private void setShadow( ICDIVariable shadow )
	{
		fShadow = shadow;
	}
	
	private ICDIVariable createShadow( ICDIStackFrame cdiFrame, String type ) throws DebugException
	{
		try
		{
			ICDIVariableObject varObject = getCDISession().getVariableManager().getVariableObjectAsType( getOriginalCDIVariable(), type );
			return getCDISession().getVariableManager().createVariable( varObject );
		}
		catch( CDIException e )
		{
			targetRequestFailed( e.getMessage(), null );
		}
		return null;
	}
	
	private ICDIVariable createShadow( ICDIStackFrame cdiFrame, String type, int start, int length ) throws DebugException
	{
		try
		{
			ICDIVariableObject varObject = getCDISession().getVariableManager().getVariableObjectAsArray( getOriginalCDIVariable(), type, start, length );
			return getCDISession().getVariableManager().createVariable( varObject );
		}
		catch( CDIException e )
		{
			targetRequestFailed( e.getMessage(), null );
		}
		return null;
	}
	
	private void destroyShadow( ICDIVariable shadow ) throws DebugException
	{
		try
		{
			getCDISession().getVariableManager().destroyVariable( shadow );
		}
		catch( CDIException e )
		{
			targetRequestFailed( e.getMessage(), null );
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.core.model.ICastToType#isCasted()
	 */
	public boolean isCasted()
	{
		return ( getShadow() != null );
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.core.model.ICastToArray#castToArray(java.lang.String, int, int)
	 */
	public void castToArray( String type, int startIndex, int length ) throws DebugException
	{
		try
		{
			ICDIVariable newVar = createShadow( getOriginalCDIVariable().getStackFrame(), type, startIndex, length );
			ICDIVariable oldVar = getShadow();
			setShadow( newVar );
			if ( oldVar != null )
				destroyShadow( oldVar );
		}
		catch( CDIException e )
		{
			targetRequestFailed( e.getMessage(), null );
		}
		finally
		{
			if ( fValue != null )
			{
				((CValue)fValue).dispose();
				fValue = null;
			}
			fEditable = null;
			if ( fType != null )
				fType.dispose();
			fType = null;
			fireChangeEvent( DebugEvent.STATE );
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.core.model.ICastToArray#supportsCastToArray()
	 */
	public boolean supportsCastToArray()
	{
		CDebugTarget target = (CDebugTarget)getDebugTarget().getAdapter( CDebugTarget.class );
		return ( target != null && isEditable() && hasChildren() );
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.core.model.ICVariable#hasChildren()
	 */
	public boolean hasChildren()
	{
		try
		{
			return ( getValue() != null && getValue().hasVariables() );
		}
		catch( DebugException e )
		{
			logError( e );
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.core.model.ICVariable#isEditable()
	 */
	public boolean isEditable()
	{
		if ( fEditable == null && getCDIVariable() != null )
		{
			try
			{
				fEditable = new Boolean( getCDIVariable().isEditable() );
			}
			catch( CDIException e )
			{
				logError( e );
			}
		}
		return ( fEditable != null ) ? fEditable.booleanValue() : false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.core.model.ICVariable#getQualifiedName()
	 */
	public String getQualifiedName() throws DebugException
	{
		if ( fQualifiedName == null )
		{
			LinkedList list = new LinkedList();
			list.add( this );
			CVariable var = getParentVariable();
			while( var != null )
			{
				if ( !( var.getType().isArray() ) && !( var instanceof CArrayPartition ) && !var.isAccessSpecifier() )
					list.addFirst( var );
				var = var.getParentVariable();
			}
			StringBuffer sb = new StringBuffer();
			CVariable[] vars = (CVariable[])list.toArray( new CVariable[list.size()] );
			for ( int i = 0; i < vars.length; ++i )
			{
				sb.insert( 0, '(' );
				if ( i > 0 )
				{
					if ( vars[i - 1].getType().isPointer() )
					{
						if ( vars[i].getName().charAt( 0 ) == '*' && vars[i-1].getName().equals( vars[i].getName().substring( 1 ) ) )
						{
							sb.insert( 0, '*' );
						}
						else
						{
							sb.append( "->" );
							sb.append( vars[i].getName() );
						}
					}
					else
					{
						sb.append( '.' );
						sb.append( vars[i].getName() );
					}
				}
				else
					sb.append( vars[i].getName() );
				sb.append( ')' );
			}
			fQualifiedName = sb.toString();
		}
		return fQualifiedName;
	}

	protected boolean isAccessSpecifier() throws DebugException
	{
		return ( "public".equals( getName() ) || "protected".equals( getName() ) || "private".equals( getName() ) );
	}

	private CVariable getParentVariable() throws DebugException
	{
		if ( getParent() instanceof CValue )
			return ((CValue)getParent()).getParentVariable();
		if ( getParent() instanceof CArrayPartitionValue )
			return ((CArrayPartitionValue)getParent()).getParentVariable();
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.core.model.ICVariable#getType()
	 */
	public ICType getType() throws DebugException
	{
		if ( fType == null && getCDIVariable() != null )
		{
			try
			{
				fType = new CType( getCDIVariable().getType() );
			}
			catch( CDIException e )
			{
				requestFailed( "Type is not available.", e );
			}
		}
		return fType;
	}
}
