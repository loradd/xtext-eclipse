/*******************************************************************************
 * Copyright (c) 2010 itemis AG (http://www.itemis.eu) and others.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.xtext.ui.editor.model.edit;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;

/**
 * @author Jan Koehnlein - Initial contribution and API
 */
public interface IModificationContext {

	IXtextDocument getXtextDocument();
	
	IXtextDocument getXtextDocument(URI uri);

}
