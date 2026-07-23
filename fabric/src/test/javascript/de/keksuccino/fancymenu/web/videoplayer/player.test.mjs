import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import vm from 'node:vm';
import {fileURLToPath} from 'node:url';

const PLAYER_RELATIVE_PATH = path.join('common', 'src', 'main', 'resources', 'assets', 'fancymenu', 'web', 'videoplayer', 'player.html');
const PROJECT_ROOT = findProjectRoot(path.dirname(fileURLToPath(import.meta.url)));
const PLAYER_PATH = path.join(PROJECT_ROOT, PLAYER_RELATIVE_PATH);
const PLAYER_SCRIPT = extractPlayerScript(fs.readFileSync(PLAYER_PATH, 'utf8'));

function findProjectRoot(startDirectory) {
    let currentDirectory = startDirectory;
    while (true) {
        if (fs.existsSync(path.join(currentDirectory, PLAYER_RELATIVE_PATH))) {
            return currentDirectory;
        }
        const parentDirectory = path.dirname(currentDirectory);
        if (parentDirectory === currentDirectory) {
            throw new Error(`Could not locate ${PLAYER_RELATIVE_PATH} from ${startDirectory}`);
        }
        currentDirectory = parentDirectory;
    }
}

function extractPlayerScript(html) {
    const match = html.match(/<script>([\s\S]*?)<\/script>/u);
    if (!match) {
        throw new Error(`No inline player script found in ${PLAYER_PATH}`);
    }
    return match[1];
}

function createDeferred() {
    let resolve;
    let reject;
    const promise = new Promise((promiseResolve, promiseReject) => {
        resolve = promiseResolve;
        reject = promiseReject;
    });
    return {promise, resolve, reject};
}

async function flushPromises() {
    await new Promise(resolve => setImmediate(resolve));
}

