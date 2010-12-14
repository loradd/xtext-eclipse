/*******************************************************************************
 * Copyright (c) 2010 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.ui.tests.refactoring;

import static org.eclipse.xtext.ui.junit.util.IResourcesSetupUtil.*;
import static org.eclipse.xtext.ui.junit.util.JavaProjectSetupUtil.*;

import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.xtext.ui.XtextProjectHelper;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.junit.util.IResourcesSetupUtil;
import org.eclipse.xtext.ui.junit.util.JavaProjectSetupUtil;
import org.eclipse.xtext.ui.refactoring.impl.RenameElementProcessor;
import org.eclipse.xtext.ui.tests.Activator;
import org.eclipse.xtext.ui.tests.editor.AbstractEditorTest;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

/**
 * @author koehnlein - Initial contribution and API
 */
public class DefaultRenameElementProcessorTest extends AbstractEditorTest {

	private IJavaProject project;

	@Inject
	private Provider<RenameElementProcessor> processorProvider;
	
	private static final String TEST_PROJECT = "refactoring.test";
	private static final String TEST_FILE_NAME = TEST_PROJECT + "/" + "File.refactoringtestlanguage";

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		project = makeJavaProject(createProject(TEST_PROJECT));
		addNature(project.getProject(), XtextProjectHelper.NATURE_ID);
		Injector injector = Activator.getInstance().getInjector(getEditorId());
		injector.injectMembers(this);
	}

	@Override
	protected String getEditorId() {
		return "org.eclipse.xtext.ui.tests.refactoring.RefactoringTestLanguage";
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		if (project != null)
			JavaProjectSetupUtil.deleteJavaProject(project);
	}

	public void testFileRename() throws Exception {
		String initialModel = "B A { ref B }";
		IFile testFile = IResourcesSetupUtil.createFile(TEST_FILE_NAME, initialModel);
		URI targetElementURI = URI.createPlatformResourceURI(testFile.getFullPath().toString(), true).appendFragment("//@elements.0");
		rename(targetElementURI, "C");
		String model = readFile(testFile);
		assertEquals(initialModel.replaceAll("B", "C"), model);
	}

	public void testEditorRename() throws Exception {
		String initialModel = "B A { ref B }";
		IFile testFile = IResourcesSetupUtil.createFile(TEST_FILE_NAME, initialModel);
		XtextEditor editor = openEditor(testFile);
		assertFalse(editor.isDirty());
		URI targetElementURI = URI.createPlatformResourceURI(testFile.getFullPath().toString(), true).appendFragment("//@elements.0");
		rename(targetElementURI, "C");
		assertTrue(editor.isDirty());
		assertEquals(initialModel.replaceAll("B", "C"), editor.getDocument().get());	
		rename(targetElementURI, "B");
		assertTrue(editor.isDirty());
		assertEquals(initialModel, editor.getDocument().get());
	}
	
	protected void rename(URI targetElementURI, String newName) throws CoreException, Exception {
		RenameElementProcessor processor = processorProvider.get();
		processor.initialize(targetElementURI);
		RefactoringStatus initialStatus = processor.checkInitialConditions(new NullProgressMonitor());
		assertTrue(initialStatus.isOK());
		processor.setNewName(newName);
		RefactoringStatus finalStatus = processor.checkFinalConditions(new NullProgressMonitor(), null);
		assertTrue(finalStatus.isOK());
		Change change = processor.createChange(new NullProgressMonitor());
		assertNotNull(change);
		change.perform(new NullProgressMonitor());
	}

	protected String readFile(IFile file) throws Exception {
		InputStream inputStream = file.getContents();
		try {
			byte[] buffer = new byte[2048];
			int bytesRead = 0;
			StringBuffer b = new StringBuffer();
			do {
				bytesRead = inputStream.read(buffer);
				if (bytesRead != -1)
					b.append(new String(buffer, 0, bytesRead));
			} while (bytesRead != -1);
			return b.toString();
		} finally {
			inputStream.close();
		}
	}
}
