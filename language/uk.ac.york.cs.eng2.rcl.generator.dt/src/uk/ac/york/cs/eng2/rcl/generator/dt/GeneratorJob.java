package uk.ac.york.cs.eng2.rcl.generator.dt;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.util.URI;

import uk.ac.york.cs.eng2.rcl.generator.CodeGenerator;

public class GeneratorJob extends Job {

	private final IFile model;
	private final File targetFolder;

	public GeneratorJob(IFile model, File targetFolder) {
		super("Generate ENG2 Code");

		this.model = model;
		this.targetFolder = targetFolder;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		CodeGenerator gen = new CodeGenerator();
		gen.setInputModelURI(URI.createPlatformResourceURI(model.getFullPath().toOSString(), true));
		gen.setTargetFolder(targetFolder);

		try {
			gen.run();

			java.net.URI targetFolderUri = targetFolder.getCanonicalFile().toURI();
			IContainer[] workspaceFiles = ResourcesPlugin.getWorkspace().getRoot().findContainersForLocationURI(targetFolderUri);
			for (IContainer iFile : workspaceFiles) {
				iFile.refreshLocal(IResource.DEPTH_INFINITE, null);
			}

			return Status.OK_STATUS;
		} catch (Exception ex) {
			return Status.error(ex.getMessage(), ex);
		}
	}

}
