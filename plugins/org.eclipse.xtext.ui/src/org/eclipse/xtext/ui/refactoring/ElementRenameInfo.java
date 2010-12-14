/*******************************************************************************
 * Copyright (c) 2010 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.ui.refactoring;


import org.eclipse.emf.common.util.URI;


/**
 * Information on an element to be renamed.  
 *  
 * @author koehnlein - Initial contribution and API
 */
public class ElementRenameInfo {

	private final URI elementURI;
	
	private final int offset;

	private final IRefactoringDocument document;
	
	public ElementRenameInfo(IRefactoringDocument document, URI renamedElementURI, int offset) {
		super();
		this.document = document;
		this.elementURI = renamedElementURI;
		this.offset = offset;
	}

	public int getOffset() {
		return offset;
	}
	
	public URI getElementURI() {
		return elementURI;
	}

	public IRefactoringDocument getDocument() {
		return document;
	}
}