function createHarness() {
    const errors = [];
    const logs = [];
    const xhrInstances = [];
    const createdObjectUrls = [];
    const revokedObjectUrls = [];
    const timers = new Map();
    const windowListeners = new Map();
    let nextTimerId = 1;
    let nextObjectUrlId = 1;
    let objectUrlCreateHook = null;

    class FakeClassList {
        constructor() {
            this.values = new Set();
        }

        add(value) {
            this.values.add(value);
        }

        remove(value) {
            this.values.delete(value);
        }

        contains(value) {
            return this.values.has(value);
        }
    }

    class FakeVideoElement {
        constructor() {
            this._src = '';
            this._currentSrc = '';
            this.attributes = new Map();
            this.listeners = new Map();
            this.playResults = [];
            this.readyState = 0;
            this.duration = Number.NaN;
            this.volume = 1;
            this.loop = false;
            this.muted = false;
            this.paused = true;
            this.currentTime = 0;
            this.videoWidth = 0;
            this.videoHeight = 0;
            this.error = null;
            this.controls = false;
            this.loadCalls = 0;
            this.pauseCalls = 0;
            this.playCalls = 0;
            this.throwAfterNextSrcAssignment = false;
            this.retainCurrentSourceWhenCleared = false;
        }

        get src() {
            return this._src;
        }

        set src(value) {
            this._src = String(value);
            if (this.throwAfterNextSrcAssignment) {
                this.throwAfterNextSrcAssignment = false;
                throw new Error('Synthetic src assignment failure');
            }
        }

        get currentSrc() {
            return this._currentSrc;
        }

        setAttribute(name, value) {
            this.attributes.set(name, String(value));
        }

        removeAttribute(name) {
            this.attributes.delete(name);
            if (name === 'src') {
                this._src = '';
            }
        }

        addEventListener(type, listener) {
            const listeners = this.listeners.get(type) || [];
            listeners.push(listener);
            this.listeners.set(type, listeners);
        }

        dispatch(type) {
            for (const listener of this.listeners.get(type) || []) {
                listener.call(this, {type, target: this});
            }
        }

        load() {
            this.loadCalls += 1;
            if (this._currentSrc !== this._src) {
                if (this._src || !this.retainCurrentSourceWhenCleared) {
                    this._currentSrc = this._src;
                }
                this.readyState = 0;
                this.duration = Number.NaN;
            }
        }

        pause() {
            this.pauseCalls += 1;
            this.paused = true;
        }

        play() {
            this.playCalls += 1;
            this.paused = false;
            return this.playResults.length > 0 ? this.playResults.shift() : Promise.resolve();
        }

        queuePlayResult(result) {
            this.playResults.push(result);
        }
    }

    class FakeXMLHttpRequest {
        constructor() {
            this.method = null;
            this.url = null;
            this.async = null;
            this.responseType = null;
            this.response = null;
            this.status = 0;
            this.sent = false;
            this.abortCalls = 0;
            this.handlersAtAbort = null;
            this.onprogress = null;
            this.onload = null;
            this.onerror = null;
            this.onabort = null;
            xhrInstances.push(this);
        }

        open(method, url, async) {
            this.method = method;
            this.url = url;
            this.async = async;
        }

        send() {
            this.sent = true;
        }

        abort() {
            this.abortCalls += 1;
            this.handlersAtAbort = this.captureHandlers();
            const abortHandler = this.onabort;
            if (abortHandler) {
                abortHandler.call(this);
            }
        }

        captureHandlers() {
            return {onprogress: this.onprogress, onload: this.onload, onerror: this.onerror, onabort: this.onabort};
        }

        complete(status = 200, response = {}) {
            this.status = status;
            this.response = response;
            const loadHandler = this.onload;
            if (loadHandler) {
                loadHandler.call(this);
            }
        }
    }

    const video = new FakeVideoElement();
    const videoContainer = {classList: new FakeClassList(), style: {}};
    const loader = {style: {}};
    const loaderText = {style: {}};
    const elements = new Map([
        ['videoContainer', videoContainer],
        ['videoPlayer', video],
        ['loader', loader],
        ['loaderText', loaderText]
    ]);
    const document = {
        body: {appendChild() {}},
        createElement() {
            throw new Error('The player fixture unexpectedly tried to create a missing DOM element');
        },
        getElementById(id) {
            return elements.get(id) || null;
        }
    };
    const window = {
        location: {search: ''},
        addEventListener(type, listener) {
            const listeners = windowListeners.get(type) || [];
            listeners.push(listener);
            windowListeners.set(type, listeners);
        }
    };
    const fakeUrl = {
        createObjectURL(response) {
            const objectUrl = `blob:fancymenu-test-${nextObjectUrlId++}`;
            createdObjectUrls.push({objectUrl, response});
            if (objectUrlCreateHook) {
                objectUrlCreateHook(objectUrl);
            }
            return objectUrl;
        },
        revokeObjectURL(objectUrl) {
            revokedObjectUrls.push(objectUrl);
        }
    };
    const setTimeoutFake = (callback, delay) => {
        const timerId = nextTimerId++;
        timers.set(timerId, {callback, delay, cleared: false, fired: false});
        return timerId;
    };
    const clearTimeoutFake = timerId => {
        const timer = timers.get(timerId);
        if (timer) {
            timer.cleared = true;
        }
    };
    const context = vm.createContext({
        console: {
            error(...values) {
                errors.push(values.join(' '));
            },
            log(...values) {
                logs.push(values.join(' '));
            }
        },
        document,
        window,
        URL: fakeUrl,
        URLSearchParams,
        XMLHttpRequest: FakeXMLHttpRequest,
        HTMLMediaElement: {HAVE_NOTHING: 0, HAVE_METADATA: 1, HAVE_CURRENT_DATA: 2, HAVE_FUTURE_DATA: 3, HAVE_ENOUGH_DATA: 4},
        setTimeout: setTimeoutFake,
        clearTimeout: clearTimeoutFake
    });
    new vm.Script(PLAYER_SCRIPT, {filename: PLAYER_PATH}).runInContext(context);

    return {
        api: window.videoPlayerAPI,
        createdObjectUrls,
        errors,
        logs,
        revokedObjectUrls,
        video,
        window,
        xhrInstances,
        activeTimerIds(delay) {
            return [...timers.entries()].filter(([, timer]) => !timer.cleared && !timer.fired && (delay === undefined || timer.delay === delay)).map(([timerId]) => timerId);
        },
        dispatchWindowEvent(type) {
            for (const listener of windowListeners.get(type) || []) {
                listener.call(window, {type, target: window});
            }
        },
        fireTimer(timerId, force = false) {
            const timer = timers.get(timerId);
            assert.ok(timer, `Unknown timer ${timerId}`);
            if (timer.fired || (timer.cleared && !force)) {
                return false;
            }
            timer.fired = true;
            timer.callback();
            return true;
        },
        setObjectUrlCreateHook(hook) {
            objectUrlCreateHook = hook;
        },
        timer(timerId) {
            return timers.get(timerId);
        }
    };
}

