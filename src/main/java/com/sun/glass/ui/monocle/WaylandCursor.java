package com.sun.glass.ui.monocle;


import com.sun.glass.ui.Size;
import org.freedesktop.jaccall.Pointer;
import org.freedesktop.wayland.client.WlBufferProxy;
import org.freedesktop.wayland.client.WlCompositorProxy;
import org.freedesktop.wayland.client.WlOutputProxy;
import org.freedesktop.wayland.client.WlShmProxy;
import org.freedesktop.wayland.client.WlSurfaceEventsV4;
import org.freedesktop.wayland.client.WlSurfaceProxy;
import org.freedesktop.wayland.shared.WlShmFormat;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import static com.sun.glass.ui.monocle.Libpixman1.PIXMAN_OP_SRC;
import static com.sun.glass.ui.monocle.Libpixman1.PIXMAN_a8r8g8b8;

public class WaylandCursor extends NativeCursor implements WlSurfaceEventsV4 {

    private final Size              size;
    private final WaylandBufferPool waylandBufferPool;
    private final WlSurfaceProxy    wlSurfaceProxy;
    private final WaylandSeat       waylandSeat;
    private       int               hotspotX;
    private       int               hotspotY;

    private Pointer<Integer> cursorPixels;

    WaylandCursor(final WlShmProxy wlShmProxy,
                  final WlCompositorProxy wlCompositorProxy,
                  final WaylandSeat waylandSeat) {
        this.wlSurfaceProxy = wlCompositorProxy.createSurface(this);
        this.waylandSeat = waylandSeat;
        this.size = new Size(16,
                             16);
        this.waylandBufferPool = new WaylandBufferPoolFactory().create(wlShmProxy,
                                                                       this.size.width,
                                                                       this.size.height,
                                                                       2,
                                                                       WlShmFormat.ARGB8888);
    }

    Size getBestSize() {
        return this.size;
    }

    void setVisibility(final boolean visibility) {
        WaylandPlatformFactory.WL_LOOP.submit(() -> {
            this.isVisible = visibility;

            final WaylandInputDevicePointer waylandInputDevicePointer = waylandSeat.getWaylandInputDevicePointer();
            if (waylandInputDevicePointer != null) {
                final int enterSerial = waylandInputDevicePointer.getEnterSerial();
                waylandInputDevicePointer.getWlPointerProxy()
                                         .setCursor(enterSerial,
                                                    this.isVisible ? this.wlSurfaceProxy : null,
                                                    this.hotspotX,
                                                    this.hotspotY);
            }
        });
    }

    void setImage(final byte[] cursorImage) {
        WaylandPlatformFactory.WL_LOOP.submit(() -> {
            final WlBufferProxy wlBufferProxy = this.waylandBufferPool.popBuffer();
            this.wlSurfaceProxy.attach(wlBufferProxy,
                                       0,
                                       0);
            this.wlSurfaceProxy.damage(0,
                                       0,
                                       this.size.width,
                                       this.size.height);

            final WaylandBuffer waylandBuffer = (WaylandBuffer) wlBufferProxy.getImplementation();

            if (cursorPixels != null) {
                cursorPixels.close();
            }
            cursorPixels = Pointer.calloc(1,
                                          this.size.width * this.size.height * 4,
                                          Integer.class);

            IntBuffer sourceBuffer = ByteBuffer.wrap(cursorImage)
                                               .order(ByteOrder.nativeOrder())
                                               .asIntBuffer();

            int i = 0;
            while (sourceBuffer.position() < sourceBuffer.limit()) {
                int b = sourceBuffer.get();
                if ((b & 0xff000000) == 0) {
                    cursorPixels.writei(i,
                                        0);
                }
                else {
                    cursorPixels.writei(i,
                                        b);
                }
                i++;
            }

            final long src = Libpixman1.pixman_image_create_bits_no_clear(PIXMAN_a8r8g8b8,
                                                                          this.size.width,
                                                                          this.size.height,
                                                                          cursorPixels.address,
                                                                          this.size.width * 4);
            final long dst = waylandBuffer.getPixmanImage();

            Libpixman1.pixman_image_composite(PIXMAN_OP_SRC,
                                              src,
                                              0L,
                                              dst,
                                              (short) 0,
                                              (short) 0,
                                              (short) 0,
                                              (short) 0,
                                              (short) 0,
                                              (short) 0,
                                              (short) this.size.width,
                                              (short) this.size.height);
            this.wlSurfaceProxy.commit();
        });
    }

    void setLocation(final int x,
                     final int y) {
        //NOOP
    }

    void setHotSpot(final int hotspotX,
                    final int hotspotY) {
        WaylandPlatformFactory.WL_LOOP.submit(() -> {
            this.hotspotX = hotspotX;
            this.hotspotY = hotspotY;
            final WaylandInputDevicePointer waylandInputDevicePointer = waylandSeat.getWaylandInputDevicePointer();
            if (waylandInputDevicePointer != null) {
                final int enterSerial = waylandInputDevicePointer.getEnterSerial();
                waylandInputDevicePointer.getWlPointerProxy()
                                         .setCursor(enterSerial,
                                                    this.isVisible ? this.wlSurfaceProxy : null,
                                                    this.hotspotX,
                                                    this.hotspotY);
            }
        });
    }

    void shutdown() {
        WaylandPlatformFactory.WL_LOOP.submit(() -> {

            final WaylandInputDevicePointer waylandInputDevicePointer = waylandSeat.getWaylandInputDevicePointer();
            if (waylandInputDevicePointer != null) {
                waylandInputDevicePointer.getWlPointerProxy()
                                         .release();
            }
        });
    }

    @Override
    public void enter(final WlSurfaceProxy emitter,
                      @Nonnull
                      final WlOutputProxy output) {
        //NOOP monocle doesnt really support multi screen setup
    }

    @Override
    public void leave(final WlSurfaceProxy emitter,
                      @Nonnull
                      final WlOutputProxy output) {
        //NOOP monocle doesnt really support multi screen setup
    }
}
