package com.sun.glass.ui.monocle;

import org.freedesktop.wayland.client.WlCompositorEventsV3;
import org.freedesktop.wayland.client.WlCompositorProxy;
import org.freedesktop.wayland.client.WlDisplayProxy;
import org.freedesktop.wayland.client.WlOutputProxy;
import org.freedesktop.wayland.client.WlRegistryEvents;
import org.freedesktop.wayland.client.WlRegistryProxy;
import org.freedesktop.wayland.client.WlSeatProxy;
import org.freedesktop.wayland.client.WlShellEvents;
import org.freedesktop.wayland.client.WlShellProxy;
import org.freedesktop.wayland.client.WlShmProxy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ExecutionException;

public class WaylandPlatform extends NativePlatform implements WlRegistryEvents {

    /*
     * A NativePlatform bundles together a NativeScreen, NativeCursor and InputDeviceRegistry.
     * NativePlatform is a singleton, as are the classes it contains.
     */

    @Nonnull
    private final WlDisplayProxy    wlDisplayProxy;
    @Nullable
    private       WlCompositorProxy compositorProxy;
    @Nullable
    private       WlShellProxy      shellProxy;
    @Nullable
    private       WaylandShm        waylandShm;
    private       WaylandOutput     waylandOutput;
    private       WaylandSeat       waylandSeat;

    WaylandPlatform(@Nonnull final WlDisplayProxy wlDisplayProxy) {
        LinuxSystem.getLinuxSystem().loadLibrary();
        this.wlDisplayProxy = wlDisplayProxy;
    }

    protected InputDeviceRegistry createInputDeviceRegistry() {
        return new InputDeviceRegistry();
    }

    protected WaylandCursor createCursor() {
        try {
            return WaylandPlatformFactory.WL_LOOP.submit(() -> {
                if (this.waylandShm == null ||
                    this.compositorProxy == null) {
                    ensureGlobals();
                }

                if (this.waylandSeat == null) {
                    return null;
                }
                else {
                    return new WaylandCursor(this.waylandShm.getWlShmProxy(),
                                             this.compositorProxy,
                                             this.waylandSeat);
                }
            })
                                                 .get();
        }
        catch (InterruptedException | ExecutionException e) {
            throw new Error(e);
        }
    }

    private void ensureGlobals() {
        //make sure we receive all globals
        while (this.waylandOutput == null ||
               this.compositorProxy == null ||
               this.shellProxy == null ||
               this.waylandShm == null) {
            //We keep on tripping (pun intended) until we have received all globals.
            //FIXME safe guard so we can bail if required globals are never received
            this.wlDisplayProxy.roundtrip();
        }
    }

    protected WaylandScreen createScreen() {
        try {
            return WaylandPlatformFactory.WL_LOOP.submit(() -> {
                if (this.waylandOutput == null ||
                    this.compositorProxy == null ||
                    this.shellProxy == null ||
                    this.waylandShm == null) {
                    ensureGlobals();
                }
                return new WaylandScreen(new WaylandBufferPoolFactory(),
                                         this.waylandOutput,
                                         this.compositorProxy,
                                         this.shellProxy,
                                         this.waylandShm);
            })
                                                 .get();
        }
        catch (InterruptedException | ExecutionException e) {
            throw new Error(e);
        }
    }

    @Override
    public void global(final WlRegistryProxy emitter,
                       final int name,
                       @Nonnull final String interface_,
                       final int version) {
        if (WlCompositorProxy.INTERFACE_NAME.equals(interface_)) {
            this.compositorProxy = emitter.bind(name,
                                                WlCompositorProxy.class,
                                                WlCompositorEventsV3.VERSION,
                                                new WlCompositorEventsV3() {
                                                });
        }
        else if (WlShmProxy.INTERFACE_NAME.equals(interface_)) {
            this.waylandShm = new WaylandShm(emitter,
                                             name,
                                             interface_,
                                             version);
        }
        else if (WlShellProxy.INTERFACE_NAME.equals(interface_)) {
            this.shellProxy = emitter.bind(name,
                                           WlShellProxy.class,
                                           WlShellEvents.VERSION,
                                           new WlShellEvents() {
                                           });
        }
        else if (WlOutputProxy.INTERFACE_NAME.equals(interface_)) {
            //monocle doesnt support multiple outputs
            if (this.waylandOutput == null) {
                this.waylandOutput = new WaylandOutput(this.wlDisplayProxy,
                                                       name,
                                                       emitter);
            }
        }
        else if (WlSeatProxy.INTERFACE_NAME.equals(interface_)) {
            //TODO support multi seat (does monocle support this?)
            if (this.waylandSeat == null) {
                this.waylandSeat = new WaylandSeat(emitter,
                                                   name,
                                                   interface_,
                                                   version,
                                                   getInputDeviceRegistry());
            }
        }
    }

    @Override
    public synchronized AcceleratedScreen getAcceleratedScreen(final int[] attributes) throws GLException, UnsatisfiedLinkError {
        if (accScreen == null) {
            accScreen = new WaylandAcceleratedScreen(attributes);
        }
        return accScreen;
    }

    @Override
    public void globalRemove(final WlRegistryProxy emitter,
                             final int name) {
        //TODO listen for eg seat removals
    }

    @Nonnull
    public WlDisplayProxy getWlDisplayProxy() {
        return wlDisplayProxy;
    }
}
