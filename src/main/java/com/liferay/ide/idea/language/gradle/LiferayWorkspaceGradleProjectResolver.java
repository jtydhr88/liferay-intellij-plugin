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

package com.liferay.ide.idea.language.gradle;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.idea.IdeaContentRoot;
import org.gradle.tooling.model.idea.IdeaModule;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension;

/**
 * @author Dominik Marks
 */
public class LiferayWorkspaceGradleProjectResolver extends AbstractProjectResolverExtension {

	@NotNull
	@Override
	public Set<Class> getExtraProjectModelClasses() {
		return Collections.<Class>singleton(LiferayWorkspaceGradleTaskModel.class);
	}

	@NotNull
	@Override
	public Set<Class> getToolingExtensionsClasses() {
		return Collections.<Class>singleton(LiferayWorkspaceSupportGradleTaskModelBuilder.class);
	}

	@Override
	public void populateModuleExtraModels(@NotNull IdeaModule gradleModule, @NotNull DataNode<ModuleData> ideModule) {
		LiferayWorkspaceGradleTaskModel liferayWorkspaceGradleTaskModel = resolverCtx.getExtraProject(
			gradleModule, LiferayWorkspaceGradleTaskModel.class);

		if (liferayWorkspaceGradleTaskModel != null) {

			// try to find the corresponding IDEA module for the Gradle Module

			DomainObjectSet<? extends IdeaContentRoot> contentRoots = gradleModule.getContentRoots();

			IdeaContentRoot contentRoot = contentRoots.getAt(0);

			File rootDirectory = contentRoot.getRootDirectory();

			VirtualFile fileByIoFile = VfsUtil.findFileByIoFile(rootDirectory, false);

			if (fileByIoFile != null) {
				Project project = ProjectUtil.guessProjectForFile(fileByIoFile);

				Module module = ModuleUtil.findModuleForFile(fileByIoFile, project);

				String liferayHome = liferayWorkspaceGradleTaskModel.getLiferayHome();

				if (liferayHome != null) {
					LocalFileSystem localFileSystem = LocalFileSystem.getInstance();

					VirtualFile virtualFile = localFileSystem.findFileByPath(liferayHome);

					if (virtualFile != null) {
						String url = virtualFile.getUrl();

						Collection<String> excludeFolders = new ArrayList<>();

						excludeFolders.add(url);

						ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);

						for (VirtualFile sourceRoot : moduleRootManager.getContentRoots()) {
							ModuleRootModificationUtil.updateExcludedFolders(
								module, sourceRoot, Collections.<String>emptyList(), excludeFolders);
						}
					}
				}
			}
		}

		super.populateModuleExtraModels(gradleModule, ideModule);
	}

}