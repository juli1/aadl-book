package org.gunnm.aadl.commentreporting.handlers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.xtext.ui.editor.outline.IOutlineNode;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;
import org.osate.aadl2.BasicPropertyAssociation;
import org.osate.aadl2.ComponentImplementation;
import org.osate.aadl2.EnumerationLiteral;
import org.osate.aadl2.NamedElement;
import org.osate.aadl2.NamedValue;
import org.osate.aadl2.Property;
import org.osate.aadl2.PropertyExpression;
import org.osate.aadl2.RecordValue;
import org.osate.aadl2.StringLiteral;
import org.osate.aadl2.instance.ComponentInstance;
import org.osate.aadl2.instance.SystemInstance;
import org.osate.aadl2.instantiation.InstantiateModel;
import org.osate.aadl2.modelsupport.resources.OsateResourceUtil;
import org.osate.aadl2.modelsupport.util.AadlUtil;
import org.osate.xtext.aadl2.properties.util.GetProperties;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
class Comment {
	String author;
	String content;
	String importance;

	public Comment (String a, String c, String i)
	{
		author = a;
		content = c;
		importance = i;
	}
}

public class SampleHandler extends AbstractHandler {

	public SampleHandler() {
	}

	protected IPath getReportPath(EObject root) {

		String filename = null;

		Resource res = root.eResource();
		URI uri = res.getURI();
		IPath path = OsateResourceUtil.getOsatePath(uri);

		path = path.removeFileExtension();
		filename = path.lastSegment() + "_comments_";
		path = path.removeLastSegments(1).append("/reports/comments/" + filename);
		path = path.addFileExtension("csv");
		return path;
	}

	public void writeReport(EObject root, StringBuffer content) {

		IPath path = getReportPath(root);
		if (path != null) {
			IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
			if (file != null) {
				final InputStream input = new ByteArrayInputStream(content.toString().getBytes());
				try {
					if (file.exists()) {
						file.setContents(input, true, true, null);
					} else {
						AadlUtil.makeSureFoldersExist(path);
						file.create(input, true, null);
					}
				} catch (final CoreException e) {
				}
			}
		}
	}





	public static List<Comment> getComments (final NamedElement ph) {
		Property property;
		List<Comment> result;
		BasicPropertyAssociation pa;

		result = new ArrayList<Comment> ();
		property = GetProperties.lookupPropertyDefinition(ph, "myproperties", "reviews");
		List<? extends PropertyExpression> propertyValues = ph.getPropertyValueList(property);
		for (PropertyExpression pe : propertyValues)
		{
			RecordValue rv = (RecordValue) pe;
			EList<BasicPropertyAssociation> fields = rv.getOwnedFieldValues();
			pa = GetProperties.getRecordField(fields, "author");
			String author = ((StringLiteral)pa.getValue()).getValue();

			pa = GetProperties.getRecordField(fields, "content");
			String content = ((StringLiteral)pa.getValue()).getValue();

			pa = GetProperties.getRecordField(fields, "importance");
			EnumerationLiteral elImportance = (EnumerationLiteral) ((NamedValue)pa.getValue()).getNamedValue();
			String importance = elImportance.getName();
			result.add(new Comment (author, content, importance));
		}
		return result;
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		IStructuredSelection selection = (IStructuredSelection) HandlerUtil.getCurrentSelection(event);
		IOutlineNode node = (IOutlineNode) selection.getFirstElement();

		node.readOnly (new IUnitOfWork<Object, EObject>() {

			@Override
			public Object exec(EObject state) throws Exception {
				SystemInstance rootInstance = null;
				EObject selectedObject = state;
				StringBuffer output = new StringBuffer();
				if (selectedObject instanceof SystemInstance)
				{
					rootInstance = (SystemInstance) selectedObject;
				}

				if (selectedObject instanceof ComponentImplementation) {
					try {
						rootInstance =
								InstantiateModel.buildInstanceModelFile((ComponentImplementation)selectedObject);

					}
					catch (Exception e) {
						e.printStackTrace();
						return null;
					}
				}
				output.append ("Author,Content,Importance\n");
				for (ComponentInstance ci : rootInstance.getAllComponentInstances())
				{

					List<Comment> comments = getComments(ci);
					for (Comment comment : comments)
					{
						output.append (comment.author);
						output.append (",");
						output.append (comment.content);
						output.append (",");
						output.append (comment.importance);
						output.append ("\n");
					}
				}
				writeReport(rootInstance, output);
				return null;
			}
		}

		);

		return null;

	}
}