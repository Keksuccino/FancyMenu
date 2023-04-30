
//Acara - Simple Java Event System

//Copyright (c) 2020-2023 Keksuccino.
//Acara is licensed under MIT.

package de.keksuccino.fancymenu.event.acara;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;

/**
 * This class handles all events and event listeners of your project.<br>
 * You should only create ONE instance per project.
 **/
@SuppressWarnings("unchecked")
public class EventHandler {

	private static final Logger LOGGER = LogManager.getLogger();

	private final Map<String, List<ListenerContainer>> events = new HashMap<>();

	/** FancyMenu's default EventHandler instance. **/
	public static final EventHandler INSTANCE = new EventHandler();

	/**
	 * Call this when all registered event listeners for the given event type should get invoked/notified.<br><br>
	 *
	 * <b>Usage:</b>
	 *
	 * <pre>
	 * {@code ExampleEvent e = new ExampleEvent();}
	 * {@code EventHandler.post(e);}
	 * </pre>
	 */
	public void postEvent(EventBase event) {
		if (eventsRegisteredForType(event.getClass())) {
			List<ListenerContainer> l = new ArrayList<>(events.get(event.getClass().getName()));
			l.sort((o1, o2) -> {
				if (o1.priority < o2.priority) {
					return -1;
				} else if (o1.priority > o2.priority) {
					return 1;
				}
				return 0;
			});
			for (ListenerContainer c : l) {
				c.noticeListener(event);
			}
		}
	}

	/**
	 * This will register all public static event listener methods of the given class annotated with {@link SubscribeEvent}.<br>
	 * Event listener methods need to have only ONE parameter and this parameter has to be the event type (subclass of {@link EventBase}).<br><br>
	 *
	 * <b>Example of a valid event listener method:</b>
	 *
	 * <pre>
	 * {@code @SubscribeEvent(priority = EventPriority.NORMAL)}
	 * {@code public static void onEvent(EventBase event)} {
	 *    //do something with the event object
	 * }
	 * </pre>
	 **/
	public void registerListenersOf(Class<?> clazz) {
		this.registerListenerMethods(this.getEventMethodsOf(clazz));
	}

	/**
	 * This will register all public non-static event listener methods of the given object annotated with {@link SubscribeEvent}.<br>
	 * Event listener methods need to have only ONE parameter and this parameter has to be the event type (subclass of {@link EventBase}).<br><br>
	 *
	 * <b>Example of a valid event listener method:</b>
	 *
	 * <pre>
	 * {@code @SubscribeEvent(priority = EventPriority.NORMAL)}
	 * {@code public void onEvent(EventBase event)} {
	 *    //do something with the event object
	 * }
	 * </pre>
	 **/
	public void registerListenersOf(Object object) {
		this.registerListenerMethods(this.getEventMethodsOf(object));
	}

