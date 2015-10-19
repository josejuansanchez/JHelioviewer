package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.math.Vector2i;
import org.w3c.dom.Document;

class MetaDataHMI extends MetaData
{
	private final static Vector2i RESOLUTION = new Vector2i(4096, 4096);
	public MetaDataHMI(Document _doc)
	{
        super(_doc, RESOLUTION, get(_doc, "TELESCOP"), get(_doc, "CONTENT"));
        if (!(instrument.equalsIgnoreCase("HMI_FRONT2")))
        	throw new UnsuitableMetaDataException("invalid instrument: "+instrument);

        instrument = "HMI";
        fullName = "HMI " + measurement.substring(0, 1) + measurement.substring(1, 3).toLowerCase();
   }
}
