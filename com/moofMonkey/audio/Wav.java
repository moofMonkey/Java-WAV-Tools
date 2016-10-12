package com.moofMonkey.audio;


import java.applet.Applet;
import java.applet.AudioClip;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import com.moofMonkey.utils.IOUtils;

/**
 * About WAV [RUS]: http://audiocoding.ru/article/2008/05/22/wav-file-structure.html
 * @author moofMonkey
 */
public class Wav {
	ByteArrayOutputStream byteArrayOutputStream;
	TargetDataLine targetDataLine;
	AudioInputStream audioInputStream;
	SourceDataLine sourceDataLine;
	float speed = 1F;
	private String filePath;
	/**
	 * Must be "RIFF"
	 */
	private String chunkID;
	private int chunkSize = 0;
	/**
	 * Must be "WAVE"
	 */
	private String format;
	/**
	 * Must be "fmt*"
	 */
	private String subChunk1ID;
	private int subChunk1Size = 0;
	private short audioFormat = 0;
	private short numChannels = 0;;
	private int sampleRate = 0;
	private int byteRate = 0;
	private short blockAlign = 0;
	private short bitsPerSample = 0;
	private String specialData = "";
	private String subChunk2ID = "data";
	public byte[] soundData;
	public byte[] dataAtEnd;

	public Wav() {
		filePath = "";
	}

	public Wav(String _filePath) {
		filePath = _filePath;
	}

	public String getPath() {
		return filePath;
	}

	public void setPath(String newPath) {
		filePath = newPath;
	}

	public boolean read() throws Throwable {
		try (
				IOUtils io = new IOUtils(new DataInputStream(new FileInputStream(filePath + ".wav")));
		) {
			chunkID = io.readString(); //RIFF
			chunkSize = io.readInt(); //Must be filesize-8
			format = io.readString(); //WAVE
			subChunk1ID = io.readString(); //fmt*

			subChunk1Size = io.readInt();
			audioFormat = io.readShort();
			numChannels = io.readShort();
			sampleRate = io.readInt();
			byteRate = io.readInt();
			blockAlign = io.readShort();
			bitsPerSample = io.readShort();
			
			specialData = "";
			while(!specialData.endsWith(subChunk2ID))
				specialData += (char) io.getIn().readByte();
			specialData = specialData.substring(0, specialData.length() - subChunk2ID.length());

			int subChunk2Size = io.readInt();
			soundData = new byte[(int) subChunk2Size];
			io.getIn().readFully(soundData);
			
			dataAtEnd = new byte[io.getIn().available()];
			io.getIn().readFully(dataAtEnd);
		} catch(Throwable t) {
			t.printStackTrace();
			return false;
		}

		return true;
	}

	public boolean save() {
		try (
				IOUtils io = new IOUtils(new DataOutputStream(new FileOutputStream(filePath + "modify.wav")));
		) {
			io.writeString(chunkID); //RIFF
			io.writeInt(chunkSize);
			io.writeString(format); //WAVE
			io.writeString(subChunk1ID); //fmt*
			
			io.writeInt(subChunk1Size);
			io.writeShort(audioFormat);
			io.writeShort(numChannels);
			io.writeInt(sampleRate);
			io.writeInt(byteRate);
			io.writeShort(blockAlign);
			io.writeShort(bitsPerSample);
			io.writeString(specialData);
			io.writeString(subChunk2ID); //data
			io.writeInt(soundData.length);
			io.getOut().write(soundData);
			//outFile.write(dataAtEnd);
		} catch(Throwable t) {
			t.printStackTrace();
			return false;
		}

		return true;
	}

	public String getSummary() {
		String newline = System.getProperty("line.separator");
		String summary = "Format: " + audioFormat + newline + "Channels: " + numChannels
				+ newline + "Sample rate: " + sampleRate + newline + "Byte rate: "
				+ byteRate + newline + "BlockAlign: " + blockAlign + newline
				+ "Bits/1 Sample: " + bitsPerSample + newline + "Data size: "
				+ soundData.length + newline + "Data at end: " + new String(dataAtEnd);
		
		return summary;
	}

	public byte[] getBytes() throws Throwable {
		return soundData;
	}

	private AudioFormat getAudioFormat() {
		boolean signed = true;
		boolean bigEndian = false;

		return new AudioFormat(sampleRate * speed, bitsPerSample, numChannels, signed, bigEndian);
	}

	public void playWav(String filePath) {
		try {
			AudioClip clip = (AudioClip) Applet.newAudioClip(new File(filePath).toURI().toURL());
			clip.play();
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}
	
	/**
	 * Plays back audio stored in the byte array using an audio format given by
	 * freq, sample rate, etc.
	 */
	public void playAudio() {
		try {
			byte[] audioData = soundData;
			
			InputStream byteArrayInputStream = new ByteArrayInputStream(audioData);
			AudioFormat audioFormat = getAudioFormat();
			audioInputStream = new AudioInputStream(
				byteArrayInputStream,
				audioFormat,
				audioData.length / audioFormat.getFrameSize());
			DataLine.Info dataLineInfo = new DataLine.Info(
				SourceDataLine.class,
				audioFormat);
			sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
			sourceDataLine.open(audioFormat);
			sourceDataLine.start();

			new PlayThread().start();
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}

	/**
	 * Inner class to play back the data that was saved
	 */
	private class PlayThread extends Thread {
		byte[] tempBuffer = new byte[10000];

		public void run() {
			try {
				int cnt;
				while ((cnt = audioInputStream.read(tempBuffer, 0, tempBuffer.length)) != -1)
					if (cnt > 0)
						sourceDataLine.write(tempBuffer, 0, cnt); // Write data to the internal buffer of the data line where it will be delivered to the speaker.
				sourceDataLine.drain();
				sourceDataLine.close();
			} catch(Exception e) {
				System.out.println(e);
				System.exit(0);
			}
		}
	}
}
