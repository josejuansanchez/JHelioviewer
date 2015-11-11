package org.helioviewer.jhv.gui.actions;

import com.google.common.io.Files;
import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.base.Globals.DialogType;
import org.helioviewer.jhv.base.Settings;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.PredefinedFileFilter;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SaveScreenshotAsAction extends AbstractAction
{
	private static final String SETTING_SCREENSHOT_IMG_WIDTH = "export.screenshot.image.width";
	private static final String SETTING_SCREENSHOT_IMG_HEIGHT = "export.screenshot.image.height";
	private static final String SETTING_SCREENSHOT_TEXT = "export.screenshot.text";
	private static final String SETTING_SCREENSHOT_EXPORT_LAST_DIRECTORY = "export.screenshot.last.directory";

	private int imageWidth;
	private int imageHeight;

	private boolean textEnabled;

	public SaveScreenshotAsAction()
	{
		super("Save screenshot as...");
		putValue(SHORT_DESCRIPTION, "Save screenshots to a file");
		putValue(
				ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.SHIFT_DOWN_MASK
						| Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	}

	public void actionPerformed(@Nullable ActionEvent e)
	{
		File selectedFile=Globals.showFileDialog(DialogType.SAVE_FILE,
				"Save screenshot",
				Settings.getString(SETTING_SCREENSHOT_EXPORT_LAST_DIRECTORY),
				false,
				getDefaultFileName(),
				PredefinedFileFilter.PNG_SINGLE,
				PredefinedFileFilter.JPG_SINGLE);
			
		if(selectedFile==null)
			return;

		Telemetry.trackEvent("Export screenshot");
		Telemetry.trackMetric("ScreenshotWidth",imageWidth);
		Telemetry.trackMetric("ScreenshotHeight",imageHeight);
		Telemetry.trackMetric("ScreenshotTextEnabled", textEnabled?1:0);

		loadSettings();
		try
		{
			ImageIO.write(MainFrame.SINGLETON.MAIN_PANEL.getBufferedImage(
					imageWidth, imageHeight, textEnabled),
					Files.getFileExtension(selectedFile.getPath()), selectedFile);
		}
		catch (IOException e1)
		{
			Telemetry.trackException(e1);
		}
	}

	private void loadSettings()
	{
		String val;
		try
		{
			val = Settings.getString(SETTING_SCREENSHOT_TEXT);
			if (val != null && !(val.length() == 0))
				this.textEnabled = Boolean.parseBoolean(val);
		}
		catch (Throwable t)
		{
			Telemetry.trackException(t);
		}

		try
		{
			val = Settings.getString(SETTING_SCREENSHOT_IMG_HEIGHT);
			if (val != null && !(val.length() == 0))
				this.imageHeight = Integer.parseInt(val);
		}
		catch (Throwable t)
		{
			Telemetry.trackException(t);
		}

		try
		{
			val = Settings.getString(SETTING_SCREENSHOT_IMG_WIDTH);
			if (val != null && !(val.length() == 0))
				this.imageWidth = Integer.parseInt(val);
		}
		catch (Throwable t)
		{
			Telemetry.trackException(t);
		}

		if (imageWidth == 0)
			imageWidth = 1280;

		if (imageHeight == 0)
			imageHeight = 720;
	}

	/**
	 * Returns the default name for a screenshot. The name consists of
	 * "JHV_screenshot_created" plus the current system date and time.
	 * 
	 * @return Default name for a screenshot.
	 */
	private String getDefaultFileName()
	{
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
		return "JHV_screenshot_created_"+dateFormat.format(new Date());
	}
}
