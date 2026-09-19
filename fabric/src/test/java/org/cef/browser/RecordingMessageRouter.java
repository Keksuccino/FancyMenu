package org.cef.browser;

import org.cef.handler.CefMessageRouterHandler;

/** Native-free router fake; CEF's package-private constructor requires this package. */
public final class RecordingMessageRouter extends CefMessageRouter {

    private int disposalCount;
    public boolean acceptHandler = true;
    public RuntimeException disposalFailure;

    public int getDisposalCount() {
        return this.disposalCount;
    }

    @Override
    public void dispose() {
        this.disposalCount++;
        if (this.disposalFailure != null) throw this.disposalFailure;
    }

    @Override
    public boolean addHandler(CefMessageRouterHandler handler, boolean first) {
        return this.acceptHandler;
    }

    @Override
    public boolean removeHandler(CefMessageRouterHandler handler) {
        return true;
    }

    @Override
    public void cancelPending(CefBrowser browser, CefMessageRouterHandler handler) {}

}
