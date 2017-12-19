package com.zach2039.sbmunpacker;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import net.lingala.zip4j.core.ZipFile;

public class SBMUnpacker extends Frame implements ActionListener {

	private static final long serialVersionUID = -6798840607857622024L;

	/*
	 * This program will take all .sbm files in the directory it is in and 
	 * convert them to unpacked folders with similar names.
	 * By Zach2039/GrilledSalmon/Sumyunguy, 12/17/2017. I have too many account names. Take your pick!
	 * 
	 * Used Tools:
	 * Zip4j: https://github.com/evosec/zip4j
	 * I don't own Zip4j, please don't sue thanks.
	 */
	public static void main(String[] args) {
		// Create instance and run.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				SBMUnpacker app = new SBMUnpacker();
				
				app.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent we) {
						System.out.println("Closing program...");
						app.dispose();
						System.exit(0);
					}
				});
			}
		});
	}
	
	// Data fields.
	private String currentMod = ""; // Current decompressing mod file.
	private int converted = 0; // How many mods have been converted.
	private int toconvert = 0; // How many mods at the start which are unconverted.
	private boolean running = false; // Set if the process to convert should be running.
	private boolean foundFiles = true; // Set if sbm files were found.
	
	// Objects.
	private Label lblProgress;
	private Label lblCurrentMod;
	private TextField tfProgress;
	private TextField tfCurrentMod;
	private Checkbox cbRemoveSBMs;
	private Checkbox cbOverwriteOld;
	private Button bBegin;
	
	// Constructor for the whole UI.
	public SBMUnpacker() {
		// Declare new UI layout.
		setLayout(new GridBagLayout());
		GridBagConstraints gridC = new GridBagConstraints();
		
		// Setup spacing.
		gridC.ipadx = 10;
		
		// Construct progress label and current mod label, and add to layout.
		gridC.fill = GridBagConstraints.HORIZONTAL;
		lblProgress = new Label("Progress");
		lblCurrentMod = new Label("Current Mod");
		gridC.gridx = 0;
		gridC.gridy = 0;
		add(lblProgress, gridC);
		gridC.gridx = 2;
		gridC.gridy = 0;
		add(lblCurrentMod, gridC);
		
		// Construct text fields for progress listing and current mod listing, and add to layout.
		gridC.fill = GridBagConstraints.HORIZONTAL;
		tfProgress = new TextField("0.0%", 5);
		tfCurrentMod = new TextField("N/A", 18);
		tfProgress.setEditable(false);
		tfCurrentMod.setEditable(false);
		gridC.gridx = 0;
		gridC.gridy = 1;
		add(tfProgress, gridC);
		gridC.gridx = 2;
		gridC.gridy = 1;
		add(tfCurrentMod, gridC);
		
		// Construct start button and option selection objects.
		gridC.fill = GridBagConstraints.HORIZONTAL;
		cbRemoveSBMs = new Checkbox("Remove original SBMs");
		cbOverwriteOld = new Checkbox("Overwrite old folders");
		bBegin = new Button("Start");
		cbRemoveSBMs.setState(false);
		cbOverwriteOld.setState(false);
		bBegin.addActionListener(this);
		gridC.gridx = 0;
		gridC.gridy = 2;
		add(cbRemoveSBMs, gridC);
		gridC.gridx = 0;
		gridC.gridy = 3;
		add(cbOverwriteOld, gridC);
		gridC.gridx = 2;
		gridC.gridy = 3;
		add(bBegin, gridC);
		
		// Set UI frame title and size.
		setTitle("SBM Decompress Tool - Zach2039");
		setSize(350, 150);
		
		// Show frame, prohibit resizing.
		setVisible(true);
		setResizable(false);
	}
	
	public void resetProgress() {
		converted = 0;
		toconvert = 0;
	}
	
	/*
	 * This method is the main processing task for the tool. It will:
	 * 1. Find SBMs (Or output an error if none are found).
	 * 2. Unpack SBMs (Or skip if we are not overwriting files).
	 * 3. Remove old SBMs (Or don't if we like our SBM files kicking around).
	 */
	public void processLogic() {
		// Reset previous run.
		resetProgress();
		
		// Get working directory.
		Path currentPath = Paths.get(".").toAbsolutePath().normalize();
		
		System.out.println(currentPath);
		
		// Collect SMB files for operation.
		List<File> sbmFiles = collectSBMFiles(currentPath);
		
		
		// If no smb files found, exit.
		if (sbmFiles.isEmpty()) {
			System.err.println("ERROR: No SBM files to convert.");
			running = false;
			foundFiles = false;
			resetProgress();
			return;
		} else {
			foundFiles = true;
			toconvert = sbmFiles.size();
		}
		
		// Convert files.
		for (File sbmFile : sbmFiles) {
			if (!running) {
				resetProgress();
				return;
			}
			String source = sbmFile.getPath();
			String destination = sbmFile.getAbsolutePath().replace(".sbm", "").concat("\\");
			System.out.println(source);
			System.out.println(destination);
			
			// Set display name for UI.
			currentMod = sbmFile.getName();
			
			// SBM to unpacked folder.
			try {
				if (new File(destination).exists() && !cbOverwriteOld.getState()) {
					// Do nothing, since we have the file and we don't want to overwrite stuff.
				} else {
					ZipFile zipFile = new ZipFile(source);
					zipFile.extractAll(destination);
				}
				converted++;
			} catch (net.lingala.zip4j.exception.ZipException e) {
				running = false;
				resetProgress();
				e.printStackTrace();
			}
			
			// Attempt to delete the old SBM if the checkbox for that option is set to do so.
			if (cbRemoveSBMs.getState()) {
				System.out.println("Removing: " + sbmFile.getName());
				sbmFile.delete();
			}
			updateUI();
		}
		
		// Finished.
		running = false;
	}
	
	/*
	 * This method gathers SBM files in the directory that this program is started from.
	 * It returns an arraylist of sbm files for manipulation.
	 */
	public static List<File> collectSBMFiles(Path currentPath) {
		File dir = new File(currentPath.toString());
		List<File> smbFiles = new ArrayList<File>();
		for (File file : dir.listFiles()) {
			if (file.getName().endsWith(".sbm")) {
				smbFiles.add(file);
			}
		}
		return smbFiles;
	}
	
	public void updateUI() {
		int progress_int = Math.round(((float)converted/(float)toconvert) * 100f);
		
		// Print some errors if stuff goes wrong, or don't if things work out.
		if (!foundFiles) {
			tfCurrentMod.setText("ERR: No SBM files found");
		} else {
			if (progress_int != 100) {
				// Print current mod id that is being decompressed.
				tfCurrentMod.setText(currentMod); 
			} else {
				// Print finished notification.
				tfCurrentMod.setText("Finished!");
			}
		}
		
		// Echo progress of current run.
		tfProgress.setText(progress_int + "%");
		
		// Change button text and state of checkboxes depending on whether the process is running or not.
		if (running) {
			
			cbRemoveSBMs.setEnabled(false);
			cbOverwriteOld.setEnabled(false);
			bBegin.setLabel("Stop");
		} else {
			cbRemoveSBMs.setEnabled(true);
			cbOverwriteOld.setEnabled(true);
			bBegin.setLabel("Start");
		}
	}

	/*
	 * This is the action listener for the "Start/Stop" button. It controls the state, updates
	 * the UI, and runs a separate thread for the processing part of the tool so the UI doesn't lock
	 * up.
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// Change start button text based on state.
		if (running) {
			running = false;
			updateUI();
		} else {
			running = true;
			
			// Start button action.
			Thread t = new Thread() {
				@Override
				public void run() {
					processLogic();
					updateUI();
				}
			};
			t.start();
		}
	}

}
