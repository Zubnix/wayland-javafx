package com.sun.glass.ui.monocle;


import org.freedesktop.wayland.client.WlSurfaceProxy;
import org.freedesktop.wayland.client.egl.EglWindow;

public class WaylandAcceleratedScreen extends AcceleratedScreen {

    /*
     * An AcceleratedScreen provides a way to get an OpenGL ES context on the screen. This typically uses the native
     * EGL API to create a drawing surface. While the EGL API used to create the surface is standard
     * (eglCreateWindowSurface), it requires a NativeWindowType parameter whose meaning varies from platform to
     * platform. It is the call to the native function eglCreateWindowSurface that typically needs to be customized for
     * a new port. This function on some platforms takes 0 as an argument to indicate that the screen's framebuffer is
     * to be used for output. On other platforms a pointer to some data structure is required.
     */

    /**
     * Perform basic egl intialization - open the display, create the drawing
     * surface, and create a GL context to that drawing surface.
     *
     * @param attributes - attributes to be used for filtering the EGL
     *                   configurations to choose from
     *
     * @throws GLException
     * @throws UnsatisfiedLinkError
     */
    WaylandAcceleratedScreen(final int[] attributes) throws GLException, UnsatisfiedLinkError {
        super(attributes);
    }

    @Override
    protected long platformGetNativeDisplay() {
        WaylandPlatform waylandPlatform = (WaylandPlatform) NativePlatformFactory.getNativePlatform();
        return waylandPlatform.getWlDisplayProxy().pointer;
    }

    @Override
    protected long platformGetNativeWindow() {

        final WaylandScreen screen = (WaylandScreen) NativePlatformFactory.getNativePlatform()
                                                                          .getScreen();

        final WlSurfaceProxy wlSurfaceProxy = screen.getWlSurfaceProxy();
        final int            width          = screen.getWidth();
        final int            height         = screen.getHeight();
        final EglWindow eglWindow = EglWindow.create(wlSurfaceProxy,
                                                     width,
                                                     height);
        return eglWindow.pointer;
    }
}
