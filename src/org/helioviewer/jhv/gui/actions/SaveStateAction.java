package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.StateParser;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.actions.filefilters.ExtensionFileFilter;
import org.helioviewer.jhv.gui.actions.filefilters.JHVStateFilter;
import org.json.JSONException;

public class SaveStateAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public SaveStateAction() {
        super("Save state...");
        putValue(SHORT_DESCRIPTION, "Saves the current state of JHV");
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
    	try {
			StateParser.writeStateFile();
		} catch (JSONException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			JOptionPane.showMessageDialog(MainFrame.MAIN_PANEL, "No file founded \n" + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
        
    }

    /**
     * Write the given xmlData to the given file handle
     * 
     * @param selectedFile
     *            - 'File' object to write to
     * @param xmlData
     *            - the data to write to disk
     * @return true if the operation was successful, false if an error occured
     */
    private boolean writeXML(File selectedFile, String xmlData)
    {
        try(Writer xmlWriter = new OutputStreamWriter(new FileOutputStream(selectedFile),StandardCharsets.UTF_8))
        {
            xmlWriter.write(xmlData);
            return true;
        }
        catch (Exception _e)
        {
        	return false;
        }
    }

    /**
     * Prompt the user to choose a filename and return a 'File' object pointing
     * to the selected location. In case the user selects an already existing
     * filename, the user is asked if he really wants to overwrite the file.
     * <p>
     * If any errors occur, null is returned.
     * 
     * @return a 'File' object pointing to the selected location, null if an
     *         error occured
     */
    private File chooseFile() {
        JFileChooser fileChooser = JHVGlobals.getJFileChooser();
        fileChooser.setFileHidingEnabled(false);
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addChoosableFileFilter(new JHVStateFilter());

        String txtTargetFile = getDefaultFileName();
        fileChooser.setSelectedFile(new File(txtTargetFile));

        int retVal = fileChooser.showSaveDialog(MainFrame.SINGLETON);
        File selectedFile = null;

        if (retVal == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();

            // Has user entered the correct extension or not?
            ExtensionFileFilter fileFilter = (ExtensionFileFilter) fileChooser.getFileFilter();

            if (!fileFilter.accept(selectedFile)) {
                selectedFile = new File(selectedFile.getPath() + "." + fileFilter.getDefaultExtension());
            }

            // does the file already exist?
            if (selectedFile.exists()) {

                // ask if the user wants to overwrite
                int response = JOptionPane.showConfirmDialog(null, "Overwrite existing file?", "Confirm Overwrite", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

                // if the user doesn't want to overwrite, simply return null
                if (response == JOptionPane.CANCEL_OPTION) {
                    return null;
                }
            }
        }

        return selectedFile;
    }

    /**
     * Returns the default name for a state. The name consists of
     * "JHV_state_saved" plus the current system date and time.
     * 
     * @return Default name for a screenshot.
     */
    private static String getDefaultFileName() {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

        String output = new String("JHV_state_saved_");
        output += dateFormat.format(new Date());

        return output;
    }

}