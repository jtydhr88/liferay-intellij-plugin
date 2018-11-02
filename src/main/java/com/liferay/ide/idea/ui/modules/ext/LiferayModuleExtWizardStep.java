/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.ide.idea.ui.modules.ext;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.UIUtil;

import com.liferay.ide.idea.core.Artifact;
import com.liferay.ide.idea.ui.compoments.FixedSizeRefreshButton;
import com.liferay.ide.idea.util.LiferayWorkspaceUtil;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;

import java.util.Collections;

import javax.swing.ComboBoxEditor;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicComboBoxEditor;

import org.jetbrains.annotations.NotNull;

/**
 * @author Charle Wu
 */
public class LiferayModuleExtWizardStep extends ModuleWizardStep {

	public LiferayModuleExtWizardStep(WizardContext wizardContext, LiferayModuleExtBuilder builder) {
		Project project = wizardContext.getProject();
		_builder = builder;
		_moduleNameHint.setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL));

		// customize the presentation of a artifact

		_originalModuleName.setRenderer(
			new ColoredListCellRenderer<Artifact>() {

				@Override
				protected void customizeCellRenderer(
					@NotNull JList<? extends Artifact> list, Artifact value, int index, boolean selected,
					boolean hasFocus) {

					append(value.getArtifact());
					append("  " + value.getVersion(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
				}

			});

		// only set the artifact name when select the value from list.

		_originalModuleName.setEditor(
			new BasicComboBoxEditor() {

				@Override
				public void setItem(Object item) {
					if (item instanceof Artifact) {
						String text = ((Artifact)item).getArtifact();

						if (!text.equals(editor.getText())) {
							editor.setText(text);
						}
					}
				}

			});

		// fill out the module version field automatic

		_originalModuleName.addItemListener(
			event -> {
				if (event.getStateChange() == ItemEvent.SELECTED) {
					Object item = event.getItem();

					if (item instanceof Artifact) {
						_originalModuleVersion.setText(((Artifact)item).getVersion());
					}
				}
			});

		if (LiferayWorkspaceUtil.getTargetPlatformVersion(project) != null) {
			_insertOriginalModuleNames(false);

			_originalModuleName.setMaximumRowCount(12);
			_originalModuleVersion.setEnabled(false);
		}

		_refreshButton.addActionListener(
			new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					_refreshButton.setEnabled(false);

					if (!LiferayWorkspaceUtil.targetPlatformResolved(project)) {
						String message = "Please refresh or wait Gradle to resolve workspace project.";

						if (LiferayWorkspaceUtil.getTargetPlatformVersion(project) == null) {
							message = "No Target Platform configuration detected in gradle.properties.";
						}

						Messages.showMessageDialog(project, message, "Warning", Messages.getWarningIcon());
						_refreshButton.setEnabled(true);

						return;
					}

					Task.Backgroundable task =
						new Task.Backgroundable(project, "Calculating Target Platform Artifacts", false) {

							@Override
							public void run(@NotNull ProgressIndicator indicator) {
								LiferayWorkspaceUtil.getTargetPlatformArtifacts(project, true);
							}

						};

					BackgroundableProcessIndicator processIndicator = new BackgroundableProcessIndicator(task) {

						@Override
						public synchronized void stop() {
							super.stop();

							_insertOriginalModuleNames(true);
							_refreshButton.setEnabled(true);
						}

					};

					ProgressManager progressManager = ProgressManager.getInstance();

					progressManager.runProcessWithProgressAsynchronously(task, processIndicator);
				}

			});
	}

	@Override
	public JComponent getComponent() {
		return _mainPanel;
	}

	@Override
	public void updateDataModel() {
		ComboBoxEditor editor = _originalModuleName.getEditor();

		Object item = editor.getItem();

		_builder.setOriginalModuleName(item.toString());

		_builder.setOriginalModuleVersion(_originalModuleVersion.getText());
	}

	@Override
	public boolean validate() {
		return true;
	}

	private void _insertOriginalModuleNames(boolean clear) {
		Collections.sort(LiferayWorkspaceUtil.targetPlatformArtifacts);

		if (clear) {
			_originalModuleName.removeAllItems();
		}

		LiferayWorkspaceUtil.targetPlatformArtifacts.forEach(
			artifact -> {
				if ("com.liferay".equals(artifact.getGroup())) {
					_originalModuleName.addItem(artifact);
				}
			});
	}

	private LiferayModuleExtBuilder _builder;
	private JPanel _mainPanel;
	private JLabel _moduleNameHint;
	private JComboBox<Artifact> _originalModuleName;
	private JTextField _originalModuleVersion;
	private FixedSizeRefreshButton _refreshButton;

}