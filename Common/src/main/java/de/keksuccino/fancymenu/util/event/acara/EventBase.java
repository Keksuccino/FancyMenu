
//Acara - Simple Java Event System

//Copyright (c) 2023 Keksuccino.
//Acara is licensed under MIT.

package de.keksuccino.fancymenu.util.event.acara;

/** The base class for events. **/
public abstract class EventBase {

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
