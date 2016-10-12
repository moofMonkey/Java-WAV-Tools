package com.moofMonkey.audio;


import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import com.moofMonkey.utils.IOUtils;

/**
 * About WAV [RUS]: http://audiocoding.ru/article/2008/05/22/wav-file-structure.html
 * @author moofMonkey
 */
public class Wav {
	float speed = 1F;
	private String filePath;
	/**
	 * Must be "RIFF"
	 */
	private String chunkID;
	public int chunkSize = 0;
	/**
	 * Must be "WAVE"
	 */
	private String format;
	/**
	 * Must be "fmt*"
	 */
	private String subChunk1ID;
	public int subChunk1Size = 0;
	public short audioFormat = 0;
	public short numChannels = 0;;
	public int sampleRate = 0;
	public int byteRate = 0;
	public short blockAlign = 0;
	public short bitsPerSample = 0;
	public String specialData = "";
	private String subChunk2ID = "data";
	public byte[] audioData;
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
			audioData = new byte[(int) subChunk2Size];
			io.getIn().readFully(audioData);
			
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
			io.writeInt(audioData.length);
			io.getOut().write(audioData);
			//outFile.write(dataAtEnd);
		} catch(Throwable t) {
			t.printStackTrace();
			return false;
		}

		return true;
	}
	
	public String getDuration() {
		float duration = speed * audioData.length / (bitsPerSample / Byte.SIZE) / numChannels / sampleRate;
		int durationSec = (int) (duration % 60);
		int durationMin = (int) (duration / 60);
		int durationHour = (int) (duration - (durationMin * 60 + durationSec));

		String strDurationMilliSec = Integer.toString (
			(int) (
				(duration % 1)
				* 1000
			)
		);
		String strDurationSec = Integer.toString(durationSec);
		String strDurationMin = Integer.toString(durationMin);
		String strDurationHour = Integer.toString(durationHour);

		while(strDurationMilliSec.length() < 4)
			strDurationMilliSec = "0" + strDurationMilliSec;
		while(strDurationSec.length() < 2)
			strDurationSec = "0" + strDurationSec;
		while(strDurationMin.length() < 2)
			strDurationMin = "0" + strDurationMin;
		while(strDurationHour.length() < 2)
			strDurationHour = "0" + strDurationHour;
		
		return strDurationHour + ":" + strDurationMin + ":" + strDurationSec + ":" + strDurationMilliSec;
	}

	public String getSummary() {
		String newline = System.getProperty("line.separator");
		String summary =
				"Format: " + audioFormat + newline +
				"Channels: " + numChannels + newline +
				"Sample rate: " + sampleRate + newline +
				"Byte rate: " + byteRate + newline +
				"BlockAlign: " + blockAlign + newline +
				"Bits/1 Sample: " + bitsPerSample + newline +
				"Data size: " + audioData.length + newline +
				"Data at end: " + new String(dataAtEnd) + newline +
				"Duration: " + getDuration();
		
		return summary;
	}

	public byte[] getBytes() throws Throwable {
		return audioData;
	}

	private AudioFormat getAudioFormat() {
		boolean signed = true;
		boolean bigEndian = false;

		return new AudioFormat(sampleRate * speed, bitsPerSample, numChannels, signed, bigEndian);
	}
	
	/**
	 * Plays back audio stored in the byte array using an audio format given by
	 * freq, sample rate, etc.
	 */
	@SuppressWarnings( "resource" )
	public void playAudio() {
		try {
			new PlayThread(audioData).start();
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}

	public static PlayThread currentPlaying;
	/**
	 * Inner class to play back the data that was saved
	 */
	private class PlayThread extends Thread implements Closeable {
		AudioInputStream audioInputStream;
		SourceDataLine sourceDataLine;
		byte[] tempBuffer = new byte[10000];
		
		public PlayThread(byte[] audioData) throws Throwable {
			if(currentPlaying != null)
				currentPlaying.close();
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
			currentPlaying = this;
		}

		public void run() {
			try {
				int cnt;
				while ((cnt = audioInputStream.read(tempBuffer, 0, tempBuffer.length)) != -1)
					if (cnt > 0)
						sourceDataLine.write(tempBuffer, 0, cnt); // Write data to the internal buffer of the data line where it will be delivered to the speaker.
				sourceDataLine.drain();
				close();
			} catch(Throwable t) {
				t.printStackTrace();
			}
		}

		@Override
		public void close() throws IOException {
			currentPlaying = null;
			interrupt();
			sourceDataLine.close();
		}
	}
}
