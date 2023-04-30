
//Acara - Simple Java Event System

//Copyright (c) 2020-2023 Keksuccino.
//Acara is licensed under MIT.

package de.keksuccino.fancymenu.event.acara;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** The base class for events. **/
public abstract class EventBase {

	private static final Logger LOGGER = LogManager.getLogger();

	private boolean canceled = false;

	public abstract boolean isCancelable();

	public void setCanceled(boolean b) {
		try {
			if (!this.isCancelable()) {
				throw new EventCancellationException("[ACARA] Tried to cancel non-cancelable event: " + this.getClass().getName());
			} else {
				this.canceled = b;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean isCanceled() {
		return this.canceled;
	}

}
