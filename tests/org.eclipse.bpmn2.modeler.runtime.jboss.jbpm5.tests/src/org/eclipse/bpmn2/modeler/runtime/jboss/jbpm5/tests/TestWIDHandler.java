/*******************************************************************************
 * Copyright (c) 2011, 2012 Red Hat, Inc.
 *  All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.wid.WIDException;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.wid.WIDParser;
import org.eclipse.bpmn2.modeler.runtime.jboss.jbpm5.wid.WorkItemDefinition;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;


/**
 * Basic tests for the WIDHandler
 * @author bfitzpat
 *
 */
public class TestWIDHandler {

	private String getWidFile( String filepath ) {
		Bundle bundle = Activator.getDefault().getBundle();
		IPath path = new Path("widfiles/"+filepath);
		URL setupUrl = FileLocator.find(bundle, path, Collections.EMPTY_MAP);
		File setupFile = null;
		try {
			setupFile = new File(FileLocator.toFileURL(setupUrl).toURI());
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		filepath = setupFile.getAbsolutePath();
		
		StringBuilder text = new StringBuilder();
	    String NL = System.getProperty("line.separator");
	    Scanner scanner = null;
	    try {
	    	scanner = new Scanner(new FileInputStream(filepath), "UTF-8");
	    	while (scanner.hasNextLine()){
	    		text.append(scanner.nextLine() + NL);
	    	}
	    	return text.toString();
	    } catch (FileNotFoundException e) {
	    	e.printStackTrace();
	    } finally {
	    	if (scanner != null)
	    		scanner.close();
	    }	
	    return null;
	}
	
	@Test
	public void testBasic() {
		System.out.println("testBasic: logemail.wid");
		String content = getWidFile("logemail.wid");
		List<WorkItemDefinition> widMap = new ArrayList<WorkItemDefinition>();
		try {
			widMap = WIDParser.parse(content);
		} catch (WIDException e) {
			Assert.fail("Failed with exception " + e.getMessage());
		}
		Assert.assertTrue(!widMap.isEmpty());
		java.util.Iterator<WorkItemDefinition> widIterator = widMap.iterator();
		while(widIterator.hasNext())
			System.out.println(widIterator.next().toString());
	}
	
	@Test
	public void testComplex() {
		System.out.println("testComplex: widfiles/Email.wid");
		String content = getWidFile("Email.wid");
		List<WorkItemDefinition> widMap = new ArrayList<WorkItemDefinition>();
		try {
			widMap = WIDParser.parse(content);
		} catch (WIDException e) {
			Assert.fail("Failed with exception " + e.getMessage());
		}
		Assert.assertTrue(!widMap.isEmpty());
		java.util.Iterator<WorkItemDefinition> widIterator = widMap.iterator();
		while(widIterator.hasNext()) {
			WorkItemDefinition wid = widIterator.next();
			Assert.assertTrue(wid.getEclipseCustomEditor() != null &&
					wid.getEclipseCustomEditor().trim().length() > 0);
			System.out.println(wid.toString());
		};
	}

	@Test
	public void testResults() {
		System.out.println("testResults: widfiles/java.wid");
		String content = getWidFile("java.wid");
		List<WorkItemDefinition> widMap = new ArrayList<WorkItemDefinition>();
		try {
			widMap = WIDParser.parse(content);
		} catch (WIDException e) {
			Assert.fail("Failed with exception " + e.getMessage());
		}
		Assert.assertTrue(!widMap.isEmpty());
		java.util.Iterator<WorkItemDefinition> widIterator = widMap.iterator();
		while(widIterator.hasNext()) {
			WorkItemDefinition wid = widIterator.next();
			Assert.assertTrue(!wid.getResults().isEmpty());
			System.out.println(wid.toString());
		}
	}

	@Test
	public void testFail() {
		System.out.println("testFail: no wid");
		List<WorkItemDefinition> widMap = new ArrayList<WorkItemDefinition>();
		try {
			widMap = WIDParser.parse(null);
		} catch (WIDException e) {
			Assert.assertTrue(e != null);
		}
	}
}
