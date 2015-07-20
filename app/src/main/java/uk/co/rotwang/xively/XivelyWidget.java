/*
 Copyright (C) 2012, 2015 Dave Berkeley android@rotwang.co.uk

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 USA
*/

package uk.co.rotwang.xively;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * Define a widget to display COSM graph and data.
 */
public class XivelyWidget extends AppWidgetProvider {

	private static final String TAG = "XivelyWidget";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] widgetIds) {    	
        // To prevent any ANR timeouts, we perform the update in a service
        for (int wid : widgetIds) {
        	// Update each widget that requests it
        	Intent intent = new Intent(context, UpdateService.class);
        	Settings.setWidgetId(intent, wid);
        	context.startService(intent);
        }
    }

    /*
    @Override
    public void onReceive(Context context, Intent intent) {
    	super.onReceive(context, intent);
    	Log.v(TAG, "onReceive " + intent.getAction());
    	// String action = intent.getAction();
    	// on each widget being deleted       : ACTION_APPWIDGET_DELETED
    	// on last widget being removed       : ACTION_APPWIDGET_DISABLED
    	// on first widget being added        : ACTION_APPWIDGET_ENABLED
    	// on widget being created + periodic : ACTION_APPWIDGET_UPDATE

    	if (intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
    		// updated (possibly with new parameters)
    		// widget(s) in EXTRA_APPWIDGET_IDS
    		Bundle extras = intent.getExtras();
    		if (extras != null) {
    			int[] wids = extras.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS);

    			for (int wid : wids) {
        			long period = 10000;
        			AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        			PendingIntent pi = makeServicePendingIntent(context, wid);
    				Calendar now = Calendar.getInstance();
        			alarm.setRepeating(AlarmManager.RTC, now.getTimeInMillis(), period, pi);
        			Log.v(TAG, "setRepeating wid=" + wid);
    			}
    		}
    	}
    	if (intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_DISABLED)) {
    		// last widget removed
    		//AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    		//alarmManager.cancel(makeServicePendingIntent(context, 0));
    	}
    	if (intent.getAction().equals(AppWidgetManager.ACTION_APPWIDGET_DELETED)) {
    		// this widget (in EXTRA_APPWIDGET_ID) deleted
    		Bundle extras = intent.getExtras();
    		if (extras != null) {
    			int wid = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    			AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    			alarmManager.cancel(makeServicePendingIntent(context, wid));
    		}
    	}
    }
    */

    private static PendingIntent makeEditorPendingIntent(Context context, int widgetId) {
    	//return makeServicePendingIntent(context, widgetId);
    	// run the widget editor when the intent is fired
        Intent intent = new Intent(context, XivelyWidgetActivity.class);
        Settings.setWidgetId(intent, widgetId);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public static class UpdateService extends Service {

		Random random = new Random();

        @Override
        public void onStart(Intent intent, int startId) {     
        	// Deprecated start command, for pre 2.0 platforms
        	handleStart(intent);
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
        	handleStart(intent);
            // We want this service to continue running until it is explicitly
            // stopped, so return sticky.
            return START_STICKY;
        }

        private void handleStart(Intent intent) {
        	Log.v(TAG, "handleStart wid=" + Settings.getWidgetId(intent));
        	Thread thread = new HandlerThread(intent);
        	thread.start();
        }

        class HandlerThread extends Thread {
        	/*
        	 * Use a thread to perform the update, so that the
        	 * same service can be shared between several widgets
        	 * without causing delay.
        	 */
        	Intent intent;
        	
        	HandlerThread(Intent i) {
        		intent = i;
        	}
        	
        	@Override
        	public void run() {
        		updateWidget(intent);
        	}
        }

        private void updateWidget(Intent intent) {
        	// Update the widget
        	//Log.v(TAG, "service start");
        	int widgetId = Settings.getWidgetId(intent);
        	// We need to be associated with a widget
        	// or we won't know which settings to load.
        	if (widgetId < 0)
        		return;

            // Push update for *this* widget to the home screen
            RemoteViews updateViews = buildUpdate(this, widgetId);
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            manager.updateAppWidget(widgetId, updateViews);
        	//Log.v(TAG, "service end");
        }

        private String nocache() {
        	/*
        	 * Some service providers (especially mobile) will use a web proxy to cache
        	 * the results, so they'll give you old data. Adding a unique key to the 
        	 * request should prevent this from happening. This only works if the server 
        	 * doesn't mind the spurious parameter!  
        	 */
    		return "killcache=" + random.nextInt();
        }

        private InputStream openHttpConnection(String text, String key) throws IOException {
        	Log.v(TAG, "url=" + text);
        	URL url = new URL(text);
        	URLConnection conn = url.openConnection();

       		HttpURLConnection http = (HttpURLConnection)conn;
       		http.setRequestMethod("GET");
       		if (key != null)
       			http.setRequestProperty("X-ApiKey", key);
       		http.connect();

       		if (http.getResponseCode() == HttpURLConnection.HTTP_OK) {
       			return http.getInputStream();
       		}

        	return null;
       	}

        private Bitmap getBitmapFromURL(String url) {
        	BitmapFactory.Options options;
        	options = new BitmapFactory.Options();
        	options.inSampleSize = 1;
            Bitmap bitmap = null;
            InputStream in = null;       
            try {
            	// Don't need the key for graphs
                in = openHttpConnection(url, null);
                if (in == null)
                	return null;
                bitmap = BitmapFactory.decodeStream(in, null, options);
                in.close();
            } catch (IOException e) {
       			e.printStackTrace();
            }
            return bitmap;     
        }

        private String getXivelyStream(String stream, JSONObject json) throws JSONException {
			JSONArray streams = json.getJSONArray("datastreams");

			String status = json.getString("status");
			if (!status.equals("live"))
				return status;

			for (int i = 0; i < streams.length(); ++i) {
				JSONObject st = streams.getJSONObject(i);
				String id = st.getString("id");
				if (!id.equals(stream))
					continue;

				String value = st.getString("current_value");
				String label = "";
				JSONObject units = st.getJSONObject("unit");
				if (units != null) {
					label = units.getString("symbol");
					if (label != null)
						value = value + " " + label;
				}
				return value;
			}
        	return getString(R.string.no_stream);
        }

        private String getCurrentValues(String url, String[] streams, String key) {
      		try {
				InputStream io = openHttpConnection(url, key);
				if (io == null)
					return getString(R.string.no_service);
				InputStreamReader is = new InputStreamReader(io);
				BufferedReader reader = new BufferedReader(is);
				String text = reader.readLine();
				if (text == null) {
					Log.v(TAG, "no JSON data");
		        	return getString(R.string.error);
				}

				// Concat each requested data stream's text
				JSONObject json = new JSONObject(text);
				String result = "";
				for (int i = 0; i < streams.length; ++i) {
					String item = getXivelyStream(streams[i], json);

					if (result.length() > 0)
						result += "\n";
					result += item;
				}
				return result;

			} catch (IOException e) {
				;
			} catch (JSONException e) {
				e.printStackTrace();
			}

        	return getString(R.string.no_service);
        }

		/**
         * Build a widget update to display Xively Data.
         * Will block until the data is acquired.
         */
        public RemoteViews buildUpdate(Context context, int widgetId) {
            // Build an update that holds the updated widget contents
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_message);

            // Allow the editor to be started when the widget is clicked
            PendingIntent pendingIntent = makeEditorPendingIntent(context, widgetId);
            views.setOnClickPendingIntent(R.id.widget, pendingIntent);

            // Fetch the persistent settings 
            Settings s = new Settings();
            s.read(this, widgetId);

            // TODO : make these work with any phone
            int width = 500;
            int height = 160;
            // convert "rrggbb" into web format
            String graph_colour = URLEncoder.encode("#" + s.graph_colour);

            String url = "http://api.cosm.com/v2/feeds/";            
            url += s.feed + "/datastreams/" + s.stream + ".png";
            url += 	"?width=" + width + 
            		"&height=" + height +
            		"&colour=" + graph_colour +
            		"&duration=" + s.duration +
            		"&show_axis_labels=true" +
            		"&detailed_grid=true" +
            		"";

            // TODO : Get the timezone working properly
            url += "&timezone=UTC";

            if (s.scale_auto) {
            	url += "&scale=auto";
            } else {
            	url += "&scale=manual";
                url += "&max=" + s.scale_max;
                url += "&min=" + s.scale_min;
            }
            
            url += "&" + nocache();
			String number_text = "";

            // Fetch the bitmap and display it
			Bitmap b = getBitmapFromURL(url);
			if (b == null) {
				//	blank the screen if we can't see any data
				b = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
				Canvas canvas = new Canvas(b);
				canvas.drawColor(Color.LTGRAY);
				number_text = getString(R.string.bitmap_error);
			}
			views.setImageViewBitmap(R.id.image, b);

			// Now fetch any numeric ids.			
			if (s.feed.length() == 0) {
				number_text = getString(R.string.no_feed);
			} else {			
				if (s.number_ids.length() > 0) {
					String number_url = "http://api.cosm.com/v2/feeds/" + s.feed + ".json";
					number_url += "?" + nocache();
					// number ids are comma separated
					String[] streams = s.number_ids.split(",");
					number_text = getCurrentValues(number_url, streams, s.key);
				}
			}
			views.setTextColor(R.id.value, Color.parseColor("#" + s.number_colour));
			views.setTextViewText(R.id.value, number_text);

            return views;
        }

        @Override
        public IBinder onBind(Intent intent) {
            // We don't need to bind to this service
            return null;
        }
    }
}
