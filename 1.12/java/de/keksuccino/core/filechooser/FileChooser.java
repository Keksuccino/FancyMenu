package de.keksuccino.core.filechooser;

import java.io.File;
import java.util.function.Consumer;

import javax.swing.filechooser.FileFilter;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;

public class FileChooser {
	
	private static volatile Consumer<File> callback = null;
	private static volatile File selectedFile = null;
	
	private static boolean init = false;
	
	public static void init() {
		if (!init) {
			MinecraftForge.EVENT_BUS.register(new FileChooser());
			init = true;
		}
	}
	
	/**
	 * Returns (callback) the choosen file or null if no file was choosen.
	 * @param homeDir The homedir to start from.
	 * @param callback The callback.
	 */
	public static void askForFile(File homeDir, Consumer<File> callback, String... fileTypes) {
		if (!init) {
			return;
		}
		
		System.setProperty("java.awt.headless", "false");
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (!homeDir.exists() || !homeDir.isDirectory()) {
					FileChooser.callback = callback;
					return;
				}
				
				SimpleFileChooser chooser = new SimpleFileChooser(homeDir);
				chooser.setMultiSelectionEnabled(false);
				
				if ((fileTypes != null) && (fileTypes.length > 0)) {
					chooser.setFileFilter(new FileFilter() {
						@Override
						public String getDescription() {
							return null;
						}
						@Override
						public boolean accept(File f) {
							if (f.isDirectory()) {
								return true;
							}
							for (String s : fileTypes) {
								if (f.getName().endsWith("." + s)) {
									return true;
								}
							}
							return false;
						}
					});
				}
				
				int result = chooser.showOpenDialog(null);
				if (result == SimpleFileChooser.APPROVE_OPTION) {
					selectedFile = chooser.getSelectedFile();
				}
				
				FileChooser.callback = callback;
			}
		}).start();
	}
	
	//"Why you don't handle the callback directly in the chooser thread?":
	//Because some texture and sound stuff needs to be done in the main thread.
	@SubscribeEvent
	public void onTick(ClientTickEvent e) {
		if (callback != null) {
			callback.accept(selectedFile);
			callback = null;
		}
		selectedFile = null;
	}

}
