package org.ubot.client;

import org.ubot.bot.Bot;
import org.ubot.bot.script.loader.ScriptLoader;
import org.ubot.classloader.ASMClassLoader;
import org.ubot.classloader.ClassArchive;
import org.ubot.client.account.Account;
import org.ubot.client.account.AccountManager;
import org.ubot.client.provider.ServerProvider;
import org.ubot.client.provider.loader.ServerLoader;
import org.ubot.client.provider.manifest.ServerManifest;
import org.ubot.client.ui.screens.BotTheaterScreen;
import org.ubot.util.directory.DirectoryManager;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class ClientModel {

	private final Client client;
	private final String username, permissionKey;
	private final AccountManager accountManager;
	private final ArrayList<Bot> bots;
	private final BotTheaterScreen botTheaterScreen;
	private final ScriptLoader scriptLoader;

	public ClientModel(Client client, String username, String accountKey, String permissionKey) {
		this.client = client;
		this.username = username;
		this.permissionKey = permissionKey;
		this.accountManager = new AccountManager(username, accountKey);
		this.bots = new ArrayList<>();
		this.botTheaterScreen = new BotTheaterScreen(client);
		this.scriptLoader = new ScriptLoader();
	}

	protected final void destroyBot(Bot bot) {
		bot.destroy();
		bots.remove(bot);
	}

	protected ScriptLoader getScriptLoader() {
		return scriptLoader;
	}

	protected final Bot createBot() {
		final Bot bot = new Bot(client, "Bot #" + (bots.size() + 1));
		bot.initiateConfiguration(getServerProviders(), getAccounts());
		this.bots.add(bot);
		return bot;
	}

	protected BotTheaterScreen getBotTheaterScreen() {
		botTheaterScreen.displayPreviews(bots);
		return botTheaterScreen;
	}

	protected ArrayList<Account> getAccounts() {
		return accountManager.getAccounts();
	}

	protected void saveAccount(Account account) {
		accountManager.addAccount(account);
	}

	protected void deleteAccount(Account account) {
		accountManager.deleteAccount(account);
	}

	protected void accountUpdated() {
		accountManager.loadAccounts();
	}

	protected final ArrayList<Bot> getBots() {
		return bots;
	}

	protected final ArrayList<ServerProvider> getServerProviders() {
		final ArrayList<ServerProvider> providers = new ArrayList<>();
		providers.addAll(loadLocalServerProviders());
		providers.addAll(loadSDNServerProviders());
		return providers;
	}

	private final List<ServerProvider> loadSDNServerProviders() {
		final List<ServerProvider> providers = new ArrayList<>();
		return providers;
	}

	private final List<ServerProvider> loadLocalServerProviders() {
		final List<ServerProvider> providers = new ArrayList<>();
		try {
			for (File file : DirectoryManager.getInstance().getRootDirectory().getSubDirectory(DirectoryManager.SERVER_PROVIDERS).getFiles()) {
				final ClassArchive classArchive = new ClassArchive();
				if (file.getAbsolutePath().endsWith(".jar")) {
					classArchive.addJar(file);
					final ASMClassLoader classLoader = new ASMClassLoader(classArchive);
					try (JarInputStream inputStream = new JarInputStream(new FileInputStream(file))) {
						JarEntry jarEntry;
						while ((jarEntry = inputStream.getNextJarEntry()) != null) {
							if (jarEntry.getName().endsWith(".class") && !jarEntry.getName().contains("$")) {
								String classPackage = jarEntry.getName().replace(".class", "");
								Class<?> clazz = classLoader.loadClass(classPackage.replaceAll("/", "."));
								if (clazz.isAnnotationPresent(ServerManifest.class)) {
									System.out.println("Loading Server");
									final ServerManifest manifest = clazz.getAnnotation(ServerManifest.class);
									final ServerLoader serverLoader = (ServerLoader) clazz.newInstance();
									providers.add(new ServerProvider(manifest, serverLoader, classArchive, classLoader));
									System.out.println("Server Loaded: " + manifest.serverName());
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return providers;
	}

}
