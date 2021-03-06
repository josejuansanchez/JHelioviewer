package org.helioviewer.jhv.layers;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.gui.OverviewPanel;
import org.helioviewer.jhv.plugins.Plugin;
import org.helioviewer.jhv.plugins.Plugin.RenderMode;
import org.helioviewer.jhv.plugins.Plugins;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.json.JSONException;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class PluginLayer extends Layer
{
	final Plugin plugin;
	
	public boolean supportsFilterContrastGamma()
	{
		return plugin.supportsFilterContrastGamma();
	}
	
	public boolean supportsFilterSharpness()
	{
		return plugin.supportsFilterSharpness();
	}
	
	public boolean supportsFilterRGB()
	{
		return plugin.supportsFilterRGB();
	}
	
	public boolean supportsFilterOpacity()
	{
		return plugin.supportsFilterOpacity();
	}
	
	public boolean supportsFilterLUT()
	{
		return plugin.supportsFilterLUT();
	}
	
	public boolean supportsFilterCorona()
	{
		return plugin.supportsFilterCorona();
	}
	
	public PluginLayer(Plugin _plugin)
	{
		name = _plugin.name;
		plugin = _plugin;
		
		plugin.visibilityChanged(isVisible());
	}

	public RenderResult renderLayer(GL2 gl, MainPanel _parent)
	{
		if(_parent instanceof OverviewPanel)
		{
			if(plugin.renderMode==RenderMode.MAIN_PANEL)
				return RenderResult.OK;
		}
		else
		{
			if(plugin.renderMode==RenderMode.OVERVIEW_PANEL)
				return RenderResult.OK;
		}
		
		plugin.render(gl,this);
		return RenderResult.OK;
	}
	
	@Override
	public void setVisible(boolean _visible)
	{
		if(_visible==isVisible())
			return;
		
		if(_visible)
		{
			plugin.timeStampChanged(Plugins.SINGLETON.currentTimeStamp, Plugins.SINGLETON.previousTimeStamp);
			plugin.timeRangeChanged(MathUtils.toLDT(TimeLine.SINGLETON.getFirstTimeMS()),MathUtils.toLDT(TimeLine.SINGLETON.getLastTimeMS()));
		}
		
		super.setVisible(_visible);
		plugin.visibilityChanged(_visible);
	}
	
	@Override
	public void dispose()
	{
		throw new RuntimeException();
	}	
	
	@Override
	public boolean retryNeeded()
	{
		return plugin.retryNeeded();
	}
	
	@Override
	public void retry()
	{
		plugin.retry();
		Plugins.repaintLayerPanel();
	}
	
	@Override
	public @Nullable long getCurrentTimeMS()
	{
		if(!isVisible())
			return 0;
		
		//this method might be invoked during initialization, when the singleton is
		//not yet available. we shouldn't expect plugins to handle this edge case.
		if(Plugins.SINGLETON==null)
			return 0;
		
		return MathUtils.fromLDT(plugin.getCurrentlyVisibleTime());
	}

	@Override
	public void storeConfiguration(JSONObject _jsonLayer) throws JSONException
	{
		_jsonLayer.put("type", "plugin");
		_jsonLayer.put("pluginId", plugin.id);
		storeJSONState(_jsonLayer);
		plugin.storeConfiguration(_jsonLayer);
	}
	
	public void restoreConfiguration(JSONObject _jsonLayer) throws JSONException
	{
		if(!plugin.id.equals(_jsonLayer.getString("pluginId")))
			throw new IllegalArgumentException();
		
		plugin.restoreConfiguration(_jsonLayer);
		
		applyJSONState(_jsonLayer);
	}

	@Override
	public @Nullable String getFullName()
	{
		return plugin.name;
	}
}
