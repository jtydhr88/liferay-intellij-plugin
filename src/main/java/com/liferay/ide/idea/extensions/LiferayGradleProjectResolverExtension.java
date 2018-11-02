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

package com.liferay.ide.idea.extensions;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ProjectData;

import com.liferay.ide.idea.core.Artifact;
import com.liferay.ide.idea.util.LiferayWorkspaceUtil;

import java.util.ArrayList;
import java.util.List;

import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.GradleModuleVersion;
import org.gradle.tooling.model.idea.IdeaDependency;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.tooling.model.idea.IdeaSingleEntryLibraryDependency;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.model.GradleConfiguration;
import org.jetbrains.plugins.gradle.model.GradleExtensions;
import org.jetbrains.plugins.gradle.model.ProjectImportAction;
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension;

/**
 * @author Charle Wu
 */
public class LiferayGradleProjectResolverExtension extends AbstractProjectResolverExtension {

	@Override
	public void populateProjectExtraModels(
		@NotNull IdeaProject gradleProject, @NotNull DataNode<ProjectData> ideProject) {

		ProjectImportAction.AllModels models = resolverCtx.getModels();

		GradleExtensions extensions = models.getExtraProject(GradleExtensions.class);

		boolean hasTargetPlatform = false;

		if (extensions != null) {
			List<GradleConfiguration> configurations = extensions.getConfigurations();

			for (GradleConfiguration config : configurations) {
				if ("targetPlatformBoms".equals(config.getName())) {
					hasTargetPlatform = true;

					break;
				}
			}
		}

		if (!hasTargetPlatform) {
			return;
		}

		try {
			String rootProjectName = gradleProject.getName();

			for (IdeaModule module : gradleProject.getModules()) {
				//workspace root module

				if (rootProjectName.equals(module.getName())) {
					_collectArtifacts(module);

					break;
				}
			}
		}
		finally {
			//do not break the gradle resolver chain
			nextResolver.populateProjectExtraModels(gradleProject, ideProject);
		}
	}

	private void _collectArtifacts(IdeaModule module) {
		DomainObjectSet<? extends IdeaDependency> dependencies = module.getDependencies();

		LiferayWorkspaceUtil.targetPlatformArtifacts = new ArrayList<>(dependencies.size());

		for (IdeaDependency commonDependency : dependencies) {
			if (commonDependency instanceof IdeaSingleEntryLibraryDependency) {
				IdeaSingleEntryLibraryDependency dependency = (IdeaSingleEntryLibraryDependency)commonDependency;

				GradleModuleVersion libVersion = dependency.getGradleModuleVersion();

				if (libVersion != null) {
					LiferayWorkspaceUtil.targetPlatformArtifacts.add(
						new Artifact(
							libVersion.getGroup(), libVersion.getName(), libVersion.getVersion(),
							dependency.getSource()));
				}
			}
		}
	}

}