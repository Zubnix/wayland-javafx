# wayland-javafx
A Wayland backend for JavaFX.

This is a work in progress. Currently software rendering, mouse & keyboard works (touch too but untested).

TODO:
 - Rewrite & cleanup code to production quality standards.
 - Remove all 3rd party jdk libraries.
 - Include in openjfx as monocle back-end?
 - Create a non monocle, desktop enabled wayland back-end.
 
DONE:
 - output using hw rendering (via wayland drm buffers). Requires a patch for JavaFX shader bug. Fix available [here](https://bitbucket.org/javafxports/8u60-rt/commits/595633bbaae36f98d85d47d276294442ea43488c). This patch is not needed if you intent to use sw rendering.
 - output using sw rendering (via wayland shared memory buffers)
 - input handling through wayland's input protocols (keyboard+pointer+touch)

This library *will* make use of libraries not present in a standard jdk/jfx install as to get things up and running as quickly as possible.
 - [wayland-java-bindings](https://github.com/udevbe/wayland-java-bindings)
 - [jaccall](https://github.com/udevbe/jaccall)

 The primary goal is to be able to use JavaFX as a pure client side widget toolkit capable to run on any Wayland compositor.
 
 Initial effort will focus on creating a Wayland implementation for the JavaFX Monocle back-end. This back-end is meant for the embedded, fullscreen, single application use case.
 
 Secondary effort is to create JavaFX Wayland back-end for general desktop usage.
 
 #Running
 
 In case you've decided you're crazy enough to take this ugly poc for a spin. Here's how:
 
 - Make sure you have a javafx version with monocle support available. This will most likely mean you'll have to build it from source (eglx86 profile for non embedded usage). See https://wiki.openjdk.java.net/display/OpenJFX/Building+OpenJFX#BuildingOpenJFX-CrossBuilds

 - Edit the pom.xml of the project and make sure ```<jfxrt.path>/home/zubzub/hg/openjfx8-devrt/build/sdk/rt/lib/ext/jfxrt.jar</jfxrt.path>```matches the jfxrt.jar of your monacle enabled and installed jfx library.

 - Build the project. You will also need to build the latest SNAPSHOT versions of [jaccall](https://github.com/udevbe/jaccall) & [wayland-java-bindings](https://github.com/udevbe/wayland-java-bindings).
 
 - Copy the ```./target/wayland-javafx-1.0.0-SNAPSHOT.jar``` to your local jdk installation's ext folder; eg. ```/usr/lib/jvm/oracle-jdk-bin-1.8/jre/lib/ext/```

 - Run your javafx application; eg. ```unset DISPLAY && java -Dglass.platform=Monocle -Dmonocle.platform=Wayland -Dembedded=monocle -jar Ensemble8.jar```. Leave out the ```-Dembedded=monocle``` option when using the sw renderer.
 
 - Make sure you delete ```wayland-javafx-1.0.0-SNAPSHOT.jar``` from your jdk installation once you're done as it might introduce some unwanted behavior in other programs.
