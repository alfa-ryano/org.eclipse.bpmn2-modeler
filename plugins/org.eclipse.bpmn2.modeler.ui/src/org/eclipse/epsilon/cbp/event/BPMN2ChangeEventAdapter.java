package org.eclipse.epsilon.cbp.event;

import org.eclipse.bpmn2.impl.DefinitionsImpl;
import org.eclipse.bpmn2.modeler.core.preferences.Bpmn2Preferences;
import org.eclipse.bpmn2.util.Bpmn2Resource;
import org.eclipse.bpmn2.util.Bpmn2ResourceImpl;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.epsilon.cbp.resource.CBPResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.BasicEList.UnmodifiableEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.emf.ecore.util.EContentsEList;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.DanglingHREFException;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.epsilon.cbp.history.ModelHistory;

public class BPMN2ChangeEventAdapter extends ChangeEventAdapter {

    protected Bpmn2ResourceImpl resource = null;
    protected Map<EObject, String> eObject2IdMap = new LinkedHashMap<>();
    protected Map<String, EObject> id2EObjectMap = new LinkedHashMap<>();
    protected boolean isActive = true;

    protected EObject monitoredObject;

    public boolean isActive() {
	return isActive;
    }

    public void setActive(boolean isActive) {
	this.isActive = isActive;
    }

    public CBPResource getResource() {
	return super.getResource();
    }

    public Bpmn2Resource getBPMN2Resource() {
	return this.resource;
    }

    public BPMN2ChangeEventAdapter(Bpmn2Resource resource) {
	super(null);
	this.resource = (Bpmn2ResourceImpl) resource;
    }

    @Override
    public void notifyChanged(Notification n) {
	if (isActive) {
	    super.notifyChanged(n);
	}
    }

    public void handleEPackageOf(EObject eObject) {
	EPackage ePackage = eObject.eClass().getEPackage();
	if (!ePackage.getNsURI().contains("MODEL-XMI")) {
	    return;
	}
	if (!ePackages.contains(ePackage)) {
	    ePackages.add(ePackage);
	    ChangeEvent<?> event = new RegisterEPackageEvent(ePackage, this);
	    event.setComposite(compositeId);
	    packageCount++;
	    changeEvents.add(event);
	    // this.addToModelHistory(event, -1);
	}
    }

