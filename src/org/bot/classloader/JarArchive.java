package org.bot.classloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;

import org.bot.util.directory.DirectoryManager;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;

public class JarArchive implements Archive<ClassNode> {
	public static final Map<String, File> resources = new HashMap<String, File>();
	public Map<String, ClassNode> classes;
	private JarFile jar;
	private Map<String, JarEntry> otherFiles = new HashMap<String, JarEntry>();

	public JarArchive(JarFile jar) throws IOException {
		this.jar = jar;
		this.classes = load();
		Logger.getLogger(this.getClass().getName())
				.info("Loaded " + classes.size() + " class files and " + otherFiles.size() + " other files.");
	}

	@Override
	public ClassNode get(String name) {
		return classes.get(name);
	}

	@Override
	public byte[] getEntry(String name) throws ClassNotFoundException {
		ClassNode node = classes.get(name);
		if (node == null) {
			throw new ClassNotFoundException("Class " + name + " is not valid in the archive.");
		}
		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		node.accept(writer);
		return writer.toByteArray();
	}

	@Override
	public Map<String, ClassNode> load() throws IOException {
		Map<String, ClassNode> classes = new HashMap<String, ClassNode>();
		Enumeration<JarEntry> entries = jar.entries();
		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			String name = entry.getName();
			if (name.endsWith(".class")) {
				ClassNode node = new ClassNode();
				ClassReader reader = new ClassReader(jar.getInputStream(entry));
				reader.accept(node, ClassReader.EXPAND_FRAMES);

				classes.put(node.name.replace(".class", ""), node);
			} else {
				if (name.contains("MANIFEST")) {
					continue;
				}

				otherFiles.put(name, entry);
				loadResource(name, jar.getInputStream(entry));
			}
		}
		return classes;
	}

	public void loadResource(final String name, final InputStream in) throws IOException {
		String path;
		path = DirectoryManager.SERVER_JARS_PATH + File.separator;
		File f1 = new File(path);

		final File f = File.createTempFile("bot", ".tmp", f1);
		f.deleteOnExit();
		try (OutputStream out = new FileOutputStream(f)) {
			byte[] buffer = new byte[1024];
			int len;
			while ((len = in.read(buffer)) != -1) {
				out.write(buffer, 0, len);
			}
		} catch (IOException e) {
		}
		resources.put(name, f);
	}

	@Override
	public void write(File file) throws IOException {
		JarOutputStream jos = new JarOutputStream(new FileOutputStream(file));
		try {
			for (Iterator<ClassNode> itr = iterator(); itr.hasNext();) {
				ClassNode node = itr.next();
				ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
				node.accept(new CheckClassAdapter(writer, true));
				byte[] bytes = writer.toByteArray();
				jos.putNextEntry(new JarEntry(node.name.concat(".class")));
				jos.write(bytes);
			}
		} finally {
			jos.close();
		}
	}

	@Override
	public Iterator<ClassNode> iterator() {
		return classes.values().iterator();
	}

}