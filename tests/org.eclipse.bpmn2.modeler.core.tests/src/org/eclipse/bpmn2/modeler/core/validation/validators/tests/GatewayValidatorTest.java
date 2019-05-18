package org.eclipse.bpmn2.modeler.core.validation.validators.tests;

import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.Gateway;
import org.eclipse.bpmn2.GatewayDirection;
import org.eclipse.bpmn2.SequenceFlow;
import org.eclipse.bpmn2.modeler.core.validation.validators.GatewayValidator;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.validation.IValidationContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class GatewayValidatorTest {
	
	@Mock
	Gateway gateway;
	
	@Mock
	IValidationContext ctx;
	
	@Mock
	SequenceFlow flowOne;
	
	@Mock
	SequenceFlow flowTwo;
	
	@Mock
	SequenceFlow flowThree;
	
	@Mock
	EClass eClass;
	
	@Mock
	EStructuralFeature eFeature;
	
	List<SequenceFlow> zeroFlows;
	
	List<SequenceFlow> oneFlow;
	
	List<SequenceFlow> twoFlows;
	
	GatewayValidator validator;
	
	@Before
	public void setUp() {
		validator = new GatewayValidator(ctx) {
			@Override
			protected void addStatus(EObject object, String featureName, int severity, String messagePattern, Object... messageArguments) {
				// do nothing for test purposes
			}
		};
		
		zeroFlows = new ArrayList<SequenceFlow>();
		
		oneFlow = new ArrayList<SequenceFlow>();
		oneFlow.add(flowOne);
		
		twoFlows = new ArrayList<SequenceFlow>();
		twoFlows.add(flowTwo);
		twoFlows.add(flowThree);
		
		//when(eClass.getEStructuralFeature(anyString())).thenReturn(eFeature);
		//when(gateway.eClass()).thenReturn(eClass);
		
	}

	@Test
	public void testConvergingGatewayWrongSequenceCounts() throws Exception {
		when(gateway.getGatewayDirection()).thenReturn(GatewayDirection.CONVERGING);
		when(gateway.getIncoming()).thenReturn(zeroFlows);
		when(gateway.getOutgoing()).thenReturn(twoFlows);
		
		validator.validate(gateway);
		
		verify(ctx, times(2)).addResult(Bpmn2Package.eINSTANCE.getGateway_GatewayDirection());
	}
}