    @Override
    protected void addEventToList(ChangeEvent<?> event, Notification n, int position) {

	// Features which are not meant to be serialised are defined as "unset"
	if (event instanceof SetEReferenceEvent || event instanceof AddToEReferenceEvent) {
	    EStructuralFeature feature = (EStructuralFeature) n.getFeature();
	    if (!((EObject) n.getNotifier()).eIsSet(feature))
		return;
	}

	if (event instanceof EObjectValuesEvent) {
	    if (((EObjectValuesEvent) event).getValues().isEmpty()) {
	    } else {
		for (EObject obj : ((EObjectValuesEvent) event).getValues()) {
		    handleEPackageOf(obj);
		    if (n.getNotifier() instanceof Resource && (Resource) n.getNotifier() == obj.eResource()) {
			handleCreateEObject(obj);
		    } else if (n.getNotifier() instanceof EObject && ((EObject) n.getNotifier()).eResource() == obj.eResource()) {
			handleCreateEObject(obj);
		    } else if (n.getNotifier() instanceof EObject) {
			handleCreateEObject(obj);
		    }
		}
	    }
	}

	if (event instanceof EStructuralFeatureEvent<?>) {
	    ((EStructuralFeatureEvent<?>) event).setEStructuralFeature((EStructuralFeature) n.getFeature());
	    ((EStructuralFeatureEvent<?>) event).setTarget(n.getNotifier());
	}

	// handleOppositeReference(event);

	if (event != null) {
	    if (position > 0) {
		event.setPosition(position);
	    } else {
		event.setPosition(n.getPosition());
	    }

	    if (event instanceof SetEReferenceEvent) {
		if (n.getOldValue() instanceof EObject || n.getOldValue() instanceof EList) {
		    if (n.getOldValue() instanceof EObject) {
			handleDeletedEObject(event, (EObject) n.getOldValue());
		    } else if (n.getOldValue() instanceof EList) {
			handleDeletedEObject(event, (EObject) event.getValue());
		    }
		} else {
		    changeEvents.add(event);
		}
	    } else if (!(event instanceof UnsetEReferenceEvent) && !(event instanceof RemoveFromResourceEvent) && !(event instanceof RemoveFromEReferenceEvent)) {
		changeEvents.add(event);
	    }
	    // this.addToModelHistory(event, position);
	}

	if (n.getOldValue() instanceof EObject || n.getOldValue() instanceof EList) {
	    if (event instanceof UnsetEReferenceEvent || event instanceof RemoveFromResourceEvent || event instanceof RemoveFromEReferenceEvent) {
		if (n.getOldValue() instanceof EObject) {
		    handleDeletedEObject(event, (EObject) n.getOldValue());
		} else if (n.getOldValue() instanceof EList) {
		    handleDeletedEObject(event, (EObject) event.getValue());
		}
	    }
	}

	// handleCompositeMove
	if ((event instanceof SetEReferenceEvent || event instanceof AddToEReferenceEvent || event instanceof AddToResourceEvent) && changeEvents.size() > 1) {
	    ChangeEvent<?> previousEvent = changeEvents.get(changeEvents.size() - 2);
	    if (previousEvent instanceof UnsetEReferenceEvent || previousEvent instanceof RemoveFromEReferenceEvent || previousEvent instanceof RemoveFromResourceEvent) {
		if ((event instanceof EReferenceEvent && ((EReferenceEvent) event).getEReference().isContainment()) || event instanceof AddToResourceEvent) {
		    Boolean localComposite = null;
		    if (compositeId == null) {
			localComposite = true;
			startCompositeOperation();
		    }
		    previousEvent.setComposite(compositeId);
		    event.setComposite(compositeId);
		    if (localComposite != null && localComposite == true) {
			endCompositeOperation();
		    }
		}
	    }
	}
	// ----
    }

    @Override
    public void removedContainedObjects(EObject targetEObject, Set<EObject> visitedObjects) {
	visitedObjects.add(targetEObject);
	for (EReference eRef : targetEObject.eClass().getEAllContainments()) {
	    if (targetEObject.eIsSet(eRef) && eRef.isChangeable() && !eRef.isDerived()) {
		if (eRef.isMany()) {
		    EList<EObject> values = (EList<EObject>) targetEObject.eGet(eRef);
		    while (values.size() > 0) {

			int position = values.size() - 1;
			EObject value = values.get(position);

			if (visitedObjects.contains(value)) {
			    continue;
			}
			removedContainedObjects(value, visitedObjects);
			// removeAllReferencingFeatures(value);
			unsetAllEFeatures(value);
			values.remove(position);

			RemoveFromEReferenceEvent e = new RemoveFromEReferenceEvent();
			e.setComposite(compositeId);
			e.setEStructuralFeature(eRef);
			e.setValue(value);
			e.setTarget(targetEObject);
			e.setPosition(position);
			changeEvents.add(e);

			String id = null;
			if (resource != null) {
			    id = resource.getURIFragment(value);
			    if ("/-1".equals(id) || id == null) {
				id = eObject2IdMap.get(value);
			    }
			} else {
			    id = eObject2IdMap.get(value);
			}
			ChangeEvent<?> deletedEvent = new DeleteEObjectEvent(value, id);
			deletedEvent.setComposite(compositeId);
			changeEvents.add(deletedEvent);
		    }
		} else {
		    if (!eRef.isUnsettable()) {
			EObject value = (EObject) targetEObject.eGet(eRef);
			if (value != null) {

			    if (visitedObjects.contains(value)) {
				continue;
			    }
			    removedContainedObjects(value, visitedObjects);
			    // removeAllReferencingFeatures(value);
			    unsetAllEFeatures(value);
			    targetEObject.eUnset(eRef);

			    UnsetEReferenceEvent e = new UnsetEReferenceEvent();
			    e.setComposite(compositeId);
			    e.setEStructuralFeature(eRef);
			    e.setOldValue(value);
			    e.setTarget(targetEObject);
			    changeEvents.add(e);

			    String id = null;
			    if (resource != null) {
				id = resource.getURIFragment(value);
				if ("/-1".equals(id) || id == null) {
				    id = eObject2IdMap.get(value);
				}
			    } else {
				id = eObject2IdMap.get(value);
			    }
			    ChangeEvent<?> deletedEvent = new DeleteEObjectEvent(value, id);
			    deletedEvent.setComposite(compositeId);
			    changeEvents.add(deletedEvent);
			}
		    }
		}
	    }
	}
    }

