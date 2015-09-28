package org.helioviewer.jhv.base.downloadmanager;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.helioviewer.jhv.Telemetry;

public class UltimateDownloadManager
{
	private static class Tuple
	{
		RuntimeException ex;
		WeakReference<AbstractDownloadRequest> request;
	}
	
	
	private static PriorityBlockingQueue<Tuple> taskDeque = new PriorityBlockingQueue<Tuple>(100, new Comparator<Tuple>()
	{
		@Override
		public int compare(Tuple o1, Tuple o2)
		{
			AbstractDownloadRequest oo1 = o1.request.get();
			AbstractDownloadRequest oo2 = o2.request.get();
			if (oo1 == null || oo2 == null)
				return 0;
			
			return oo2.priority.ordinal() - oo1.priority.ordinal();
		}
	});

	private static final int CONCURRENT_DOWNLOADS = 4;
	private static AtomicInteger activeNormalAndHighPrioDownloads = new AtomicInteger();

	static
	{
		for (int i = 0; i < CONCURRENT_DOWNLOADS; i++)
		{
			Thread thread = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						for(;;)
						{
							Tuple t = taskDeque.take();
							AbstractDownloadRequest request = t.request.get();
							if (request != null)
								try
								{
									if(request.priority.ordinal()>DownloadPriority.LOW.ordinal())
										activeNormalAndHighPrioDownloads.incrementAndGet();
									request.execute();
								}
								catch (IOException e)
								{
									System.err.println(request.url);
									Telemetry.trackException(e);
									if (request.justTriedShouldTryAgain())
										addRequest(request);
									else
										request.setError(e);
								}
								finally
								{
									if(request.priority.ordinal()>DownloadPriority.LOW.ordinal())
										activeNormalAndHighPrioDownloads.decrementAndGet();
								}
							else
								Telemetry.trackException(t.ex);
						}
					}
					catch (InterruptedException e)
					{
					}
				}
			});
			thread.setName("Download thread #" + i);
			thread.setDaemon(true);
			thread.start();
		}
	}

	public static void addRequest(AbstractDownloadRequest _request)
	{
		Tuple t=new Tuple();
		t.request=new WeakReference<>(_request);
		
		try
		{
			throw new RuntimeException("Request for was not canceled properly: "+_request.url);
		}
		catch(RuntimeException _t)
		{
			t.ex=_t;
		}
		
		taskDeque.put(t);
	}

	public static void remove(AbstractDownloadRequest request)
	{
		for (Tuple t : taskDeque)
			if(t.request.get()==request)
			{
				taskDeque.remove(t);
				return;
			}
	}

	public synchronized static boolean areDownloadsActive()
	{
		return activeNormalAndHighPrioDownloads.get()>0;
	}
}
