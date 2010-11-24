/*******************************************************************************
 * Copyright (c) 2010 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.xtext.ui.editor.folding;

import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.Position;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.folding.DefaultFoldingRegionProvider;
import org.eclipse.xtext.ui.editor.folding.IFoldingRegion;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;

import com.google.common.collect.Lists;

/**
 * @author Sven Efftinge - Initial contribution and API
 */
public class XtextGrammarFoldingRegionProvider extends DefaultFoldingRegionProvider {

	@Override
	protected List<IFoldingRegion> doGetFoldingRegions(IXtextDocument xtextDocument, XtextResource xtextResource) {
		List<IFoldingRegion> result = Lists.newArrayList();
		if (xtextResource==null || xtextResource.getContents().isEmpty())
			return result;
		EList<EObject> contents = xtextResource.getContents().get(0).eContents();
		for (EObject eObject : contents) {
			Position position = getPosition(xtextDocument, NodeModelUtils.getNode(eObject));
			if (position != null) {
				result.addAll(createFoldingRegions(eObject, position));
			}
		}
		return result;
	}
}