    @Override
    protected void unsetAllEFeatures(EObject targetEObject) {
	for (EAttribute eAtt : targetEObject.eClass().getEAllAttributes()) {
	    if (targetEObject.eIsSet(eAtt) && eAtt.isChangeable() && !eAtt.isDerived()) {
		if (eAtt.isMany()) {
		    EList<Object> values = (EList<Object>) targetEObject.eGet(eAtt);
		    while (values.size() > 0) {
			int position = values.size() - 1;
			Object value = values.remove(position);

			RemoveFromEAttributeEvent e = new RemoveFromEAttributeEvent();
			e.setComposite(compositeId);
			e.setEStructuralFeature(eAtt);
			e.setValue(value);
			e.setTarget(targetEObject);
			e.setPosition(position);
			changeEvents.add(e);
		    }
		} else {
		    if (!eAtt.isUnsettable()) {
			Object value = targetEObject.eGet(eAtt);
			if (value != null) {
			    targetEObject.eUnset(eAtt);

			    UnsetEAttributeEvent e = new UnsetEAttributeEvent();
			    e.setComposite(compositeId);
			    e.setEStructuralFeature(eAtt);
			    e.setOldValue(value);
			    e.setTarget(targetEObject);
			    changeEvents.add(e);
			}
		    }
		}
	    }
	}

	for (EReference eRef : targetEObject.eClass().getEAllReferences()) {
	    try {
		targetEObject.eIsSet(eRef);
	    } catch (Exception e) {
		continue;
	    }
	    if (!eRef.isContainment() && targetEObject.eIsSet(eRef) && eRef.isChangeable() && !eRef.isDerived()) {
		if (eRef.isMany()) {
		    EList<EObject> values = (EList<EObject>) targetEObject.eGet(eRef);
		    while (values.size() > 0) {
			int position = values.size() - 1;
			EObject value = values.remove(position);

			RemoveFromEReferenceEvent e = new RemoveFromEReferenceEvent();
			e.setComposite(compositeId);
			e.setEStructuralFeature(eRef);
			e.setValue(value);
			e.setTarget(targetEObject);
			e.setPosition(position);
			changeEvents.add(e);
		    }
		} else {
		    if (!eRef.isUnsettable()) {
			EObject value = (EObject) targetEObject.eGet(eRef);
			if (value != null) {
			    targetEObject.eUnset(eRef);

			    UnsetEReferenceEvent e = new UnsetEReferenceEvent();
			    e.setComposite(compositeId);
			    e.setEStructuralFeature(eRef);
			    e.setOldValue(value);
			    e.setTarget(targetEObject);
			    changeEvents.add(e);
			}
		    }
		}
	    }
	}
    }

