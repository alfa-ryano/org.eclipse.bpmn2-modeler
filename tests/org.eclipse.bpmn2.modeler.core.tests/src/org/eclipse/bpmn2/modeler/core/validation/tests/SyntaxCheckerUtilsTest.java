package org.eclipse.bpmn2.modeler.core.validation.tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.eclipse.bpmn2.modeler.core.validation.SyntaxCheckerUtils;
import org.junit.Test;

public class SyntaxCheckerUtilsTest {
	
	@Test
	public void testIsJavaIdentifier() throws Exception {
		assertTrue( SyntaxCheckerUtils.isJavaIdentifier("_ab") );
		assertTrue( SyntaxCheckerUtils.isJavaIdentifier("a1b") );
		assertTrue( SyntaxCheckerUtils.isJavaIdentifier("_ab") );
	}
        /*
	@Test
	public void testIsJavaIdentifierUTF8() throws Exception {
		assertTrue( SyntaxCheckerUtils.isJavaIdentifier("形声") );
	}
        */
	@Test
	public void testIsJavaIdentifierNegative() throws Exception {
		assertFalse( SyntaxCheckerUtils.isJavaIdentifier("_a b") );
		assertFalse( SyntaxCheckerUtils.isJavaIdentifier("1ab") );
		assertFalse( SyntaxCheckerUtils.isJavaIdentifier("!ab") );
		assertFalse( SyntaxCheckerUtils.isJavaIdentifier("*") );
		assertFalse( SyntaxCheckerUtils.isJavaIdentifier("+") );
		assertFalse( SyntaxCheckerUtils.isJavaIdentifier("-") );
		assertFalse( SyntaxCheckerUtils.isJavaIdentifier("a+") );
		assertFalse( SyntaxCheckerUtils.isJavaIdentifier("a#") );
	}
}
