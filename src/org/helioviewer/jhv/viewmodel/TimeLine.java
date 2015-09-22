package org.helioviewer.jhv.viewmodel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.swing.Timer;

import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.leftPanel.MoviePanel.AnimationMode;
import org.helioviewer.jhv.layers.AbstractImageLayer;
import org.helioviewer.jhv.layers.AbstractLayer;
import org.helioviewer.jhv.layers.LayerListener;
import org.helioviewer.jhv.layers.Layers;

public class TimeLine implements LayerListener
{
	private LocalDateTime current = LocalDateTime.now();

	private boolean isPlaying = false;

	private ArrayList<TimeLineListener> timeLineListeners;

	private NavigableSet<LocalDateTime> localDateTimes = null;

	private int millisecondsPerFrame = 50;
	private AnimationMode animationMode = AnimationMode.LOOP;

	public static TimeLine SINGLETON = new TimeLine();
	private boolean forward = true;
	
	private static final Timer timer = new Timer(0, new ActionListener()
	{
		@Override
		public void actionPerformed(ActionEvent e)
		{
			MainFrame.MAIN_PANEL.display();
			timer.stop();
		}
	});
	
	private TimeLine()
	{
		localDateTimes = new ConcurrentSkipListSet<LocalDateTime>();
		Layers.addNewLayerListener(this);
		timeLineListeners = new ArrayList<TimeLine.TimeLineListener>();
	}

	public boolean isPlaying()
	{
		return isPlaying;
	}

	public void setPlaying(boolean _playing)
	{
		if(localDateTimes.isEmpty() && _playing)
		{
			System.out.println("TimeLine: There's nothing to play");
			_playing=false;
		}
		
		this.isPlaying = _playing;
		forward = true;
		MainFrame.MAIN_PANEL.resetLastFrameChangeTime();
		MainFrame.MAIN_PANEL.repaint();
	}

	public LocalDateTime nextFrame()
	{
		if (localDateTimes.isEmpty())
			return null;
		
		LocalDateTime next = localDateTimes.higher(current);
		LocalDateTime last = current;
		if (next == null)
			next = localDateTimes.first();
		
		current = next;
		dateTimeChanged(last);
		return current;
	}

	public LocalDateTime previousFrame()
	{
		if (localDateTimes.isEmpty())
			return null;
		LocalDateTime next = localDateTimes.lower(current);
		if (next == null)
			next = localDateTimes.last();
		
		current = next;
		dateTimeChanged(current);
		return current;
	}

	@Deprecated
	public int getFrameCount() {
		return localDateTimes.size();
	}

	@Deprecated
	public int getCurrentFrame() {
		return localDateTimes.headSet(current).size();
	}

	public LocalDateTime getCurrentDateTime() {
		return current;
	}

	public void addListener(TimeLineListener timeLineListener) {
		timeLineListeners.add(timeLineListener);
	}

	public void removeListener(TimeLineListener timeLineListener) {
		timeLineListeners.remove(timeLineListener);
	}

	private void dateTimeChanged(LocalDateTime last) {
		for (TimeLine.TimeLineListener timeLineListener : timeLineListeners) {
			timeLineListener.timeStampChanged(current, last);
		}
	}

	private void notifyUpdateDateTimes()
	{
		MainFrame.MOVIE_PANEL.setButtonsEnabled(!localDateTimes.isEmpty());
		for (TimeLine.TimeLineListener timeLineListener : timeLineListeners)
		{
			timeLineListener.dateTimesChanged(localDateTimes.size());
		}
	}

	public void setFPS(int fps)
	{
		this.millisecondsPerFrame = Math.round(1000f / fps);
	}

	public int getMillisecondsPerFrame()
	{
		return millisecondsPerFrame;
	}

	@Override
	public void newLayerAdded()
	{
	}

	@Override
	public void newlayerRemoved(int idx)
	{
		if (Layers.getActiveImageLayer() == null)
			localDateTimes = null;
	}

	@Override
	public void activeLayerChanged(AbstractLayer layer)
	{
		if (layer != null && layer.isImageLayer())
		{
			setLocalDateTimes(((AbstractImageLayer)layer).getLocalDateTime());
			MainFrame.MOVIE_PANEL.setButtonsEnabled(!localDateTimes.isEmpty());
		}
	}

	public void setCurrentFrame(int value)
	{
		Iterator<LocalDateTime> it = localDateTimes.iterator();
		int i = 0;
		LocalDateTime current = null;
		while (it.hasNext() && i <= value)
		{
			current = it.next();
			i++;
		}
		if (current != null && !current.isEqual(this.current))
		{
			LocalDateTime last = this.current;
			this.current = current;
			dateTimeChanged(last);
		}
		MainFrame.MAIN_PANEL.repaint();
	}

	public interface TimeLineListener
	{
		void timeStampChanged(LocalDateTime current, LocalDateTime last);

		@Deprecated
		void dateTimesChanged(int framecount);
	}

	public void setLocalDateTimes(NavigableSet<LocalDateTime> localDateTimes)
	{
		this.localDateTimes = localDateTimes;
		notifyUpdateDateTimes();
	}

	public LocalDateTime getFirstDateTime()
	{
		if (localDateTimes == null || localDateTimes.isEmpty())
			return null;
		return localDateTimes.first();
	}

	public LocalDateTime getLastDateTime()
	{
		if (localDateTimes == null || localDateTimes.isEmpty())
			return null;
		return localDateTimes.last();
	}

	public void setCurrentDate(LocalDateTime currentDateTime)
	{
		LocalDateTime last = current;
		this.current = currentDateTime;
		dateTimeChanged(last);
	}

	public void setAnimationMode(AnimationMode animationMode)
	{
		this.animationMode = animationMode;
	}
	
	/**
	 * 
	 * 
	 * @param _elapsedMilliseconds
	 * @return Returns true iff the current frame changed
	 */
	public boolean processElapsedAnimationTime(long _elapsedMilliseconds)
	{
		if (_elapsedMilliseconds <= 0)
			return true;
		
		int elapsedFrames = (int)_elapsedMilliseconds / millisecondsPerFrame;
		if (elapsedFrames <= 0)
		{
			timer.stop();
			timer.setDelay((int) (millisecondsPerFrame - _elapsedMilliseconds));
			timer.start();
			return false;
		}
		
		while (elapsedFrames > 0)
		{
			elapsedFrames--;
			switch (animationMode)
			{
				case LOOP:
					loop();
					break;
				case STOP:
					stop();
					break;
				case SWING:
					swing();
					break;
				default:
					break;
			}
			if (!isPlaying)
			{
				dateTimeChanged(current);
				return true;
			}
		}
		dateTimeChanged(current);
		return true;
	}
	
	private void loop()
	{
		current = localDateTimes.higher(current);
		if (current == null)
			current = localDateTimes.first();
	}
	
	private void stop()
	{
		current = localDateTimes.lower(current);
		if (current == null)
		{
			current = localDateTimes.first();		
			MainFrame.MOVIE_PANEL.setPlaying(false);
		}
	}
	
	private void swing()
	{
		LocalDateTime next;
		if (forward)
		{
			next = localDateTimes.higher(current);
			if (next == null)
			{
				forward = false;
				next = localDateTimes.lower(current);
			}
		}
		else
		{
			next = localDateTimes.lower(current);
			if (next == null)
			{
				forward = true;
				next = localDateTimes.higher(current);
			}
		}
		current = next;
	}
}