    @Override
    public void handleDeletedEObject(ChangeEvent<?> event, EObject removedObject, Object parent, Object feature) {
	if (removedObject.eResource() == null
	// && removedObject.eAdapters().stream().noneMatch(adapter -> adapter
	// instanceof ChangeEventAdapter)
	) {
	    // System.out.println(removedObject + " : " +
	    // removedObject.eCrossReferences().size());
	    // && (removedObject.eCrossReferences() == null ||
	    // removedObject.eCrossReferences().size() == 0)) {

	    Boolean localComposite = null;
	    if (compositeId == null) {
		localComposite = true;
		startCompositeOperation();
	    }
	    event.setComposite(compositeId);

	    Set<EObject> visitedObjects = new HashSet<>();
	    removedContainedObjects(removedObject, visitedObjects);
	    visitedObjects.clear();
	    unsetAllEFeatures(removedObject);
	    // unsetOppositeEReference(event, removedObject);

	    changeEvents.add(event);

	    String id = null;
		if (resource != null) {
		    id = resource.getURIFragment(removedObject);
		    if ("/-1".equals(id) || id == null) {
			id = eObject2IdMap.get(removedObject);
		    }
		}else {
		    id = eObject2IdMap.get(removedObject);
		}
	    ChangeEvent<?> deletedEvent = new DeleteEObjectEvent(removedObject, id);
	    // eObject2IdMap.remove(removedObject);
	    // id2EObjectMap.remove(id);
	    deletedEvent.setComposite(compositeId);
	    changeEvents.add(deletedEvent);
	    if (localComposite != null && localComposite == true) {
		endCompositeOperation();
	    }

	} else {
	    changeEvents.add(event);
	}
    }

    @SuppressWarnings("unchecked")
    public void handleCreateEObject(EObject obj) {
	if (!isRegistered(obj)) {
	    // if (resource.getURIFragment(obj).contains("/")) {
	    // return;
	    // }
	    // if (!obj.eClass().getEPackage().getNsURI().contains("MODEL-XMI"))
	    // {
	    // return;
	    // }
	    System.out.println(resource.getURIFragment(obj));
	    ChangeEvent<?> event = new CreateEObjectEvent(obj, register(obj, resource.getURIFragment(obj)));
	    event.setComposite(compositeId);
	    createCount++;
	    changeEvents.add(event);
	    // this.addToModelHistory(event, -1);

	    // Include prior attribute values into the resource
	    for (EAttribute eAttr : obj.eClass().getEAllAttributes()) {
		if (eAttr.isChangeable() && obj.eIsSet(eAttr) && !eAttr.isDerived()) {

		    System.out.println("+--- " + eAttr.getName());

		    if (eAttr.isMany()) {
			Collection<?> values = (Collection<?>) obj.eGet(eAttr);
			int i = 0;
			for (Object value : values) {
			    AddToEAttributeEvent e = new AddToEAttributeEvent();
			    event.setComposite(compositeId);
			    addAttCount++;
			    e.setEStructuralFeature(eAttr);
			    e.setValue(value);
			    e.setTarget(obj);
			    e.setPosition(i++);
			    changeEvents.add(e);
			    // this.addToModelHistory(e, -1);
			}
		    } else {
			Object value = obj.eGet(eAttr);
			SetEAttributeEvent e = new SetEAttributeEvent();
			event.setComposite(compositeId);
			setAttCount++;
			e.setEStructuralFeature(eAttr);
			e.setValue(value);
			e.setTarget(obj);
			changeEvents.add(e);
			// this.addToModelHistory(e, -1);
		    }
		}
	    }

	    // Include prior reference values into the resource
	    for (EReference eRef : obj.eClass().getEAllReferences()) {

		if (eRef.isChangeable() && obj.eIsSet(eRef) && !eRef.isDerived()) {
		    if (eRef.getEOpposite() != null && eRef.getEOpposite().isMany() && eRef.getEOpposite().isChangeable()) {
			// If this is the "1" side of an 1:N pair of references,
			// ignore it:
			// the "N" side has more information.
			continue;
		    }

		    System.out.println("+--- " + eRef.getName());

		    if (eRef.isMany()) {
			Collection<EObject> values = (Collection<EObject>) obj.eGet(eRef);
			int i = 0;
			for (EObject value : values) {
			    if (value.eResource() == obj.eResource()) {
				handleCreateEObject(value);
			    }

			    AddToEReferenceEvent e = new AddToEReferenceEvent();
			    e.setComposite(compositeId);
			    addRefCount++;
			    e.setEStructuralFeature(eRef);
			    e.setValue(value);
			    e.setTarget(obj);
			    e.setPosition(i++);
			    changeEvents.add(e);
			    // this.addToModelHistory(e, -1);
			}
		    } else {
			EObject value = (EObject) obj.eGet(eRef);
			if (value.eResource() == obj.eResource()) {
			    handleCreateEObject(value);
			}
			SetEReferenceEvent e = new SetEReferenceEvent();
			e.setComposite(compositeId);
			setRefCount++;
			e.setEStructuralFeature(eRef);
			e.setValue(value);
			e.setTarget(obj);
			changeEvents.add(e);
		    }
		}
	    }
	}
    }