	protected void registerListenerMethods(List<EventMethod> methods) {
		for (EventMethod m : methods) {
			Consumer<EventBase> listener = (event) -> {
				try {
					m.method.invoke(m.parentObject, event);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			};
			ListenerContainer container = new ListenerContainer(m.eventType.getName(), listener, m.priority);
			container.listenerParentClassName = m.parentClass.getName();
			container.listenerMethodName = m.method.getName();
			this.registerListener(container);
		}
	}

	protected List<EventMethod> getEventMethodsOf(Object objectOrClass) {
		List<EventMethod> l = new ArrayList<>();
		try {
			if (objectOrClass != null) {
				boolean isClass = (objectOrClass instanceof Class<?>);
				Class<?> c = isClass ? (Class<?>) objectOrClass : objectOrClass.getClass();
				for (Method m : c.getMethods()) {
					if (isClass && Modifier.isStatic(m.getModifiers())) {
						EventMethod em = EventMethod.tryCreateFrom(new AnalyzedMethod(m, c));
						if (em != null) l.add(em);
					}
					if (!isClass && !Modifier.isStatic(m.getModifiers())) {
						EventMethod em = EventMethod.tryCreateFrom(new AnalyzedMethod(m, objectOrClass));
						if (em != null) l.add(em);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return l;
	}

	public void registerListener(Consumer<EventBase> listener, Class<? extends EventBase> eventClass) {
		this.registerListener(listener, eventClass, 0);
	}

	public void registerListener(Consumer<EventBase> listener, Class<? extends EventBase> eventClass, int priority) {
		this.registerListener(new ListenerContainer(eventClass.getName(), listener, priority));
	}

	protected void registerListener(ListenerContainer listenerContainer) {
		try {
			if (!eventsRegisteredForIdentifier(listenerContainer.eventIdentifier)) {
				events.put(listenerContainer.eventIdentifier, new ArrayList<>());
			}
			events.get(listenerContainer.eventIdentifier).add(listenerContainer);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean eventsRegisteredForType(Class<? extends EventBase> listenerType) {
		if (listenerType == null) {
			return false;
		}
		return this.eventsRegisteredForIdentifier(listenerType.getName());
	}

	public boolean eventsRegisteredForIdentifier(String identifier) {
		if (identifier == null) {
			return false;
		}
		return (events.get(identifier) != null);
	}

	public static class ListenerContainer {

		public final Consumer<EventBase> listener;
		public final String eventIdentifier;
		public final int priority;
		public String listenerParentClassName = "[unknown]";
		public String listenerMethodName = "[unknown]";

		public ListenerContainer(String eventIdentifier, Consumer<EventBase> listener, int priority) {
			this.listener = listener;
			this.eventIdentifier = eventIdentifier;
			this.priority = priority;
		}

		public void noticeListener(EventBase event) {
			try {
				this.listener.accept(event);
			} catch (Exception e) {
				LOGGER.error("##################################");
				LOGGER.error("[ACARA] Failed to notify event listener!");
				LOGGER.error("[ACARA] Event Name: " + this.eventIdentifier);
				LOGGER.error("[ACARA] Listener Parent Class Name: " + this.listenerParentClassName);
				LOGGER.error("[ACARA] Listener Method Name In Parent Class: " + this.listenerMethodName);
				LOGGER.error("##################################");
				e.printStackTrace();
			}
		}

	}

	protected static class AnalyzedMethod {

		/** The actual method. **/
		public Method method;
		/** The object this method is part of. It's possible that this is the parent class. **/
		public Object parentObject;
		/** The class this method is part of. **/
		public Class<?> parentClass;
		/** If the method is static. **/
		public boolean isStatic;
		/** All annotations of the method. This includes annotations of methods in superclasses. **/
		public List<Annotation> annotations = new ArrayList<>();

		protected AnalyzedMethod() {
		}

		protected AnalyzedMethod(Method method, Object parentObjectOrClass) {
			this.method = method;
			this.parentObject = parentObjectOrClass;
			this.parentClass = this.tryGetParentClass();
			this.isStatic = Modifier.isStatic(method.getModifiers());
			collectMethodAnnotations(this.isStatic ? null : this.parentObject.getClass(), this.method, this.annotations);
		}

		protected Class<?> tryGetParentClass() {
			if (this.parentObject instanceof Class<?>) {
				return (Class<?>) this.parentObject;
			}
			return this.parentObject.getClass();
		}

		protected static void collectMethodAnnotations(Class<?> c, Method m, List<Annotation> addToList) {
			try {
				addToList.addAll(List.of(m.getAnnotations()));
				if (!Modifier.isStatic(m.getModifiers()) && (c != null)) {
					Class<?> sc = c.getSuperclass();
					if (sc != null) {
						try {
							Method sm = sc.getMethod(m.getName(), m.getParameterTypes());
							collectMethodAnnotations(sc, sm, addToList);
						} catch (Exception ignored) {}
					}
				}
			} catch (Exception ignored) {}
		}

	}

	protected static class EventMethod extends AnalyzedMethod {

		/** The event priority. **/
		public final int priority;
		/** The event type. **/
		public final Class<? extends EventBase> eventType;

		/** Will return the created EventMethod instance or NULL if the AnalyzedMethod was not a valid event method. **/
		protected static EventMethod tryCreateFrom(AnalyzedMethod method) {
			EventMethod em = new EventMethod(method);
			return (em.eventType != null) ? em : null;
		}

		protected EventMethod(AnalyzedMethod method) {

			super();
			this.method = method.method;
			this.parentObject = method.parentObject;
			this.parentClass = method.parentClass;
			this.isStatic = method.isStatic;
			this.annotations = method.annotations;

			this.priority = this.tryGetPriority();
			this.eventType = this.tryGetEventType();

		}

		protected Class<? extends EventBase> tryGetEventType() {
			try {
				if (this.method != null) {
					Class<?>[] params = this.method.getParameterTypes();
					if (params.length > 0) {
						Class<?> firstParam = params[0];
						if (EventBase.class.isAssignableFrom(firstParam)) {
							return (Class<? extends EventBase>) firstParam;
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		protected int tryGetPriority() {
			try {
				for (Annotation a : this.annotations) {
					if (a instanceof SubscribeEvent) {
						return ((SubscribeEvent) a).priority();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return 0;
		}

	}

}
