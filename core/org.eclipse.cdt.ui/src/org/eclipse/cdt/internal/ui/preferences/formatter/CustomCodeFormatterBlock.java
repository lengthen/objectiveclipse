/*******************************************************************************
 * Copyright (c) 2000, 2006 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *     Sergey Prigogin, Google
 *     Anton Leherbauer (Wind River Systems)
 *******************************************************************************/

package org.eclipse.cdt.internal.ui.preferences.formatter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.CCorePreferenceConstants;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.cdt.utils.ui.controls.ControlFactory;

import org.eclipse.cdt.internal.ui.ICHelpContextIds;
import org.eclipse.cdt.internal.ui.preferences.PreferencesAccess;

/**
 * Allows to choose the formatter in a combo box.
 * If no formatter is contributed, nothing is shown.
 */
public class CustomCodeFormatterBlock extends Observable {

	private HashMap idMap = new HashMap();
	private IEclipsePreferences fPrefs;
	private String fDefaultFormatterId;
	private Combo fFormatterCombo;
	private static final String ATTR_NAME = "name"; //$NON-NLS-1$
	private static final String ATTR_ID = "id"; //$NON-NLS-1$
	private static final String DEFAULT = FormatterMessages.CustomCodeFormatterBlock_default_formatter;

	public CustomCodeFormatterBlock(PreferencesAccess access) {
		fPrefs = access.getInstanceScope().getNode(CUIPlugin.PLUGIN_ID);
		IEclipsePreferences defaults= access.getDefaultScope().getNode(CUIPlugin.PLUGIN_ID);
		fDefaultFormatterId= defaults.get(CCorePreferenceConstants.CODE_FORMATTER, null);

		initializeFormatters();
	}

	public void performOk() {
		if (fFormatterCombo == null) {
			return;
		}
		String text = fFormatterCombo.getText();
		String formatterId = (String)idMap.get(text);
		if (formatterId != null && formatterId.length() > 0) {
			fPrefs.put(CCorePreferenceConstants.CODE_FORMATTER, formatterId);
		} else {
			// simply reset to the default one.
			performDefaults();
		}
	}

	public void performDefaults() {
		if (fDefaultFormatterId != null) {
			fPrefs.put(CCorePreferenceConstants.CODE_FORMATTER, fDefaultFormatterId);
		} else {
			fPrefs.remove(CCorePreferenceConstants.CODE_FORMATTER);
		}

		if (fFormatterCombo == null) {
			return;
		}
		fFormatterCombo.clearSelection();
		fFormatterCombo.setText(DEFAULT);
		Iterator iterator = idMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry entry = (Map.Entry)iterator.next();
			String val = (String)entry.getValue();
			if (val != null && val.equals(fDefaultFormatterId)) {
				fFormatterCombo.setText((String)entry.getKey());
			}
		}
		handleFormatterChanged();
	}


	/**
	 * Get the currently selected formatter id.
	 * 
	 * @return the selected formatter id or <code>null</code> if the default is selected.
	 */
	public String getFormatterId() {
		if (fFormatterCombo == null) {
			return fPrefs.get(CCorePreferenceConstants.CODE_FORMATTER, fDefaultFormatterId);
		}
		String formatterId= (String)idMap.get(fFormatterCombo.getText());
		return formatterId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public Control createContents(Composite parent) {
		if (getNumberOfAvailableFormatters() == 0) {
			return parent;
		}
		Composite composite = ControlFactory.createGroup(parent, FormatterMessages.CustomCodeFormatterBlock_formatter_name, 1);
		((GridData)composite.getLayoutData()).horizontalSpan = 5;

		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, ICHelpContextIds.CODEFORMATTER_PREFERENCE_PAGE);

		fFormatterCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
		fFormatterCombo.setFont(parent.getFont());
		fFormatterCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fFormatterCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleFormatterChanged();
			}
		});
		Iterator items = idMap.keySet().iterator();
		while (items.hasNext()) {
			fFormatterCombo.add((String) items.next());
		}

		final String noteTitle= FormatterMessages.CustomCodeFormatterBlock_formatter_note;
		final String noteMessage= FormatterMessages.CustomCodeFormatterBlock_contributed_formatter_warning;
		ControlFactory.createNoteComposite(JFaceResources.getDialogFont(), composite, noteTitle, noteMessage);
		
		initDefault();
		
		return composite;
	}

	private void handleFormatterChanged() {
		setChanged();
		String formatterId= getFormatterId();
		notifyObservers(formatterId);
	}

	private void initDefault() {
		boolean init = false;
		String formatterID= fPrefs.get(CCorePreferenceConstants.CODE_FORMATTER, fDefaultFormatterId);
		if (formatterID != null) {
			Iterator iterator = idMap.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry entry = (Map.Entry)iterator.next();
				String val = (String)entry.getValue();
				if (val != null && val.equals(formatterID)) {
					fFormatterCombo.setText((String)entry.getKey());
					init = true;
				}
			}
		}
		if (!init) {
			formatterID= null;
			fFormatterCombo.setText(DEFAULT);
		}
	}

	private void initializeFormatters() {
		idMap = new HashMap();
		idMap.put(DEFAULT, null);
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(CCorePlugin.PLUGIN_ID, CCorePlugin.FORMATTER_EXTPOINT_ID);
		if (point != null) {
			IExtension[] exts = point.getExtensions();
			for (int i = 0; i < exts.length; i++) {
		 		IConfigurationElement[] elements = exts[i].getConfigurationElements();
		 		for (int j = 0; j < elements.length; ++j) {
		 			String name = elements[j].getAttribute(ATTR_NAME);
		 			String id= elements[j].getAttribute(ATTR_ID);
		 			idMap.put(name, id);
		 		}
			}
		}
	}
	
	private final int getNumberOfAvailableFormatters() {
		return idMap.size() - 1;
	}
}
