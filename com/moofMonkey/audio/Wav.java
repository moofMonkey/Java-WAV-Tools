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
	public byte[] data;
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
				DataInputStream inFile = new DataInputStream(new FileInputStream(filePath + ".wav"));
		) {
			chunkID = new String (
				getBytes (
					4,
					inFile
				)
			);

			chunkSize = NativeTranslate.bytes2int (
				getBytes (
					4,
					inFile
				)
			);

			format = new String (
				getBytes (
					4,
					inFile
				)
			);
			
			subChunk1ID = new String (
				getBytes (
					4,
					inFile
				)
			);

			subChunk1Size = NativeTranslate.bytes2int (
				getBytes (
					4,
					inFile
				)
			);
			audioFormat = NativeTranslate.bytes2short (
				getBytes (
					2,
					inFile
				)
			);
			numChannels = NativeTranslate.bytes2short (
				getBytes (
					2,
					inFile
				)
			);
			sampleRate = NativeTranslate.bytes2int (
				getBytes (
					4,
					inFile
				)
			);
			byteRate = NativeTranslate.bytes2int (
				getBytes (
					4,
					inFile
				)
			);
			blockAlign = NativeTranslate.bytes2short (
				getBytes (
					2,
					inFile
				)
			);
			bitsPerSample = NativeTranslate.bytes2short (
				getBytes (
					2,
					inFile
				)
			);
			
			specialData = "";
			while(!specialData.endsWith(subChunk2ID))
				specialData += (char) inFile.readByte();
			specialData = specialData.substring(0, specialData.length() - subChunk2ID.length());

			int subChunk2Size = NativeTranslate.bytes2int (
				getBytes (
					4,
					inFile
				)
			);
			
			data = new byte[(int) subChunk2Size];
			inFile.read(data);
			dataAtEnd = new byte[inFile.available()];
			inFile.readFully(dataAtEnd);
		} catch(Throwable t) {
			t.printStackTrace();
			return false;
		}

		return true;
	}
	
	public static byte[] getBytes(int num, DataInputStream in) throws Throwable {
		byte[] bytes = new byte[num];
		in.readFully(bytes);
		
		return bytes;
	}

	public boolean save() {
		try (
				DataOutputStream outFile = new DataOutputStream(new FileOutputStream(filePath + "modify.wav"));
		) {
			outFile.writeBytes(chunkID); //RIFF
			outFile.write (
				NativeTranslate.int2bytes (
					chunkSize
				)
			);
			outFile.writeBytes(format); //WAVE
			outFile.writeBytes(subChunk1ID); //fmt*
			outFile.write (
				NativeTranslate.int2bytes (
					subChunk1Size
				)
			);
			outFile.write (
				NativeTranslate.short2bytes (
					audioFormat
				)
			);
			outFile.write (
				NativeTranslate.short2bytes (
					numChannels
				)
			);
			outFile.write (
				NativeTranslate.int2bytes (
					sampleRate
				)
			);
			outFile.write (
				NativeTranslate.int2bytes (
					byteRate
				)
			);
			outFile.write (
				NativeTranslate.short2bytes (
					blockAlign
				)
			);
			outFile.write (
				NativeTranslate.short2bytes (
					bitsPerSample
				)
			);
			outFile.writeBytes(specialData);
			outFile.writeBytes(subChunk2ID); //data
			outFile.write (
				NativeTranslate.int2bytes (
					data.length
				)
			);
			outFile.write(data);
			outFile.write(dataAtEnd);
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
				+ data.length + newline + "Data at end: " + new String(dataAtEnd);
		
		return summary;
	}

	public byte[] getBytes() throws Throwable {
		return data;
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
			byte[] audioData = data;
			
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
