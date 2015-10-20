package org.helioviewer.jhv.base;

import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Settings
{
    private static final Properties DEFAULT_PROPERTIES = new Properties();
	private static final Preferences PREF_NODE = Preferences.userRoot().node("jhelioviewer");
    
    static
    {
        if(PREF_NODE.get("UUID",null)==null)
        {
            PREF_NODE.put("UUID",UUID.randomUUID().toString());
            try
            {
                PREF_NODE.flush();
            }
            catch(BackingStoreException e)
            {
            	e.printStackTrace();
            }
        }
    }
    

    private Settings()
    {
    }

    public static void load()
    {
        DEFAULT_PROPERTIES.clear();
        try (InputStream defaultPropStream = Settings.class.getResourceAsStream("/settings/defaults.properties"))
        {
            DEFAULT_PROPERTIES.load(defaultPropStream);
            DEFAULT_PROPERTIES.setProperty("default.local.path",Directories.REMOTEFILES.getPath());
            
            defaultPropStream.close();
            System.out.println(">> Settings.load() > Load default system settings: " + DEFAULT_PROPERTIES.toString());
        }
        catch (Exception ex)
        {
            System.err.println(">> Settings.load(boolean) > Could not load settings");
            Telemetry.trackException(ex);
        }
    }

    /**
     * Method sets the value of a specified property and saves it as a user
     * setting.
     * 
     * @param key
     *            Default field to be written to
     * @param val
     *            Value to be set to
     */
	public static void setProperty(String key, String val)
    {
        if (val.equals(getProperty(key)))
            return;
        
        PREF_NODE.put(key,val);
        
        synchronized(syncObj)
        {
            if(saveThread!=null)
                saveThread.interrupt();
            else
            {
                saveThread=new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        for(;;)
                            try
                            {
                                Thread.sleep(1000);
                                break;
                            }
                            catch(InterruptedException _ie)
                            {
                            }
                        
                        synchronized(syncObj)
                        {
                            saveThread=null;
                        }
                        
                        try
                        {
                            PREF_NODE.flush();
                        }
                        catch (Exception ex)
                        {
                        	Telemetry.trackException(ex);
                        }
                    }
                });
                saveThread.start();
            }
        }
    }
    
    //used to coordinate delayed flushing
    private final static Object syncObj=new Object();
    private static @Nullable Thread saveThread;

    /**
     * Method that returns the value of the specified property. User defined
     * properties are always preferred over the default settings.
     * 
     * @param key
     *            Default field to read
     */
    public static @Nullable String getProperty(@Nonnull String key)
    {
        return PREF_NODE.get(key,DEFAULT_PROPERTIES.getProperty(key));
    }
}