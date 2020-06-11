package de.keksuccino.core.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import de.keksuccino.core.math.MathUtils;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class KeyboardHandler {
	
	private static boolean keyPressed = false;
	private static int keycode = 0;
	private static int scancode = 0;
	private static char typedChar = " ".charAt(0);
	private static int charModifiers = 0;
	private static int modifiers = 0;
	
	private static List<Integer> ids = new ArrayList<Integer>();
	
	//Using seperated lists to add new listeners to prevent Comodifications
	private static Map<Integer, Consumer<KeyboardData>> pressedRaw = new HashMap<Integer, Consumer<KeyboardData>>();
	private static Map<Integer, Consumer<KeyboardData>> releasedRaw = new HashMap<Integer, Consumer<KeyboardData>>();
	private static Map<Integer, Consumer<CharData>> charRaw = new HashMap<Integer, Consumer<CharData>>();
	
	private static Map<Integer, Consumer<KeyboardData>> keyPressedListeners = new HashMap<Integer, Consumer<KeyboardData>>();
	private static Map<Integer, Consumer<KeyboardData>> keyReleasedListeners = new HashMap<Integer, Consumer<KeyboardData>>();
	private static Map<Integer, Consumer<CharData>> charListeners = new HashMap<Integer, Consumer<CharData>>();
	
	private static boolean init = false;
	
	public static void init() {
		if (!init) {
			MinecraftForge.EVENT_BUS.register(new KeyboardHandler());
			init = true;
		}
	}
	
	@SubscribeEvent
	public void onKeyPressPost(GuiScreenEvent.KeyboardKeyPressedEvent.Post e) {
		keycode = e.getKeyCode();
		scancode = e.getScanCode();
		modifiers = e.getModifiers();
		keyPressed = true;
		
		keyPressedListeners.clear();
		keyPressedListeners.putAll(pressedRaw);
		
		for (Consumer<KeyboardData> c : keyPressedListeners.values()) {
			c.accept(new KeyboardData(e.getKeyCode(), e.getScanCode(), e.getModifiers()));
		}
	}
	
	@SubscribeEvent
	public void onKeyReleasedPost(GuiScreenEvent.KeyboardKeyReleasedEvent.Post e) {
		keyPressed = false;
		
		keyReleasedListeners.clear();
		keyReleasedListeners.putAll(releasedRaw);
		
		for (Consumer<KeyboardData> c : keyReleasedListeners.values()) {
			c.accept(new KeyboardData(e.getKeyCode(), e.getScanCode(), e.getModifiers()));
		}
	}
	
	@SubscribeEvent
	public void onCharTyped(GuiScreenEvent.KeyboardCharTypedEvent.Post e) {
		typedChar = e.getCodePoint();
		charModifiers = e.getModifiers();
		
		charListeners.clear();
		charListeners.putAll(charRaw);
		
		for (Consumer<CharData> c : charListeners.values()) {
			c.accept(new CharData(e.getCodePoint(), e.getModifiers()));
		}
	}
	
	public static boolean isKeyPressed() {
		return keyPressed;
	}
	
	public static int getCurrentKeyCode() {
		return keycode;
	}
	
	public static int getCurrentKeyScanCode() {
		return scancode;
	}
	
	public static int getCurrentKeyModifiers() {
		return modifiers;
	}
	
	public static char getCurrentChar() {
		return typedChar;
	}
	
	public static int getCurrentCharModifiers() {
		return charModifiers;
	}
	
	/**
	 * Adds a new listener and returns its unique ID.
	 */
	public static int addKeyPressedListener(Consumer<KeyboardData> c) {
		int id = generateUniqueId();
		pressedRaw.put(id, c);
		return id;
	}
	
	/**
	 * Adds a new listener and returns its unique ID.
	 */
	public static int addKeyReleasedListener(Consumer<KeyboardData> c) {
		int id = generateUniqueId();
		releasedRaw.put(id, c);
		return id;
	}
	
	/**
	 * Adds a new listener and returns its unique ID.
	 */
	public static int addCharTypedListener(Consumer<CharData> c) {
		int id = generateUniqueId();
		charRaw.put(id, c);
		return id;
	}
	
	public static void removeKeyPressedListener(int id) {
		if (pressedRaw.containsKey(id)) {
			pressedRaw.remove(id);
		}
	}
	
	public static void removeKeyReleasedListener(int id) {
		if (releasedRaw.containsKey(id)) {
			releasedRaw.remove(id);
		}
	}
	
	public static void removeCharTypedListener(int id) {
		if (charRaw.containsKey(id)) {
			charRaw.remove(id);
		}
	}
	
	//TODO switch to UUID
	private static int generateUniqueId() {
		int i = MathUtils.getRandomNumberInRange(100000000, 999999999);
		while(ids.contains(i)) {
			i = MathUtils.getRandomNumberInRange(100000000, 999999999);
		}
		ids.add(i);
		return i;
	}

}
