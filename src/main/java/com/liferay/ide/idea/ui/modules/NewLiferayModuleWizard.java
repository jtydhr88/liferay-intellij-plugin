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

package com.liferay.ide.idea.ui.modules;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.util.newProjectWizard.AbstractProjectWizard;
import com.intellij.ide.util.newProjectWizard.StepSequence;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.util.Disposer;

import com.liferay.ide.idea.ui.modules.ext.LiferayModuleExtBuilder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Terry Jia
 */
public class NewLiferayModuleWizard extends AbstractProjectWizard {

	public NewLiferayModuleWizard(
		@Nullable Project project, @NotNull ModulesProvider modulesProvider, @Nullable String defaultPath) {

		super("New Liferay Modules", project, defaultPath);

		init(modulesProvider);
	}

	@Override
	public StepSequence getSequence() {
		return _sequence;
	}

	@Nullable
	@Override
	protected String getDimensionServiceKey() {
		return "new project wizard";
	}

	@Override
	protected void helpAction() {
		if (getProjectBuilder() instanceof LiferayModuleExtBuilder) {
			BrowserUtil.browse("https://dev.liferay.com/de/develop/reference/-/knowledge_base/7-1/ext");
		}
	}

	protected void init(@NotNull ModulesProvider modulesProvider) {
		myWizardContext.setModulesProvider(modulesProvider);

		LiferayProjectTypeStep projectTypeStep = new LiferayProjectTypeStep(myWizardContext, this, modulesProvider);

		Disposer.register(getDisposable(), projectTypeStep);

		_sequence.addCommonStep(projectTypeStep);

		_sequence.addCommonFinishingStep(new LiferayProjectSettingsStep(myWizardContext), null);

		for (ModuleWizardStep step : _sequence.getAllSteps()) {
			addStep(step);
		}

		super.init();
	}

	private StepSequence _sequence = new StepSequence();

}