    public boolean isCbpExisted() {
	String path = resource.getURI().toString();
	IProject project = Bpmn2Preferences.getActiveProject();
	IFile bpmnFile = project.getFile(path);
	IPath x = bpmnFile.getProjectRelativePath();
	String a = x.toString().replace(bpmnFile.getName(), "");
	IPath iPath = bpmnFile.getLocation();
	iPath = iPath.removeFileExtension().addFileExtension("cbpxml");
	String realPath = iPath.toString().replace(a, "");
	File cbpFile = new File(realPath);
	return cbpFile.exists();
    }

    public void saveCbp() {
	try {
	    String path = resource.getURI().toString();
	    IProject project = Bpmn2Preferences.getActiveProject();
	    IFile bpmnFile = project.getFile(path);
	    IPath x = bpmnFile.getProjectRelativePath();
	    String a = x.toString().replace(bpmnFile.getName(), "");
	    IPath iPath = bpmnFile.getLocation();
	    iPath = iPath.removeFileExtension().addFileExtension("cbpxml");
	    String realPath = iPath.toString().replace(a, "");
	    FileOutputStream os = new FileOutputStream(realPath, true);
	    this.saveCbp(os);
	    os.flush();
	    os.close();
	    project.refreshLocal(IResource.DEPTH_INFINITE, null);
	} catch (FileNotFoundException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	} catch (CoreException e) {
	    e.printStackTrace();
	}
    }