function assertXhrCallbacksDetached(xhr) {
    assert.deepEqual(xhr.handlersAtAbort, {onprogress: null, onload: null, onerror: null, onabort: null});
}

function invokeCapturedXhrCallbacks(xhr, handlers) {
    xhr.status = 200;
    xhr.response = {stale: true};
    handlers.onprogress.call(xhr, {lengthComputable: true, loaded: 1, total: 1});
    handlers.onload.call(xhr);
    handlers.onerror.call(xhr);
    handlers.onabort.call(xhr);
}

test('rapid A to B to C loads ignore every stale XHR callback', () => {
    const harness = createHarness();
    harness.api.loadVideo('file:///A.mp4');
    const xhrA = harness.xhrInstances[0];
    const handlersA = xhrA.captureHandlers();

    harness.api.loadVideo('file:///B.mp4');
    const xhrB = harness.xhrInstances[1];
    const handlersB = xhrB.captureHandlers();
    assert.equal(xhrA.abortCalls, 1);
    assertXhrCallbacksDetached(xhrA);

    harness.api.loadVideo('file:///C.mp4');
    const xhrC = harness.xhrInstances[2];
    assert.equal(xhrB.abortCalls, 1);
    assertXhrCallbacksDetached(xhrB);

    invokeCapturedXhrCallbacks(xhrA, handlersA);
    invokeCapturedXhrCallbacks(xhrB, handlersB);
    assert.equal(harness.createdObjectUrls.length, 0);
    assert.equal(harness.video.src, '');
    assert.equal(harness.api.currentSettings.currentVideoSrc, 'file:///C.mp4');

    xhrC.complete(200, {source: 'C'});
    assert.equal(harness.video.src, 'blob:fancymenu-test-1');
    assert.equal(harness.video.currentSrc, 'blob:fancymenu-test-1');
    assert.equal(harness.createdObjectUrls.length, 1);
});

test('same-source reload gets a new owner and rejects the first load result', () => {
    const harness = createHarness();
    harness.api.loadVideo('file:///same.mp4');
    const firstXhr = harness.xhrInstances[0];
    const firstLoadHandler = firstXhr.onload;

    harness.api.loadVideo('file:///same.mp4');
    const secondXhr = harness.xhrInstances[1];
    assert.equal(firstXhr.abortCalls, 1);
    assertXhrCallbacksDetached(firstXhr);

    firstXhr.status = 200;
    firstXhr.response = {generation: 1};
    firstLoadHandler.call(firstXhr);
    assert.equal(harness.createdObjectUrls.length, 0);

    secondXhr.complete(200, {generation: 2});
    assert.equal(harness.video.src, 'blob:fancymenu-test-1');
    assert.equal(harness.api.currentSettings.currentVideoSrc, 'file:///same.mp4');
});

test('local blob loading tolerates Chromium retaining the previous read-only currentSrc', () => {
    const harness = createHarness();
    harness.api.loadVideo('data:video/mp4;base64,AA==#direct');
    harness.video.retainCurrentSourceWhenCleared = true;
    harness.api.loadVideo('file:///local-after-direct.mp4');
    assert.equal(harness.video.src, '');
    assert.equal(harness.video.currentSrc, 'data:video/mp4;base64,AA==#direct');

    const localXhr = harness.xhrInstances[0];
    localXhr.complete(200, {source: 'local'});
    assert.equal(harness.video.src, 'blob:fancymenu-test-1');
    assert.equal(harness.video.currentSrc, 'blob:fancymenu-test-1');
});

test('fallback timers and fallback XHRs are cancelled by a replacement or direct readiness', () => {
    const harness = createHarness();
    harness.api.loadVideo('https://example.test/A.mp4');
    const timerA = harness.activeTimerIds(2000)[0];

    harness.api.loadVideo('https://example.test/B.mp4');
    const timerB = harness.activeTimerIds(2000)[0];
    assert.equal(harness.timer(timerA).cleared, true);
    harness.fireTimer(timerA, true);
    assert.equal(harness.xhrInstances.length, 0);

    harness.video.readyState = 1;
    harness.video.dispatch('loadedmetadata');
    assert.equal(harness.timer(timerB).cleared, true);
    harness.fireTimer(timerB, true);
    assert.equal(harness.xhrInstances.length, 0);

    harness.api.loadVideo('https://example.test/C.mp4');
    const timerC = harness.activeTimerIds(2000)[0];
    harness.fireTimer(timerC);
    const fallbackXhr = harness.xhrInstances[0];
    const fallbackLoadHandler = fallbackXhr.onload;
    harness.video.readyState = 2;
    harness.video.dispatch('loadedmetadata');
    assert.equal(fallbackXhr.abortCalls, 1);
    assertXhrCallbacksDetached(fallbackXhr);

    fallbackXhr.status = 200;
    fallbackXhr.response = {stale: true};
    fallbackLoadHandler.call(fallbackXhr);
    assert.equal(harness.createdObjectUrls.length, 0);
    assert.equal(harness.video.src, 'https://example.test/C.mp4');
});

