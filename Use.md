# Introduction #

VST Plugins are made available through a standard toot2 SPI.
They only rely on ~/toot/vst.paths to exist and contain one plugin path per line.
toot2 provides support for this file, including a modal dialog which is
presented automatically at startup if the file does not exist, and allows the user to add multiple paths to the file. Removal of paths is not supported since, due to the way pugins are cached, it would have no effect. The only way to remove plugins is to delete them or move them within the filesystem which will cause them to be disabled in the cache.

VST plugins use two human readable caches, one for instruments and one for effects.

  * ~/toot/synths/vsti.cache
  * ~/toot/audio/vstfx.cache

Fields in each line of each cache are comma separated index, id, file, product, company.
These caches are generated and maintained automatically.

Textual details may be edited in these caches to change product names etc.
Plugins may be disabled by negating the index at the start of a line.
The index should not be changed in value, this would break automation persistence portability.

There is a limit of 127 plugin effects and 127 plugin instruments.

When using this project with toot2 do not forget to integrate META-INF/services and to move the /lib jar and relevant native libraries.

# Details #

No code is required for basic support.

To show the path setup dialog explicitly, which may be useful for maintenance of the file within an application:
```
    VstSetupUI.showDialog(VstSetup.readPaths());
```
This would be irrelevant if this project was not used and this can be tested with:
```
    VstSetup.isVstAvailable();
```

# Compatibility #

VST Effect support is compatible with toot Release 2, VST plugins may be inserted in the audio mixer strips just like java plugins.

VST Instrument support requires support of additional toot Release 3 features such as the toot midi model, synthesizer model and audio system model.

# Portability #

Unlike java plugins, VST plugins are not inherently portable between client machines. Automation data used on a different client machine to the one it was produced on may not have access to the required plugins. Even if it were legal, VST plugins could not be copied between machines because toot only knows about the native library component of the plugins, many plugins have additional data files which are unknown to toot. Hence this limitation can never be removed, it is a common limitation of all DAW software.

Conversely java plugins are in principle always portable between client machines. Commercial closed source java plugins are not possible due to the use of the GPL.