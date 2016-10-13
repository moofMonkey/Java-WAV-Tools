package com.moofMonkey;


import java.io.File;

import it.sauronsoftware.jave.AudioAttributes;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncodingAttributes;

public class Main {
	public static void main(String[] args) throws Throwable {
		wavToMP3("C:/Users/recston/Music/DotA 2 Песня про - Techiesmodify.wav", "C:/Users/recston/Music/DotA 2 Песня про - Techiesmodify.mp3");
	}

	public static void wavToMP3(String sourceFile, String targetFile) throws Throwable {
		int samplingRate = 16000;// this could be 8000, 16000 mono or 16000
									// stereo
		int channels = 2;// this could be 1 for mono and 2 for stereo
		int bitRate = 180 * 1000;// this could be 128, 160, 190 kbps, etc..

		AudioAttributes audio = new AudioAttributes();
		audio.setCodec("libmp3lame");
		audio.setBitRate(bitRate);
		audio.setChannels(channels);
		audio.setSamplingRate(samplingRate);
		EncodingAttributes ea = new EncodingAttributes();
		ea.setAudioAttributes(audio);
		ea.setFormat("mp3");
		File f = new File(sourceFile);
		Encoder e = new Encoder();

		e.encode(f, new File(targetFile), ea);
	}
}