test('completed blob fallback cannot replace a direct source that recovered', () => {
    const recoveredBeforeCompletion = createHarness();
    recoveredBeforeCompletion.api.loadVideo('https://example.test/direct.mp4');
    recoveredBeforeCompletion.fireTimer(recoveredBeforeCompletion.activeTimerIds(2000)[0]);
    const firstFallbackXhr = recoveredBeforeCompletion.xhrInstances[0];
    recoveredBeforeCompletion.video.readyState = 2;
    firstFallbackXhr.complete(200, {fallback: true});
    assert.equal(recoveredBeforeCompletion.video.src, 'https://example.test/direct.mp4');
    assert.equal(recoveredBeforeCompletion.createdObjectUrls.length, 0);

    const recoveredDuringUrlCreation = createHarness();
    recoveredDuringUrlCreation.api.loadVideo('https://example.test/direct.mp4');
    recoveredDuringUrlCreation.fireTimer(recoveredDuringUrlCreation.activeTimerIds(2000)[0]);
    recoveredDuringUrlCreation.setObjectUrlCreateHook(() => {
        recoveredDuringUrlCreation.video.readyState = 2;
    });
    recoveredDuringUrlCreation.xhrInstances[0].complete(200, {fallback: true});
    assert.equal(recoveredDuringUrlCreation.video.src, 'https://example.test/direct.mp4');
    assert.deepEqual(recoveredDuringUrlCreation.revokedObjectUrls, ['blob:fancymenu-test-1']);
});

test('media events mutate state only for the exact assigned source', async () => {
    const harness = createHarness();
    const expectedSource = 'https://example.test/current.mp4';
    harness.api.loadVideo(expectedSource);
    const fallbackTimer = harness.activeTimerIds(2000)[0];
    harness.api.setVolume(0.75);
    harness.video.volume = 0.1;
    harness.video.readyState = 4;
    harness.video.error = {code: 2};
    harness.video.src = 'https://example.test/stale.mp4';

    for (const eventType of ['loadedmetadata', 'canplay', 'canplaythrough', 'playing', 'pause', 'waiting', 'ended', 'error']) {
        harness.video.dispatch(eventType);
    }
    assert.equal(harness.api.currentSettings.videoLoadedAndReady, false);
    assert.equal(harness.api.currentSettings.playRequestPending, true);
    assert.equal(harness.video.volume, 0.1);
    assert.equal(harness.errors.length, 0);
    assert.equal(harness.timer(fallbackTimer).cleared, false);

    harness.video.src = expectedSource;
    harness.video.load();
    harness.video.readyState = 3;
    harness.video.dispatch('canplay');
    await flushPromises();
    assert.equal(harness.api.currentSettings.videoLoadedAndReady, true);
    assert.equal(harness.api.currentSettings.playRequestPending, false);
    assert.equal(harness.video.volume, 0.75);
    assert.equal(harness.timer(fallbackTimer).cleared, true);
});

test('stale play promise rejection and fulfillment cannot mutate a newer load', async () => {
    const harness = createHarness();
    const playA = createDeferred();
    harness.api.loadVideo('https://example.test/A.mp4');
    harness.video.queuePlayResult(playA.promise);
    harness.video.readyState = 3;
    harness.video.dispatch('canplay');

    harness.api.loadVideo('https://example.test/B.mp4');
    playA.reject(new Error('late A rejection'));
    await flushPromises();
    assert.equal(harness.video.playCalls, 1);
    assert.equal(harness.video.muted, false);
    assert.equal(harness.api.currentSettings.playRequestPending, true);

    const playB = createDeferred();
    harness.video.queuePlayResult(playB.promise);
    harness.video.readyState = 3;
    harness.video.dispatch('canplay');
    assert.equal(harness.video.playCalls, 2);
    harness.api.loadVideo('https://example.test/C.mp4');
    playB.resolve();
    await flushPromises();
    assert.equal(harness.api.currentSettings.currentVideoSrc, 'https://example.test/C.mp4');
    assert.equal(harness.api.currentSettings.playRequestPending, true);
});

