package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.base.Globals.DialogType;
import org.helioviewer.jhv.base.Settings;
import org.helioviewer.jhv.base.Settings.StringKey;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.PredefinedFileFilter;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.jp2view.newjpx.KakaduLayer;
import org.helioviewer.jhv.viewmodel.metadata.UnsuitableMetaDataException;

/**
 * Action to open a local file.
 * 
 * <p>
 * Opens a file chooser dialog, opens the selected file. Currently supports the
 * following file extensions: "jpg", "jpeg", "png", "fts", "fits", "jp2" and
 * "jpx"
 */
public class OpenLocalFileAction extends AbstractAction
{
	public OpenLocalFileAction()
	{
		super("Open...");
		putValue(SHORT_DESCRIPTION, "Open image");
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit
				.getDefaultToolkit().getMenuShortcutKeyMask()));
	}

	public void actionPerformed(@Nullable ActionEvent e)
	{
		//TODO: add png, jpg & fits support
		File selectedFile = Globals.showFileDialog(
				DialogType.OPEN_FILE,
				"Open local file",
				Settings.getString(StringKey.MOVIE_OPEN_PATH),
				false,
				null,
				//PredefinedFileFilter.ALL_SUPPORTED_IMAGE_TYPES,
				PredefinedFileFilter.JP2/*,
				PredefinedFileFilter.FITS,
				PredefinedFileFilter.PNG_SINGLE,
				PredefinedFileFilter.JPG_SINGLE*/);

		if (selectedFile == null)
			return;
		
		// remember the current directory for future
		Settings.setString(StringKey.MOVIE_OPEN_PATH, selectedFile.getParent());
		
		try
		{
			KakaduLayer newLayer=new KakaduLayer(selectedFile.getPath());
			Layers.addLayer(newLayer);
			
			newLayer.animateCameraToFacePlane=true;
		}
		catch(UnsuitableMetaDataException _umde)
		{
			JOptionPane.showMessageDialog(MainFrame.SINGLETON.MAIN_PANEL, "The source's metadata could not be read.");
		}
		catch (IOException _e)
		{
			JOptionPane.showMessageDialog(MainFrame.SINGLETON.MAIN_PANEL, "The file could not be read:\n"+_e.getMessage());
		}
	}
}
