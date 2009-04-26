// Copyright (C) 2009 Steve Taylor.
// Distributed under the Toot Software License, Version 1.0. (See
// accompanying file LICENSE_1_0.txt or copy at
// http://www.toot.org.uk/LICENSE_1_0.txt)

package uk.org.toot.misc;

import javax.sound.midi.Track;
import javax.swing.JPanel;

import com.synthbot.audioplugin.vst.vst2.JVstHost2;

import uk.org.toot.control.CompoundControl;
import uk.org.toot.control.NativeSupport;
import uk.org.toot.swingui.miscui.VstEditButton;

public class VstNativeSupport implements NativeSupport
{
	private JVstHost2 vst;
	private CompoundControl controls;
	
	public VstNativeSupport(CompoundControl cc, JVstHost2 vst) {
		this.vst = vst;
		controls = cc;
	}
	
	public boolean canAddUI() {
		return true;
	}


	public void addUI(JPanel panel) {
		if ( vst.hasEditor() ) {
			String frameTitle = controls.getName()+" - Toot";
			vst.openEditor(frameTitle);
			panel.add(new VstEditButton(vst, frameTitle));
		}
	}

	public boolean canPersistMidi() {
		return true;
	}

	public void recall(Track t, int pos) {
		VstMidiPersistence.recall(controls, t, pos);
	}

	public void store(Track t) {
		VstMidiPersistence.store(controls, t);
	}

}
