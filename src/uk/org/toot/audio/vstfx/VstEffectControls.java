// Copyright (C) 2009 Steve Taylor.
// Distributed under the Toot Software License, Version 1.0. (See
// accompanying file LICENSE_1_0.txt or copy at
// http://www.toot.org.uk/LICENSE_1_0.txt)

package uk.org.toot.audio.vstfx;

import java.io.File;

import com.synthbot.audioplugin.vst.vst2.JVstHost2;

import uk.org.toot.audio.core.AudioControls;
import uk.org.toot.audio.spi.AudioControlServiceDescriptor;
import uk.org.toot.control.NativeSupport;
import uk.org.toot.misc.VstHost;
import uk.org.toot.misc.VstNativeSupport;

public class VstEffectControls extends AudioControls implements VstHost
{
	private JVstHost2 vstfx;
	private NativeSupport nativeSupport;
	
	public VstEffectControls(AudioControlServiceDescriptor d) throws Exception {
		super(d.getModuleId(), d.getName());
		// buffer size is large for bad plugins that only set it ONCE
		vstfx = JVstHost2.newInstance(new File(d.getPluginPath()), 44100, 4410);
		nativeSupport = new VstNativeSupport(this, vstfx);
	}
	
	public NativeSupport getNativeSupport() {
		return nativeSupport;
	}
	
	// causes plugins to show Preset menu
	public boolean isPluginParent() { 
		return true; 
	}
	
	public JVstHost2 getVst() {
		return vstfx;
	}
}
