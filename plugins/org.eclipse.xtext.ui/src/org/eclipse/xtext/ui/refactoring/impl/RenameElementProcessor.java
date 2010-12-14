/*******************************************************************************
 * Copyright (c) 2010 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.ui.refactoring.impl;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.ParticipantManager;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.xtext.resource.IExternalContentSupport;
import org.eclipse.xtext.ui.XtextProjectHelper;
import org.eclipse.xtext.ui.editor.IDirtyStateManager;
import org.eclipse.xtext.ui.refactoring.ElementRenameInfo;
import org.eclipse.xtext.ui.refactoring.IDependentElementsCalculator;
import org.eclipse.xtext.ui.refactoring.IRefactoringDocument;
import org.eclipse.xtext.ui.refactoring.IRenameStrategy;
import org.eclipse.xtext.ui.resource.IResourceSetProvider;
import org.eclipse.xtext.util.ReplaceRegion;
import org.eclipse.xtext.util.Strings;

import com.google.inject.Inject;

/**
 * @author koehnlein - Initial contribution and API
 */
public class RenameElementProcessor extends AbstractRenameProcessor {

	protected static final Logger LOG = Logger.getLogger(RenameElementProcessor.class);

	@Inject
	private IResourceSetProvider resourceSetProvider;

	@Inject
	private IExternalContentSupport externalContentSupport;

	@Inject
	private IDirtyStateManager dirtyStateManager;

	@Inject 
	private IRefactoringDocument.Provider refactoringDocumentProvider;

	@Inject
	private IDependentElementsCalculator dependentElementsCalculator;

	@Inject
	private IRenameStrategy.Provider strategyProvider;

	@Inject
	private IWorkspace workspace;
	
	@Inject
	private LocalReferenceUpdater localReferenceUpdater;
	
	@Inject
	private CompositeIndexBasedReferenceUpdater indexedReferenceUpdaterDispatcher;

	private ResourceSet resourceSet;
	private RefactoringStatus status;
	private URI targetElementURI;
	private IRefactoringDocument targetDocument;
	private EObject targetElement;
	private IRenameStrategy strategy;
	private String newName;

	private UpdateAcceptor updateAcceptor;

	@Override
	public void initialize(final URI targetElementURI) {
		status = new RefactoringStatus();
		try {
			this.targetElementURI = targetElementURI;
			this.targetDocument = refactoringDocumentProvider.get(targetElementURI, status);
			resourceSet = resourceSetProvider.get(getProject(targetElementURI));
			resourceSet.getLoadOptions().put(org.eclipse.xtext.resource.impl.ResourceDescriptionsProvider.LIVE_SCOPE, Boolean.TRUE);
			externalContentSupport.configureResourceSet(resourceSet, dirtyStateManager);
			targetElement = resourceSet.getEObject(targetElementURI, true);
			if(targetElement == null) {
				throw new RefactoringStatusException("Rename target element can not be resolved", true);
			}
			this.strategy = strategyProvider.get(targetElement, targetDocument);
		} catch (Exception e) {
			handleException(e);
		}
	}

	public IProject getProject(URI targetElementURI) {
		if (!targetElementURI.isPlatformResource())
			throw new IllegalArgumentException("Refactored element URI must be a platform resource URI: " + Strings.notNull(targetElementURI));
		String projectName = targetElementURI.segment(1);
		IProject project = workspace.getRoot().getProject(projectName);
		if(project == null) 
			throw new IllegalArgumentException("Cannot find containing project for " + Strings.notNull(targetElementURI));
		return project;
	}

	@Override
	public IRenameStrategy getRenameElementStrategy() {
		return strategy;
	}

	@Override
	public Object[] getElements() {
		return new Object[] { targetElementURI };
	}

	@Override
	public String getIdentifier() {
		return getClass().getName();
	}

	@Override
	public String getProcessorName() {
		return "Rename element";
	}

	@Override
	public boolean isApplicable() throws CoreException {
		return true;
	}

	@Override
	public void setNewName(String newName) {
		this.newName = newName;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		return status;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context)
			throws CoreException, OperationCanceledException {
		updateAcceptor = new UpdateAcceptor();
		try {
			ReplaceRegion declarationEdit = strategy.getReplaceRegion(newName);
			if (declarationEdit == null)
				throw new RefactoringStatusException("Could not create a text edit", true);
			updateAcceptor.accept(targetDocument, declarationEdit);
			ElementRenameInfo baseRenameInfo = new ElementRenameInfo(targetDocument, targetElementURI, declarationEdit.getOffset());
			Iterable<ElementRenameInfo> dependentRenameInfos = dependentElementsCalculator.getDependentElementRenameInfos(targetElement, baseRenameInfo);
			createReferenceUpdates(declarationEdit, baseRenameInfo, dependentRenameInfos);
		} catch (Exception exc) {
			handleException(exc);
		}
		return status;
	}

	protected void createReferenceUpdates(final ReplaceRegion declarationEdit, ElementRenameInfo baseRenameInfo,
			Iterable<ElementRenameInfo> dependentRenameInfos) {
		RefactoringStatus localReferencesStatus = localReferenceUpdater.createReferenceUpdates(baseRenameInfo, dependentRenameInfos, declarationEdit, resourceSet, updateAcceptor);
		status.merge(localReferencesStatus);
		RefactoringStatus externalReferenceStatus = indexedReferenceUpdaterDispatcher.createReferenceUpdates(baseRenameInfo, dependentRenameInfos, declarationEdit, resourceSet, updateAcceptor);
		status.merge(externalReferenceStatus);
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return updateAcceptor.createChange("Rename " + strategy.getCurrentName() + " to " + newName);
	}

	@Override
	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants sharedParticipants)
			throws CoreException {
		RenameArguments renameArguments = new RenameArguments(newName, false);
		RenameParticipant[] renameParticipants = ParticipantManager.loadRenameParticipants(status, this,
				targetElementURI, renameArguments, new String[] { XtextProjectHelper.NATURE_ID }, sharedParticipants);
		return renameParticipants;
	}

	protected void handleException(Exception e) {
		if (e instanceof RefactoringStatusException) {
			((RefactoringStatusException) e).reportToStatus(status);
		} else {
			status.addFatalError("Error during refactoring. See log for details");
			LOG.error(e);
		}
	}
}