    public void saveCbp(OutputStream out) throws IOException {

	try {
	    DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	    TransformerFactory transformerFactory = TransformerFactory.newInstance();
	    Transformer transformer = transformerFactory.newTransformer();
	    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

	    int eventNumber = 0;

	    for (ChangeEvent<?> event : this.getChangeEvents()) {

		Document document = documentBuilder.newDocument();
		Element e = null;

		if (event instanceof StartNewSessionEvent) {
		    StartNewSessionEvent s = ((StartNewSessionEvent) event);
		    e = document.createElement("session");
		    e.setAttribute("id", s.getSessionId());
		    e.setAttribute("time", s.getTime());
		} else if (event instanceof RegisterEPackageEvent) {
		    RegisterEPackageEvent r = ((RegisterEPackageEvent) event);
		    e = document.createElement("register");
		    e.setAttribute("epackage", r.getEPackage().getNsURI());
		} else if (event instanceof CreateEObjectEvent) {
		    e = document.createElement("create");
		    e.setAttribute("epackage", ((CreateEObjectEvent) event).getEClass().getEPackage().getNsURI());
		    e.setAttribute("eclass", ((CreateEObjectEvent) event).getEClass().getName());
		    String id = ((CreateEObjectEvent) event).getId();
		    if (id.contains("/")) {
			id = resource.getURIFragment((EObject) event.getValue());
		    }
		    e.setAttribute("id", id);
		    EObject eObject = ((CreateEObjectEvent) event).getValue();
		} else if (event instanceof DeleteEObjectEvent) {
		    e = document.createElement("delete");
		    e.setAttribute("epackage", ((DeleteEObjectEvent) event).getEClass().getEPackage().getNsURI());
		    e.setAttribute("eclass", ((DeleteEObjectEvent) event).getEClass().getName());
		    e.setAttribute("id", ((DeleteEObjectEvent) event).getId());
		    EObject eObject = ((DeleteEObjectEvent) event).getValue();
		} else if (event instanceof AddToResourceEvent) {
		    e = document.createElement("add-to-resource");
		    EObject eObject = ((AddToResourceEvent) event).getValue();
		    e.setAttribute("eclass", eObject.eClass().getName());
		} else if (event instanceof RemoveFromResourceEvent) {
		    e = document.createElement("remove-from-resource");
		    EObject eObject = ((RemoveFromResourceEvent) event).getValue();
		    e.setAttribute("eclass", eObject.eClass().getName());
		} else if (event instanceof AddToEReferenceEvent) {
		    e = document.createElement("add-to-ereference");
		    EObject eObject = ((AddToEReferenceEvent) event).getTarget();
		    e.setAttribute("eclass", eObject.eClass().getName());
		    EObject value = ((AddToEReferenceEvent) event).getValue();
		} else if (event instanceof RemoveFromEReferenceEvent) {
		    e = document.createElement("remove-from-ereference");
		    EObject eObject = ((RemoveFromEReferenceEvent) event).getTarget();
		    e.setAttribute("eclass", eObject.eClass().getName());
		    EObject value = ((RemoveFromEReferenceEvent) event).getValue();
		} else if (event instanceof SetEAttributeEvent) {
		    e = document.createElement("set-eattribute");
		    EObject eObject = ((SetEAttributeEvent) event).getTarget();
		    e.setAttribute("eclass", eObject.eClass().getName());
		} else if (event instanceof SetEReferenceEvent) {
		    e = document.createElement("set-ereference");
		    EObject eObject = ((SetEReferenceEvent) event).getTarget();
		    e.setAttribute("eclass", eObject.eClass().getName());
		    EObject value = ((SetEReferenceEvent) event).getValue();
		} else if (event instanceof UnsetEReferenceEvent) {
		    e = document.createElement("unset-ereference");
		    EObject eObject = ((UnsetEReferenceEvent) event).getTarget();
		    e.setAttribute("eclass", eObject.eClass().getName());
		    EObject value = ((UnsetEReferenceEvent) event).getValue();
		} else if (event instanceof UnsetEAttributeEvent) {
		    e = document.createElement("unset-eattribute");
		    EObject eObject = ((UnsetEAttributeEvent) event).getTarget();
		    e.setAttribute("eclass", eObject.eClass().getName());
		} else if (event instanceof AddToEAttributeEvent) {
		    e = document.createElement("add-to-eattribute");
		    EObject eObject = ((AddToEAttributeEvent) event).getTarget();
		    e.setAttribute("eclass", eObject.eClass().getName());
		    Object value = ((AddToEAttributeEvent) event).getValue();
		} else if (event instanceof RemoveFromEAttributeEvent) {
		    e = document.createElement("remove-from-eattribute");
		    EObject eObject = ((RemoveFromEAttributeEvent) event).getTarget();
		    e.setAttribute("eclass", eObject.eClass().getName());
		    Object value = ((RemoveFromEAttributeEvent) event).getValue();
		} else if (event instanceof MoveWithinEReferenceEvent) {
		    e = document.createElement("move-in-ereference");
		    EObject eObject = ((MoveWithinEReferenceEvent) event).getTarget();
		    e.setAttribute("eclass", eObject.eClass().getName());
		    Object values = ((MoveWithinEReferenceEvent) event).getValues();
		} else if (event instanceof MoveWithinEAttributeEvent) {
		    e = document.createElement("move-in-eattribute");
		    EObject eObject = ((MoveWithinEAttributeEvent) event).getTarget();
		    e.setAttribute("eclass", eObject.eClass().getName());
		    Object values = ((MoveWithinEAttributeEvent) event).getValues();
		} else {
		    throw new RuntimeException("Unexpected event:" + event);
		}

		if (event instanceof EStructuralFeatureEvent<?>) {
		    e.setAttribute("name", ((EStructuralFeatureEvent<?>) event).getEStructuralFeature().getName());
		    e.setAttribute("target", getURIFragment(((EStructuralFeatureEvent<?>) event).getTarget()));
		}

		if (event instanceof AddToEReferenceEvent || event instanceof AddToEAttributeEvent || event instanceof AddToResourceEvent) {
		    e.setAttribute("position", event.getPosition() + "");
		}

		if (event instanceof RemoveFromEReferenceEvent || event instanceof RemoveFromEAttributeEvent || event instanceof RemoveFromResourceEvent) {
		    e.setAttribute("position", event.getPosition() + "");
		}

		if (event instanceof FromPositionEvent) {
		    e.setAttribute("from", ((FromPositionEvent) event).getFromPosition() + "");
		    e.setAttribute("to", event.getPosition() + "");
		}

		if (event instanceof EObjectValuesEvent) {

		    for (EObject eObject : ((EObjectValuesEvent) event).getOldValues()) {
			if (eObject != null) {
			    Element o = document.createElement("old-value");
			    o.setAttribute("eobject", getURIFragment(eObject));
			    o.setAttribute("eclass", eObject.eClass().getName());
			    e.appendChild(o);
			}
		    }
		    for (EObject eObject : ((EObjectValuesEvent) event).getValues()) {
			if (eObject != null) {
			    Element o = document.createElement("value");
			    o.setAttribute("eobject", getURIFragment(eObject));
			    o.setAttribute("eclass", eObject.eClass().getName());
			    e.appendChild(o);
			}
		    }
		} else if (event instanceof EAttributeEvent) {
		    for (Object object : ((EAttributeEvent) event).getOldValues()) {
			if (object != null) {
			    Element o = document.createElement("old-value");
			    o.setAttribute("literal", object + "");
			    e.appendChild(o);
			}
		    }
		    for (Object object : ((EAttributeEvent) event).getValues()) {
			if (object != null) {
			    Element o = document.createElement("value");
			    o.setAttribute("literal", object + "");
			    e.appendChild(o);
			}
		    }
		}
		if (event.getComposite() != null && e != null) {
		    e.setAttribute("composite", event.getComposite());
		}

		if (e != null)
		    document.appendChild(e);

		DOMSource source = new DOMSource(document);
		StreamResult result = new StreamResult(out);
		transformer.transform(source, result);
		out.write(System.getProperty("line.separator").getBytes());
		out.flush();

		eventNumber += 1;
	    }
	    documentBuilder.reset();
	    getChangeEvents().clear();
	} catch (

	Exception ex) {
	    ex.printStackTrace();
	    throw new IOException(ex);
	}
    }

