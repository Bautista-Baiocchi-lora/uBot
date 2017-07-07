package org.bot.ui.screens.serverselector;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.bot.Engine;
import org.bot.provider.loader.ServerLoader;
import org.bot.provider.manifest.NullManifestException;
import org.bot.provider.manifest.ServerManifest;
import org.bot.ui.InterfaceManager;
import org.bot.ui.screens.Screen;
import org.bot.util.directory.exceptions.InvalidDirectoryNameException;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;

public class ServerSelectorScreen extends Screen {

	private static HBox layout;
	private ServerInformationTab serverTab;

	public ServerSelectorScreen(InterfaceManager manager) {
		super(layout = new HBox(), manager);
		configure();
	}

	private void configure() {
		ListView<ServerLabel> list = new ListView<ServerLabel>();
		list.setEditable(false);
		ArrayList<ServerLabel> providers = getServerProviderComponents();
		ObservableList<ServerLabel> items = FXCollections.observableArrayList(providers);
		list.setItems(items);
		list.setOnMouseClicked((e) -> {
			displayTab(list.getSelectionModel().getSelectedItem().getTab());
		});
		list.setMaxWidth(250);
		layout.getChildren().add(list);
	}

	private ArrayList<ServerLabel> getServerProviderComponents() {
		final ArrayList<ServerLabel> providers = new ArrayList<ServerLabel>();
		JarFile jar;
		try {
			for (File file : Engine.getInstance().getDirectoryManager().getRootDirectory()
					.getSubDirectory("Server Providers").getFiles()) {
				jar = new JarFile(file.getAbsolutePath());
				URL[] urls = { new URL("jar:file:" + file.getAbsolutePath() + "!/") };
				ClassLoader cl = URLClassLoader.newInstance(urls);
				Enumeration<JarEntry> entries = jar.entries();
				while (entries.hasMoreElements()) {
					JarEntry e = entries.nextElement();
					if (e.getName().endsWith(".class") && !e.getName().contains("$")) {
						Class<?> clazz;
						String classPackage = e.getName().replace(".class", "");
						clazz = cl.loadClass(classPackage.replaceAll("/", "."));
						ServerLoader<?> loader = null;
						if (clazz.isAnnotationPresent(ServerManifest.class)) {
							final ServerManifest manifest = clazz.getAnnotation(ServerManifest.class);
							if (manifest == null) {
								throw new NullManifestException();
							}
							loader = (ServerLoader<?>) clazz.newInstance();
							providers.add(new ServerLabel(loader, manifest, manager));
						}
					}
				}
			}
		} catch (IOException | InstantiationException | IllegalAccessException | ClassNotFoundException
				| InvalidDirectoryNameException e) {
			e.printStackTrace();
		} catch (NullManifestException e1) {
			e1.printStackTrace();
			System.exit(0);
		}
		return providers;
	}

	private void displayTab(ServerInformationTab tab) {
		if (serverTab == null) {
			serverTab = tab;
			layout.getChildren().add(serverTab);
		} else {
			layout.getChildren().remove(serverTab);
			serverTab = tab;
			layout.getChildren().add(serverTab);
		}
	}

}