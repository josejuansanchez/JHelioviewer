package org.helioviewer.jhv;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.TimeZone;

import javafx.embed.swing.JFXPanel;

import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import kdu_jni.Kdu_global;
import kdu_jni.Kdu_message_formatter;

import org.helioviewer.jhv.base.Log;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.dialogs.AboutDialog;
import org.helioviewer.jhv.io.CommandLineProcessor;
import org.helioviewer.jhv.opengl.TextureCache;
import org.helioviewer.jhv.plugins.plugin.Plugins;
import org.helioviewer.jhv.viewmodel.jp2view.kakadu.KduErrorHandler;

import com.install4j.api.launcher.ApplicationLauncher;
import com.install4j.api.update.UpdateScheduleRegistry;
import com.jogamp.opengl.DebugGL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLProfile;

public class JHelioviewer
{
	public static void main(String[] args)
	{
		// Setup Swing
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e2)
		{
			e2.printStackTrace();
		}
		
		// Uncaught runtime errors are displayed in a dialog box in addition
		JHVUncaughtExceptionHandler.setupHandlerForThread();
		
		try
		{
			Log.redirectStdOutErr();
			if (JHVGlobals.isReleaseVersion())
			{
				if (UpdateScheduleRegistry.checkAndReset())
				{
					// This will return immediately if you call it from the EDT,
					// otherwise it will block until the installer application
					// exits
					ApplicationLauncher.launchApplicationInProcess("366", null,
							new ApplicationLauncher.Callback()
							{
								public void exited(int exitValue)
								{
								}

								public void prepareShutdown()
								{
								}
							}, ApplicationLauncher.WindowMode.FRAME, null);
				}
			}

			if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help")))
			{
				System.out.println(CommandLineProcessor.getUsageMessage());
				return;
			}

			Telemetry.trackEvent("Startup","args",Arrays.toString(args));
			CommandLineProcessor.setArguments(args);

			ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
			JPopupMenu.setDefaultLightWeightPopupEnabled(false);

			// initializes JavaFX environment
			if(JHVGlobals.USE_JAVA_FX)
				SwingUtilities.invokeLater(new Runnable()
				{
				    public void run()
				    {
				        new JFXPanel();
				    }
				});
			
			// Save command line arguments
			CommandLineProcessor.setArguments(args);

			// Save current default system timezone in user.timezone
			System.setProperty("user.timezone", TimeZone.getDefault().getID());

			// Per default all times should be given in GMT
			TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

			// Save current default locale to user.locale
			System.setProperty("user.locale", Locale.getDefault().toString());
			
			// Per default, the us locale should be used
			Locale.setDefault(Locale.US);
			
			GLProfile.initSingleton();
			GLDrawableFactory factory = GLDrawableFactory.getFactory(GLProfile.getDefault());
			GLProfile profile = GLProfile.get(GLProfile.GL2);
			profile = GLProfile.getDefault();
			GLCapabilities capabilities = new GLCapabilities(profile);
			final boolean createNewDevice = true;
			final GLAutoDrawable sharedDrawable = factory.createDummyAutoDrawable(null, createNewDevice, capabilities, null);
			sharedDrawable.display();
			if (System.getProperty("jhvVersion") == null)
				sharedDrawable.setGL(new DebugGL2(sharedDrawable.getGL().getGL2()));
			
			System.out.println("JHelioviewer started with command-line options:" + String.join(" ", args));
			System.out.println("Initializing JHelioviewer");

			// display the splash screen
			final SplashScreen splash = SplashScreen.getSingletonInstance();

			splash.setProgressSteps(5);

			JHVGlobals.initFileChooserAsync();

			// Load settings from file but do not apply them yet
			// The settings must not be applied before the kakadu engine has
			// been initialized
			splash.progressTo("Loading settings...");
			Settings.load();

			splash.progressTo("Initializing Kakadu libraries...");
			try
			{
				loadLibraries();
			}
			catch (UnsatisfiedLinkError _ule)
			{
				if (JHVGlobals.isLinux() && _ule.getMessage().contains("GLIBC"))
				{
					splash.setVisible(false);
					JOptionPane.showMessageDialog(null,
									"JHelioviewer requires a more recent version of GLIBC. Please update your distribution.\n\n"
									+ _ule.getMessage(),
									"JHelioviewer", JOptionPane.ERROR_MESSAGE);
					return;
				}

				throw _ule;
			}

			// The following code-block attempts to start the native message handling, otherwise
			// KDU just terminates our process when something goes wrong... (!?!)
			splash.progressTo("Setting up Kakadu message handlers");
            Kdu_global.Kdu_customize_warnings(new Kdu_message_formatter(new KduErrorHandler(false), 80));
            Kdu_global.Kdu_customize_errors(new Kdu_message_formatter(new KduErrorHandler(true), 80));

			// Create main view chain and display main window
            splash.progressTo("Starting Swing");
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					MainFrame.SINGLETON.initSharedContext(sharedDrawable.getContext());
					
					// force initialization of UltimatePluginInterface
					splash.progressTo("Initialize plugins");
					Plugins.SINGLETON.getClass();
					
					splash.progressTo("Setting up texture cache");
					sharedDrawable.getContext().makeCurrent();
					TextureCache.init();
					
					splash.progressTo("Opening main window");
					MainFrame.SINGLETON.setVisible(true);
					
					splash.dispose();
					UILatencyWatchdog.startWatchdog();
				}
			});
		}
		catch (Throwable _t)
		{
			JHVUncaughtExceptionHandler.getSingletonInstance().uncaughtException(Thread.currentThread(), _t);
		}
	}

	private static void loadLibraries()
	{
		try
		{
			Path tmpLibDir = Files.createTempDirectory("jhv-libs");
			tmpLibDir.toFile().deleteOnExit();

			if (JHVGlobals.isWindows())
			{
				System.loadLibrary("msvcr120");
				System.loadLibrary("kdu_v75R");
				System.loadLibrary("kdu_a75R");
			}

			System.loadLibrary("kdu_jni");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private static void setupOSXApplicationListener()
	{
		final AboutDialog aboutDialog = new AboutDialog();
		try
		{
			Class<?> applicationClass = Class.forName("com.apple.eawt.Application");
			Object application = applicationClass.newInstance();
			Class<?> applicationListener = Class.forName("com.apple.eawt.ApplicationListener");
			Object listenerProxy = Proxy.newProxyInstance(
					applicationListener.getClassLoader(),
					new Class[] { applicationListener },
					new InvocationHandler()
					{
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
						{
							if ("handleAbout".equals(method.getName()))
							{
								aboutDialog.showDialog();
								setHandled(args[0], Boolean.TRUE);
							}
							else if ("handleQuit".equals(method.getName()))
							{
								System.exit(0);
							}
							
							return null;
						}

						private void setHandled(Object event, Boolean val) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
						{
							Method handleMethod = event.getClass().getMethod("setHandled", new Class[] { boolean.class });
							handleMethod.invoke(event, new Object[] { val });
						}
					});
			
			Method registerListenerMethod = applicationClass.getMethod("addApplicationListener", new Class[] { applicationListener });
			registerListenerMethod.invoke(application, new Object[] { listenerProxy });
		}
		catch (Throwable e)
		{
			System.err.println("Failed to create native menuitems for Mac OSX");
		}
	}
}