    public String getURIFragment(EObject eObject) {
	String uriFragment = resource.getURIFragment(eObject);
	if ("/-1".equals(uriFragment)) {
	    uriFragment = eObject2IdMap.get(eObject);
	}
	return uriFragment;
    }

    public void registerLoadedElements() {
	TreeIterator<EObject> iterator = resource.getAllContents();
	while (iterator.hasNext()) {
	    EObject eObject = iterator.next();
	    if (!isRegistered(eObject)) {
		register(eObject, resource.getURIFragment(eObject));
	    }
	}
    }

    public boolean isRegistered(EObject eObject) {
	if (eObject2IdMap.get(eObject) == null) {
	    return false;
	}
	return true;
    }

    public String register(EObject eObject, String candidateId) {

	EStructuralFeature feature = eObject.eClass().getEStructuralFeature("id");
	if (feature != null) {
	    String tempId = (String) eObject.eGet(feature);
	    if (tempId == null || !tempId.contains("cbp_")) {
		System.out.println(resource.getURIFragment(eObject) + " | " + tempId);
		candidateId = "cbp_" + EcoreUtil.generateUUID();
		try {
		    setActive(false);
		    eObject.eSet(feature, candidateId);
		    setActive(true);
		} catch (Exception e) {
		    setActive(true);
		}
	    }
	}

	eObject2IdMap.put(eObject, candidateId);
	id2EObjectMap.put(candidateId, eObject);
	return candidateId;
    }
}