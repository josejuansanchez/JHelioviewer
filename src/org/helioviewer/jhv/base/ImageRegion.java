package org.helioviewer.jhv.base;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.time.LocalDateTime;

import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.gui.opengl.MainPanel;
import org.helioviewer.jhv.layers.LayerInterface;
import org.helioviewer.jhv.opengl.OpenGLHelper;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

/**
 * Class to find the current ImageRegion that's needed to decode
 * @author stefanmeier
 *
 */
public class ImageRegion {
	
	// relative image coordinates
	private Rectangle2D imageData;
	// image size, which have been decoded
	private Rectangle imageSize;
	
	private float imageScaleFactor;
	private double imageZoomFactor;

	private float textureOffsetX = 0;
	private float textureOffsetY = 0;
	private float textureScaleWidth = 1;
	private float textureScaleHeight = 1;

	private LocalDateTime localDateTime;
	
	private int textureID;
	
	public int textureWidth;
	public int textureHeight;

	private float xTextureScale = 1;
	private float yTextureScale = 1;
	
	private MetaData metaData;
	private String fileName;
	private int id;
	
	public ImageRegion(LocalDateTime localDateTime) {
		imageData = new Rectangle();
		this.localDateTime = localDateTime;
	}
			
	public void setImageData(Rectangle2D imageData){
		this.imageData = imageData;
	}
	
	public Rectangle2D getImageData(){
		return this.imageData;
	}

	/**
	 * Function to check, whether the last decoded ImageRegion contains the next ImageRegion
	 * @param imageRegion
	 * @return TRUE --> contain, else FALSE
	 */
	public boolean contains(ImageRegion imageRegion) {
		return imageSize.contains(imageRegion.imageSize);
	}

	/**
	 * Return the current scalefactor
	 * @return scalefactor
	 */
	public double getScaleFactor(){
		return this.imageScaleFactor;
	}
	
	/**
	 * Function to compare the Scalefactor of two ImageRegion
	 * @param imageRegion
	 * @return TRUE --> current scalefactor is equal or higher, else FALSE
	 */
	public boolean compareScaleFactor(ImageRegion imageRegion) {
		return this.imageScaleFactor >= imageRegion.getScaleFactor();
	}

	public void calculateScaleFactor(LayerInterface layerInterface, MainPanel mainPanel, MetaData metaData, Dimension size) {
		// Get the image resolution
		Vector2i resolution = metaData.getResolution();
		
		// Calculate the current texelCount
		int textureMaxX = getNextInt(resolution.getX() * (imageData.getX() + imageData.getWidth()));
		int textureMaxY = getNextInt(resolution.getY() * (imageData.getY() + imageData.getHeight()));
		int textureMinX = (int)(resolution.getX() * imageData.getX());
		int textureMinY = (int)(resolution.getY() * imageData.getY());
		int textureWidth = textureMaxX - textureMinX;
		int textureHeight = textureMaxY - textureMinY;
		double texelCount = textureWidth * textureHeight;
		
		// Calculate the current pixelCount
		double z = metaData.getPhysicalImageWidth() / Math.tan(Math.toRadians(MainPanel.FOV));
		double zoom = mainPanel.getTranslation().z / z;
		int screenMaxX = getNextInt(size.getWidth() * (imageData.getX() + imageData.getWidth()) /  zoom);
		int screenMaxY = getNextInt(size.getHeight() * (imageData.getY() + imageData.getHeight()) / zoom);
		int screenMinX = (int)(size.getWidth() * imageData.getX() / zoom);
		int screenMinY = (int)(size.getHeight() * imageData.getY() / zoom);
		int screenWidth = screenMaxX - screenMinX;
		int screenHeight = screenMaxY - screenMinY;
		double pixelCount = screenWidth * screenHeight;

		// Calculate the imageScalefactors
		this.imageZoomFactor = pixelCount >= texelCount ? 1 : Math.sqrt((pixelCount / texelCount));
		this.imageScaleFactor = imageZoomFactor >= 1.0 ? 1 : nextZoomFraction(imageZoomFactor);
		
		imageSize = new Rectangle(textureMinX, textureMinY, textureWidth, textureHeight);
		
		this.calculateImageSize(resolution);
		
	}	
	
	public float getTextureOffsetX(){
		return textureOffsetX;
	}
	
	public float getTextureOffsetY(){
		return textureOffsetY;
	}
	
	public float getTextureScaleWidth(){
		return textureScaleWidth * xTextureScale;
	}
	
	public float getTextureScaleHeight(){
		return textureScaleHeight * yTextureScale;
	}
	
	/**
	 * Function to calculate the current image size
	 * @param resolution
	 */
	private void calculateImageSize(Vector2i resolution){
		int textureMaxX = getNextInt(resolution.getX() * (imageData.getX() + imageData.getWidth()) * imageScaleFactor);
		int textureMaxY = getNextInt(resolution.getY() * (imageData.getY() + imageData.getHeight()) * imageScaleFactor);
		int textureMinX = (int)(resolution.getX() * imageData.getX() * imageScaleFactor);
		int textureMinY = (int)(resolution.getY() * imageData.getY() * imageScaleFactor);
		int textureWidth = textureMaxX - textureMinX;
		int textureHeight = textureMaxY - textureMinY;
		this.imageSize = new Rectangle(textureMinX, textureMinY, textureWidth, textureHeight);
		
		this.textureOffsetX = textureMinX / (float)(resolution.getX() * imageScaleFactor);
		this.textureOffsetY = textureMinY / (float)(resolution.getY() * imageScaleFactor);
		this.textureScaleWidth = imageSize.width / (float)(resolution.getX() * imageScaleFactor);
		this.textureScaleHeight = imageSize.height / (float)(resolution.getY() * imageScaleFactor);
	}
	
	public Rectangle getImageSize(){
		return imageSize;
	}
	
	private static int getNextInt(double d){
		return d % 1 == 0 ? (int) d : (int)(d - d % 1) + 1;
	}
	
	public float getZoomFactor(){
		return imageScaleFactor;
	}
	
	
	private static float nextZoomFraction(double zoomFactor){
		int powerOfTwo = OpenGLHelper.nextPowerOfTwo(getNextInt((1/zoomFactor)));
		powerOfTwo >>= 1;
		return 1/(float)powerOfTwo;
	}
	

	public LocalDateTime getDateTime() {
		return this.localDateTime;
	}

	public void setLocalDateTime(LocalDateTime currentDateTime) {
		this.localDateTime = currentDateTime;
	}	
	
	public int getTextureID(){
		return this.textureID;
	}
	
	public void setTextureID(int textureID){
		this.textureID = textureID;
	}

	public void setTextureScaleFactor(float xScale, float yScale) {
		this.xTextureScale = xScale;
		this.yTextureScale = yScale;
	}
	
	/**
	 * Set the last decoded MetaData
	 * @param metaData
	 */
	public void setMetaData(MetaData metaData){
		this.metaData = metaData;
	}
	
	public MetaData getMetaData(){
		return metaData;
	}

	public void setID(int id) {
		this.id = id;
	}

	public int getID() {
		return id;
	}
}

