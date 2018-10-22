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

package com.liferay.ide.idea.language.maven;

import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.module.Module;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jdom.Element;

import org.jetbrains.idea.maven.importing.MavenImporter;
import org.jetbrains.idea.maven.importing.MavenRootModelAdapter;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.model.MavenPlugin;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectChanges;
import org.jetbrains.idea.maven.project.MavenProjectsProcessorTask;
import org.jetbrains.idea.maven.project.MavenProjectsTree;
import org.jetbrains.idea.maven.project.SupportedRequestType;

/**
 * @author Dominik Marks
 *
 * A Maven Importer which resolves the Liferay Home directory if the bundle-support plugin is present and will "exclude" the bundles folder from indexing in IntelliJ IDEA
 */
public class LiferayBundleSupportMavenImporter extends MavenImporter {

	public LiferayBundleSupportMavenImporter() {
		super(_LIFERAY_BUNDLE_SUPPORT_MAVEN_GROUP_ID, _LIFERAY_BUNDLE_SUPPORT_MAVEN_ARTIFACT_ID);
	}

	@Override
	public void getSupportedDependencyTypes(Collection<String> result, SupportedRequestType type) {
		getSupportedPackagings(result);
	}

	@Override
	public void getSupportedPackagings(Collection<String> result) {
		result.add("pom");
	}

	@Override
	public boolean isApplicable(MavenProject mavenProject) {
		if (super.isApplicable(mavenProject) && _isRootProject(mavenProject)) {
			return true;
		}

		return false;
	}

	@Override
	public void preProcess(
		Module module, MavenProject mavenProject, MavenProjectChanges mavenProjectChanges,
		IdeModifiableModelsProvider ideModifiableModelsProvider) {
	}

	@Override
	public void process(
		IdeModifiableModelsProvider ideModifiableModelsProvider, Module module,
		MavenRootModelAdapter mavenRootModelAdapter, MavenProjectsTree mavenProjectsTree, MavenProject mavenProject,
		MavenProjectChanges mavenProjectChanges, Map<MavenProject, String> map, List<MavenProjectsProcessorTask> list) {

		MavenPlugin plugin = mavenProject.findPlugin(myPluginGroupID, myPluginArtifactID);

		if (plugin != null) {
			String liferayHome = "bundles";

			Element configurationElement = plugin.getConfigurationElement();

			if (configurationElement != null) {
				Element configBundleSupportLiferayHome = configurationElement.getChild(
					_CONFIG_BUNDLE_SUPPORT_LIFERAY_HOME);

				if (configBundleSupportLiferayHome != null) {
					liferayHome = configBundleSupportLiferayHome.getText();
				}
			}

			if (liferayHome == null) {
				Properties properties = mavenProject.getProperties();

				liferayHome = properties.getProperty(_PROPERTY_BUNDLE_SUPPORT_LIFERAY_HOME);
			}

			if (liferayHome != null) {
				if (!(liferayHome.startsWith("/") || liferayHome.contains(":"))) {
					mavenRootModelAdapter.addExcludedFolder(liferayHome);
				}
			}
		}
	}

	private boolean _isRootProject(MavenProject mavenProject) {
		MavenId parentId = mavenProject.getParentId();

		if ((parentId == null) || (parentId.getGroupId() == null)) {
			return true;
		}

		return false;
	}

	private static final String _CONFIG_BUNDLE_SUPPORT_LIFERAY_HOME = "liferayHome";

	private static final String _LIFERAY_BUNDLE_SUPPORT_MAVEN_ARTIFACT_ID = "com.liferay.portal.tools.bundle.support";

	private static final String _LIFERAY_BUNDLE_SUPPORT_MAVEN_GROUP_ID = "com.liferay";

	private static final String _PROPERTY_BUNDLE_SUPPORT_LIFERAY_HOME = "liferayHome";

}