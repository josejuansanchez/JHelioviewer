package org.helioviewer.jhv.gui.leftPanel;

import java.awt.Toolkit;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.base.MultiClickListener;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.components.WheelSupport;
import org.helioviewer.jhv.layers.AbstractImageLayer;
import org.helioviewer.jhv.layers.AbstractLayer;
import org.helioviewer.jhv.layers.LayerListener;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.filter.LUT.LUT_ENTRY;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class FilterPanel extends JPanel implements LayerListener
{
	private static final long serialVersionUID = 6032053412521027833L;
	private JSlider opacitySlider;
	private JSlider sharpenSlider;
	private JSlider gammaSlider;
	private JSlider contrastSlider;
	private JComboBox<LUT_ENTRY> comboBoxColorTable;
	private JCheckBox chckbxRed;
	private JCheckBox chckbxGreen;
	private JCheckBox chckbxBlue;
	private JToggleButton btnInverseColorTable;
	private JLabel lblOpacity, lblSharpen, lblGamma, lblContrast;
	private AbstractImageLayer activeLayer;
	private static final double GAMMA_FACTOR = 0.01 * Math.log(10);
	
    private static final Icon ICON_INVERT = IconBank.getIcon(JHVIcon.INVERT, 16, 16);

	public FilterPanel() {
		initGui();
		Layers.addNewLayerListener(this);
	}
	
	private void initGui(){
        Object clickInterval = Toolkit.getDefaultToolkit().
                getDesktopProperty("awt.multiClickInterval");
        int delay = clickInterval != null ? (int)clickInterval : 200;

		FormLayout formLayout = new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("right:default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("right:default"),
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,});
		formLayout.setColumnGroups(new int[][]{new int[]{8, 4, 6}});
		setLayout(formLayout);
		
		JLabel lblOpacityTitle = new JLabel("Opacity:");
		add(lblOpacityTitle, "2, 2");
		
		opacitySlider = new JSlider();
		opacitySlider.setMinorTickSpacing(20);
		opacitySlider.setPaintTicks(true);
		add(opacitySlider, "4,2,5,1");
		WheelSupport.installMouseWheelSupport(opacitySlider);
		lblOpacity = new JLabel("%");
		add(lblOpacity, "10, 2");

		
		opacitySlider.addChangeListener(new ChangeListener() {			
			@Override
			public void stateChanged(ChangeEvent e) {
				lblOpacity.setText(opacitySlider.getValue() + "%");
				if (activeLayer != null && activeLayer.opacity != opacitySlider.getValue() / 100.0){
					activeLayer.opacity = opacitySlider.getValue() / 100.0;
					repaintComponent();
				}
			}
		});
		opacitySlider.addMouseListener(new MultiClickListener(delay){
			@Override
			public void doubleClick(MouseEvent e) {
				opacitySlider.setValue(100);
			}
		});
		
		JLabel lblSharpenTitle = new JLabel("Sharpen");
		add(lblSharpenTitle, "2, 4");

		sharpenSlider = new JSlider();
		sharpenSlider.setMinorTickSpacing(20);
		sharpenSlider.setPaintTicks(true);
		add(sharpenSlider, "4, 4, 5, 1");
		WheelSupport.installMouseWheelSupport(sharpenSlider);
		lblSharpen = new JLabel("%");
		add(lblSharpen, "10, 4");
		sharpenSlider.addChangeListener(new ChangeListener() {			
			@Override
			public void stateChanged(ChangeEvent e) {
				lblSharpen.setText(sharpenSlider.getValue() + "%");
				if (activeLayer != null && activeLayer.sharpness != sharpenSlider.getValue()/100.0){
					activeLayer.sharpness = sharpenSlider.getValue()/100.0;
					repaintComponent();
				}
			}
		});
		sharpenSlider.addMouseListener(new MultiClickListener(delay){
			@Override
			public void doubleClick(MouseEvent e) {
				sharpenSlider.setValue(0);
			}
		});
		
		JLabel lblGammaTitle = new JLabel("Gamma");
		add(lblGammaTitle, "2, 6");
		
		gammaSlider = new JSlider(JSlider.HORIZONTAL, -100, 100, 0);
		gammaSlider.setMinorTickSpacing(20);
		gammaSlider.setPaintTicks(true);
		gammaSlider.setValue(10);
		add(gammaSlider, "4, 6, 5, 1");
		
		lblGamma = new JLabel("1.0");
		add(lblGamma, "10, 6");
		WheelSupport.installMouseWheelSupport(gammaSlider);
		gammaSlider.addChangeListener(new ChangeListener() {			
			@Override
			public void stateChanged(ChangeEvent e) {
				double gammaValue = Math.exp(gammaSlider.getValue() * GAMMA_FACTOR);
		        String label = Double.toString(Math.round(gammaValue * 10.0) * 0.1);
		        if (gammaSlider.getValue() == 100) {
		            label = label.substring(0, 4);
		        } else {
		            label = label.substring(0, 3);
		        }
				lblGamma.setText(label);
				if (activeLayer != null && activeLayer.gamma != gammaValue){
					activeLayer.gamma = gammaValue;
					repaintComponent();
				}
			}
		});
		gammaSlider.addMouseListener(new MultiClickListener(delay){
			@Override
			public void doubleClick(MouseEvent e) {
				gammaSlider.setValue((int)(Math.log(1) / GAMMA_FACTOR));
			}
		});
		
		JLabel lblContrastTitle = new JLabel("Contrast");
		add(lblContrastTitle, "2, 8");
		
		contrastSlider = new JSlider();
		contrastSlider.setMinorTickSpacing(20);
		contrastSlider.setPaintTicks(true);
		contrastSlider.setMaximum(100);
		contrastSlider.setMinimum(-100);
		contrastSlider.setValue(0);
		add(contrastSlider, "4, 8, 5, 1");
		WheelSupport.installMouseWheelSupport(contrastSlider);
		
		lblContrast = new JLabel("0");
		add(lblContrast, "10,8");

		contrastSlider.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
				lblContrast.setText(contrastSlider.getValue() + "");
				if (activeLayer != null &&  activeLayer.contrast != contrastSlider.getValue() / 10.0)
				{
					activeLayer.contrast = contrastSlider.getValue() / 10.0;
					repaintComponent();
				}
			}
		});
		contrastSlider.addMouseListener(new MultiClickListener(delay)
		{
			@Override
			public void doubleClick(MouseEvent e)
			{
				contrastSlider.setValue(0);
			}
		});
		
		JLabel lblColorTitle = new JLabel("Color:");
		add(lblColorTitle, "2, 10, right, default");
		
        /*Map<String, LUT> lutMap = LUT.getStandardList();
        lutMap.put("<Load new GIMP gradient file>", null);*/
        //comboBoxColorTable = new JComboBox<String>();
        //comboBoxColorTable = new JComboBox<String>(LUT.getNames());
        comboBoxColorTable = new JComboBox<LUT_ENTRY>(LUT_ENTRY.values());
        comboBoxColorTable.setSelectedItem(LUT_ENTRY.GRAY);
		add(comboBoxColorTable, "4, 10, 5, 1, fill, default");
		comboBoxColorTable.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (activeLayer != null && activeLayer.getLut() != comboBoxColorTable.getSelectedItem()){
					activeLayer.setLut((LUT_ENTRY) comboBoxColorTable.getSelectedItem());
					repaintComponent();
				}
			}
		});
		
		btnInverseColorTable = new JToggleButton(ICON_INVERT);
		add(btnInverseColorTable, "10, 10");
		btnInverseColorTable.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				if(activeLayer != null && activeLayer.invertedLut != btnInverseColorTable.isSelected()){
					activeLayer.invertedLut=btnInverseColorTable.isSelected();
					repaintComponent();
				}
			}
		});
		
		JLabel lblChannelsTitle = new JLabel("Channels");
		add(lblChannelsTitle, "2, 12");
		
		chckbxRed = new JCheckBox("Red");
		add(chckbxRed, "4, 12");
		chckbxRed.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (activeLayer != null && activeLayer.redChannel != chckbxRed.isSelected()){
					activeLayer.redChannel=chckbxRed.isSelected();
					repaintComponent();
				}
			}
		});
		
		chckbxGreen = new JCheckBox("Green");
		add(chckbxGreen, "6, 12");
		chckbxGreen.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (activeLayer != null && activeLayer.greenChannel != chckbxGreen.isSelected()){
					activeLayer.greenChannel=chckbxGreen.isSelected();
					repaintComponent();
				}
			}
		});
		
		chckbxBlue = new JCheckBox("Blue");
		add(chckbxBlue, "8, 12");
		chckbxBlue.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (activeLayer != null && activeLayer.blueChannel != chckbxBlue.isSelected()){
					activeLayer.blueChannel=chckbxBlue.isSelected();
					repaintComponent();
				}
			}
		});

	}

	public void updateUIElements(AbstractImageLayer layer){
		this.activeLayer = layer;
		this.contrastSlider.setValue((int)(layer.contrast * 10));
		this.gammaSlider.setValue((int) (Math.log(layer.gamma) / GAMMA_FACTOR));
		this.opacitySlider.setValue((int) (layer.opacity * 100));
		this.sharpenSlider.setValue((int) (layer.sharpness * 100));
		this.comboBoxColorTable.setSelectedItem(layer.getLut());
		this.btnInverseColorTable.setSelected(layer.invertedLut);
		this.chckbxRed.setSelected(layer.redChannel);
		this.chckbxGreen.setSelected(layer.greenChannel);
		this.chckbxBlue.setSelected(layer.blueChannel);
	}
	
	@Override
	public void newLayerAdded() {
		AbstractImageLayer activeLayer = Layers.getActiveImageLayer();
		if (activeLayer != null) this.updateUIElements(activeLayer);
	}

	@Override
	public void newlayerRemoved(int idx) {
	}

	@Override
	public void activeLayerChanged(AbstractLayer layer) {
		if (layer != null && layer.isImageLayer()) {
			this.updateUIElements((AbstractImageLayer) layer);
		}
	}
	
	private void repaintComponent(){
		MainFrame.MAIN_PANEL.repaint();
	}
	
}