test('retiring a load invalidates its delayed unmute callback', async () => {
    const harness = createHarness();
    const audiblePlay = createDeferred();
    const mutedPlay = createDeferred();
    harness.api.loadVideo('https://example.test/A.mp4');
    harness.video.queuePlayResult(audiblePlay.promise);
    harness.video.queuePlayResult(mutedPlay.promise);
    harness.video.readyState = 3;
    harness.video.dispatch('canplay');

    audiblePlay.reject(new Error('autoplay requires mute'));
    await flushPromises();
    assert.equal(harness.video.playCalls, 2);
    mutedPlay.resolve();
    await flushPromises();
    const unmuteTimer = harness.activeTimerIds(500)[0];
    assert.ok(unmuteTimer);
    assert.equal(harness.video.muted, true);

    harness.api.loadVideo('https://example.test/B.mp4');
    assert.equal(harness.timer(unmuteTimer).cleared, true);
    assert.equal(harness.video.muted, false);
    harness.fireTimer(unmuteTimer, true);
    assert.equal(harness.video.muted, false);
    assert.equal(harness.api.currentSettings.muted, false);
});

test('temporary autoplay mute is restored when its load retires before muted play settles', async () => {
    const harness = createHarness();
    const audiblePlay = createDeferred();
    const mutedPlay = createDeferred();
    harness.api.loadVideo('https://example.test/A.mp4');
    harness.video.queuePlayResult(audiblePlay.promise);
    harness.video.queuePlayResult(mutedPlay.promise);
    harness.video.readyState = 3;
    harness.video.dispatch('canplay');

    audiblePlay.reject(new Error('autoplay requires mute'));
    await flushPromises();
    assert.equal(harness.video.muted, true);
    assert.equal(harness.api.currentSettings.muted, false);

    harness.api.loadVideo('https://example.test/B.mp4');
    assert.equal(harness.video.muted, false);
    mutedPlay.resolve();
    await flushPromises();
    assert.equal(harness.video.muted, false);
    assert.equal(harness.api.currentSettings.muted, false);
    assert.equal(harness.activeTimerIds(500).length, 0);

    const explicitMuteHarness = createHarness();
    const explicitAudiblePlay = createDeferred();
    const explicitMutedPlay = createDeferred();
    explicitMuteHarness.api.loadVideo('https://example.test/A.mp4');
    explicitMuteHarness.video.queuePlayResult(explicitAudiblePlay.promise);
    explicitMuteHarness.video.queuePlayResult(explicitMutedPlay.promise);
    explicitMuteHarness.video.readyState = 3;
    explicitMuteHarness.video.dispatch('canplay');
    explicitAudiblePlay.reject(new Error('autoplay requires mute'));
    await flushPromises();
    explicitMuteHarness.api.setMuted(true);
    explicitMuteHarness.api.loadVideo('https://example.test/B.mp4');
    explicitMutedPlay.resolve();
    await flushPromises();
    assert.equal(explicitMuteHarness.video.muted, true);
    assert.equal(explicitMuteHarness.api.currentSettings.muted, true);
    assert.equal(explicitMuteHarness.activeTimerIds(500).length, 0);
});

test('empty reset and source replacement revoke only player-owned object URLs', () => {
    const harness = createHarness();
    harness.api.loadVideo('file:///owned-A.mp4');
    const xhrA = harness.xhrInstances[0];
    const staleLoadHandler = xhrA.onload;
    xhrA.complete(200, {source: 'A'});
    assert.equal(harness.video.src, 'blob:fancymenu-test-1');

    harness.api.loadVideo('');
    assert.equal(harness.video.src, '');
    assert.equal(harness.video.currentSrc, '');
    assert.equal(harness.api.currentSettings.currentVideoSrc, '');
    assert.equal(harness.api.currentSettings.videoLoadedAndReady, false);
    assert.equal(harness.api.currentSettings.playRequestPending, false);
    assert.deepEqual(harness.revokedObjectUrls, ['blob:fancymenu-test-1']);

    xhrA.status = 200;
    xhrA.response = {stale: true};
    staleLoadHandler.call(xhrA);
    assert.equal(harness.createdObjectUrls.length, 1);

    harness.api.loadVideo('https://example.test/external-A.mp4');
    harness.api.loadVideo('file:///owned-B.mp4');
    harness.xhrInstances[1].complete(200, {source: 'B'});
    harness.api.loadVideo('https://example.test/external-B.mp4');
    assert.deepEqual(harness.revokedObjectUrls, ['blob:fancymenu-test-1', 'blob:fancymenu-test-2']);
    assert.ok(harness.revokedObjectUrls.every(source => source.startsWith('blob:fancymenu-test-')));
});

