package com.artifice.refactoring.UI;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

import com.artifice.refactoring.data.DataConverter;
import com.artifice.refactoring.engine.Refactor;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.ModifyEvent;

public class Main extends Dialog {
	private Text methText;
	private Text fieldText;
	private Text varText;
	private Group grpRenaming;
	private Group grpControlObfuscations;
	
	/**
	 * Create the dialog.
	 * @param parentShell
	 */
	public Main(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(final Composite parent) {
		
		Composite container = (Composite) super.createDialogArea(parent);				// Container
		container.setLayout(new FillLayout(SWT.HORIZONTAL));
		
		grpRenaming = new Group(container, SWT.NONE);
		grpRenaming.setText("Data Obfuscations");
		GridLayout gl_grpRenaming = new GridLayout(2, false);
		gl_grpRenaming.verticalSpacing = 10;
		grpRenaming.setLayout(gl_grpRenaming);
		
		final Button methCheck = new Button(grpRenaming, SWT.CHECK);					// Method Check
		methCheck.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(methCheck.getSelection() == false) {
					methText.setEnabled(false);
					Refactor.refactorings[0] = false;
				}
				else {
					methText.setEnabled(true);
					Refactor.refactorings[0] = true;
				}
			}
		});
		methCheck.setText("rename methods");
		
		methText = new Text(grpRenaming, SWT.BORDER);									// Method Text
		methText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				if((methText.getText() != "") && (methText.getText().matches("[a-z].*")))
					DataConverter.newNames[0] = methText.getText();
				else {
					methText.setText("m");
					MessageDialog.openWarning(null, "Warning", "Method identifiers have to start with a non-capital letter!");
				}
			}
		});
		methText.setEnabled(false);
		methText.setText("m");
		methText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		final Button fieldCheck = new Button(grpRenaming, SWT.CHECK);					// Field Check
		fieldCheck.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(fieldCheck.getSelection() == false) {
					fieldText.setEnabled(false);
					Refactor.refactorings[1] = false;
				}
				else {
					fieldText.setEnabled(true);
					Refactor.refactorings[1] = true;
				}
			}
		});
		fieldCheck.setText("rename fields");
		
		fieldText = new Text(grpRenaming, SWT.BORDER);									// Field Text
		fieldText.setEnabled(false);
		fieldText.setText("f");
		fieldText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		final Button varCheck = new Button(grpRenaming, SWT.CHECK);						// Variable Check
		varCheck.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(varCheck.getSelection() == false) {
					varText.setEnabled(false);
					Refactor.refactorings[2] = false;
				}
				else {
					varText.setEnabled(true);
					Refactor.refactorings[2] = true;
				}
			}
		});
		varCheck.setText("rename variable");
		
		varText = new Text(grpRenaming, SWT.BORDER);									// Variable Text
		varText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent arg0) {
				if((varText.getText() != "") && (varText.getText().matches("[a-z].*")))
					DataConverter.newNames[2] = varText.getText();
				else {
					varText.setText("v");
					MessageDialog.openWarning(null, "Warning", "Variable identifieres have to start with a non-capital letter!");
				}
				if(fieldText.getText().equals(varText.getText()) && (fieldText.isEnabled()) && (varText.isEnabled())) {
					varText.setText("v");
					MessageDialog.openWarning(null, "Warning", "Field and variable identifiers have to be different!");
				}
			}
		});
		varText.setEnabled(false);
		varText.setText("v");
		varText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		fieldText.addModifyListener(new ModifyListener() {										//FieldText modifier
			public void modifyText(ModifyEvent arg0) {
				if((fieldText.getText() != "") && (fieldText.getText().matches("[a-z].*")))
					DataConverter.newNames[1] = fieldText.getText();
				else {
					fieldText.setText("f");
					MessageDialog.openWarning(null, "Warning", "Field identifiers have to start with a non-capital letter!");
				}		
				if(fieldText.getText().equals(varText.getText()) && (fieldText.isEnabled()) && (varText.isEnabled())) {
					fieldText.setText("f");
					MessageDialog.openWarning(null, "Warning", "Field and variable identifiers have to be different!");
				}
			}
		});
		
		grpControlObfuscations = new Group(container, SWT.NONE);					// Group Container (Control)
		grpControlObfuscations.setText("Control Obfuscations");
		grpControlObfuscations.setLayout(new FillLayout(SWT.VERTICAL));
		
		final Button expCheck = new Button(grpControlObfuscations, SWT.CHECK);
		expCheck.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(expCheck.getSelection() == true)
					Refactor.refactorings[3] = true;
				else
					Refactor.refactorings[3] = false;
			}
		});
		expCheck.setText("Expansion");
		
		final Button contrCheck = new Button(grpControlObfuscations, SWT.CHECK);
		contrCheck.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(contrCheck.getSelection() == true)
					Refactor.refactorings[4] = true;
				else
					Refactor.refactorings[4] = false;
			}
		});
		contrCheck.setText("Contraction");
		
		final Button loopCheck = new Button(grpControlObfuscations, SWT.CHECK);
		loopCheck.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(loopCheck.getSelection() == true)
					Refactor.refactorings[5] = true;
				else
					Refactor.refactorings[5] = false;
			}
		});
		loopCheck.setText("Loop Transformation");
		
		final Button condCheck = new Button(grpControlObfuscations, SWT.CHECK);
		condCheck.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(condCheck.getSelection() == true)
					Refactor.refactorings[6] = true;
				else
					Refactor.refactorings[6] = false;
			}
		});
		condCheck.setText("Conditional Transformation");

		return container;
	}

	/**
	 * Create contents of the button bar.
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(450, 190);
	}
	
	protected void okPressed() {
		this.getButton(OK).setEnabled(false);
		DataConverter.getData();
		
		if(Refactor.refactorings[2])
			Refactor.renameVariables();
		if(Refactor.refactorings[1])
			Refactor.renameFields();
		if(Refactor.refactorings[0])
			Refactor.renameMethods();
		if((Refactor.refactorings[3]) || (Refactor.refactorings[4]) || (Refactor.refactorings[5]) || (Refactor.refactorings[6]))
			Refactor.refactorCustoms();
		
	    super.okPressed();
	  }
}
