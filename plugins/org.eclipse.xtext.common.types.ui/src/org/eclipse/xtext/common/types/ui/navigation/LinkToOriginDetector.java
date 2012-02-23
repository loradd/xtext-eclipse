/*******************************************************************************
 * Copyright (c) 2011 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.common.types.ui.navigation;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.xtext.generator.trace.ILocationInResource;
import org.eclipse.xtext.generator.trace.ITrace;
import org.eclipse.xtext.generator.trace.ITraceInformation;
import org.eclipse.xtext.util.TextRegion;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Navigates to the original source element if the selected Java type was generated
 * from an Xbase language (e.g. {@link ITraceInformation} is available).
 * 
 * @author Sebastian Zarnekow - Initial contribution and API
 */
public class LinkToOriginDetector extends AbstractHyperlinkDetector {

	@Inject
	private Provider<LinkToOrigin> hyperlinkProvider;
	
	@Inject
	private ITraceInformation traceInformation;
	
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		try {
			// very pessimistic guards - most things should never happen
			ITextEditor textEditor = (ITextEditor) getAdapter(ITextEditor.class);
			if (textEditor == null)
				return null;
			IEditorInput editorInput = textEditor.getEditorInput();
			if (editorInput == null)
				return null;
			IJavaElement adaptedJavaElement = (IJavaElement) Platform.getAdapterManager().getAdapter(editorInput, IJavaElement.class);
			if (adaptedJavaElement == null)
				return null;
			ICompilationUnit compilationUnit = (ICompilationUnit) adaptedJavaElement.getAncestor(IJavaElement.COMPILATION_UNIT);
			if (compilationUnit == null)
				return null;
			try {
				IRegion selectedWord = org.eclipse.jdt.internal.ui.text.JavaWordFinder.findWord(
						textViewer.getDocument(), region.getOffset());
				// the actual implementation - find the referenced Java type under the cursor and link
				// to its origin if it's contained in a 'derived' resource
				IJavaElement[] javaElements = compilationUnit.codeSelect(selectedWord.getOffset(), selectedWord.getLength());
				for(IJavaElement javaElement: javaElements) {
					if (javaElement instanceof IMember) {
						IMember selectedMember = (IMember) javaElement;
						IResource resource = selectedMember.getResource();
						if (resource instanceof IFile) {
							ITrace traceToSource = traceInformation.getTraceToSource((IStorage) resource);
							if (traceToSource == null) {
								return null;
							}
							Iterable<ILocationInResource> sourceInformation = traceToSource.getAllAssociatedLocations(new TextRegion(selectedWord.getOffset(), selectedWord.getLength()));
							List<ILocationInResource> sourceInformationAsList = Lists.newArrayList(sourceInformation);
							if (!canShowMultipleHyperlinks && sourceInformationAsList.size() > 1)
								return null;
							List<IHyperlink> result = Lists.newArrayListWithCapacity(sourceInformationAsList.size());
							for(ILocationInResource source: sourceInformationAsList) {
								try {
									URI resourceURI = source.getResourceURI();
									if (resourceURI != null) {
										LinkToOrigin hyperlink = hyperlinkProvider.get();
										hyperlink.setHyperlinkRegion(new Region(selectedWord.getOffset(), selectedWord.getLength()));
										hyperlink.setURI(resourceURI);
										hyperlink.setHyperlinkText("Go to " + resourceURI.lastSegment());
										hyperlink.setTypeLabel("Navigate to source artifact");
										hyperlink.setMember(selectedMember);
										result.add(hyperlink);
									}
								} catch(IllegalArgumentException e) { /* invalid URI - ignore */ }
							}
							if (result.isEmpty())
								return null;
							return result.toArray(new IHyperlink[result.size()]);
						}
					}
				}
				return null;
			} catch (JavaModelException e) {
				return null;
			}
		} catch(Throwable t) {
			return null;
		}
	}

}