test('failed blob assignment detaches and immediately revokes the new object URL', () => {
    const harness = createHarness();
    harness.api.loadVideo('file:///assignment-failure.mp4');
    harness.video.throwAfterNextSrcAssignment = true;
    harness.xhrInstances[0].complete(200, {source: 'broken'});

    assert.deepEqual(harness.createdObjectUrls.map(entry => entry.objectUrl), ['blob:fancymenu-test-1']);
    assert.deepEqual(harness.revokedObjectUrls, ['blob:fancymenu-test-1']);
    assert.equal(harness.video.src, '');
    assert.equal(harness.video.currentSrc, '');
    assert.equal(harness.api.currentSettings.videoLoadedAndReady, false);
    harness.video.readyState = 4;
    harness.video.dispatch('canplaythrough');
    assert.equal(harness.api.currentSettings.videoLoadedAndReady, false);
});

test('pagehide disposal aborts work, clears timers, revokes ownership, and is idempotent', () => {
    const xhrHarness = createHarness();
    xhrHarness.api.loadVideo('file:///pending.mp4');
    const pendingXhr = xhrHarness.xhrInstances[0];
    const pendingHandlers = pendingXhr.captureHandlers();
    xhrHarness.dispatchWindowEvent('pagehide');
    assert.equal(pendingXhr.abortCalls, 1);
    assertXhrCallbacksDetached(pendingXhr);
    invokeCapturedXhrCallbacks(pendingXhr, pendingHandlers);
    assert.equal(xhrHarness.createdObjectUrls.length, 0);
    assert.equal(xhrHarness.api.play(), false);
    assert.equal(xhrHarness.api.currentSettings.playRequestPending, false);
    assert.equal(xhrHarness.api.loadVideo('file:///ignored.mp4'), false);
    xhrHarness.dispatchWindowEvent('pagehide');
    xhrHarness.api.dispose();
    assert.equal(pendingXhr.abortCalls, 1);

    const timerHarness = createHarness();
    timerHarness.api.loadVideo('https://example.test/pending.mp4');
    const pendingTimer = timerHarness.activeTimerIds(2000)[0];
    timerHarness.dispatchWindowEvent('pagehide');
    assert.equal(timerHarness.timer(pendingTimer).cleared, true);
    timerHarness.fireTimer(pendingTimer, true);
    assert.equal(timerHarness.xhrInstances.length, 0);

    const objectUrlHarness = createHarness();
    objectUrlHarness.api.loadVideo('file:///owned.mp4');
    objectUrlHarness.xhrInstances[0].complete(200, {source: 'owned'});
    objectUrlHarness.dispatchWindowEvent('pagehide');
    objectUrlHarness.dispatchWindowEvent('pagehide');
    assert.deepEqual(objectUrlHarness.revokedObjectUrls, ['blob:fancymenu-test-1']);
    assert.equal(objectUrlHarness.video.src, '');
});

test('reentrant object URL creation cannot transfer ownership from a retired load', () => {
    const harness = createHarness();
    harness.api.loadVideo('file:///A.mp4');
    let replaced = false;
    harness.setObjectUrlCreateHook(() => {
        if (!replaced) {
            replaced = true;
            harness.api.loadVideo('file:///B.mp4');
        }
    });

    harness.xhrInstances[0].complete(200, {source: 'A'});
    assert.equal(harness.api.currentSettings.currentVideoSrc, 'file:///B.mp4');
    assert.deepEqual(harness.revokedObjectUrls, ['blob:fancymenu-test-1']);
    assert.equal(harness.video.src, '');

    harness.xhrInstances[1].complete(200, {source: 'B'});
    assert.equal(harness.video.src, 'blob:fancymenu-test-2');
    harness.api.loadVideo('https://example.test/C.mp4');
    assert.deepEqual(harness.revokedObjectUrls, ['blob:fancymenu-test-1', 'blob:fancymenu-test-2']);
    assert.equal(harness.video.src, 'https://example.test/C.mp4');
});
