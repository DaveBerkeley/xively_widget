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

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
//import android.util.Log;

public class Settings {
	
	//private static final String TAG = "Xively/Settings";
	public static final String PREFS_NAME = "xively.rotwang.co.uk";	

	String key;
	String feed;
	String stream;
	String duration;
	String number_ids;
	String scale_min;
	String scale_max;
	boolean scale_auto;
	String graph_colour;
	String number_colour;
	String update_period;

	public void show(String label) {
		/*
		Log.v(TAG, label + 
				//" key=" + key +
				" feed=" + feed + 
				", stream=" + stream +
				", duration=" + duration +
				", number_ids=" + number_ids +
				", scale_auto=" + scale_auto +
				", scale_min=" + scale_min +
				", scale_max=" + scale_max +
				", graph_colour=" + graph_colour +
				", number_colour=" + number_colour +
				", update_period=" + update_period +
				//", value=" + show_value +
				""
				);
		*/
	}

	private String prefsName(int widgetId) {
		return PREFS_NAME + " " + widgetId;
	}

	public void read(Context context, int widgetId)
	{
		String name = prefsName(widgetId);
        SharedPreferences sp = context.getSharedPreferences(name, Context.MODE_WORLD_READABLE);

        key = sp.getString("key", "");
        feed = sp.getString("feed", "");
        stream = sp.getString("stream", "0");
        duration = sp.getString("duration", "12hours");
        number_ids = sp.getString("number_ids", "");
        scale_min = sp.getString("scale_min", "0");
        scale_max = sp.getString("scale_max", "3000");
        scale_auto = sp.getBoolean("scale_auto", true);
        graph_colour = sp.getString("graph_colour", "FF0000"); // RRGGBB
        number_colour = sp.getString("number_colour", "0000FF"); // RRGGBB
        update_period = sp.getString("update_period", "3600000"); // 30m
        show("read");
	}

	public void save(Context context, int widgetId)
	{
		//	Save to SharedPreferences
		show("save");
		String name = prefsName(widgetId);
        SharedPreferences sp = context.getSharedPreferences(name, Context.MODE_WORLD_READABLE);
        SharedPreferences.Editor edit = sp.edit();
	    edit.putString("key", key);
	    edit.putString("feed", feed);
	    edit.putString("stream", stream);
	    edit.putString("duration", duration);
	    edit.putString("number_ids", number_ids);
	    edit.putString("scale_min", scale_min);
	    edit.putString("scale_max", scale_max);
	    edit.putBoolean("scale_auto", scale_auto);
	    edit.putString("graph_colour", graph_colour);
	    edit.putString("number_colour", number_colour);
	    edit.putString("update_period", update_period);
	    edit.commit();
	}

    public static int getWidgetId(Intent intent) {
    	int id = -1;
    	Bundle bundle = intent.getExtras();
    	if (bundle != null)
    		id = bundle.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -3);
    	return id;
    }

    public static void setWidgetId(Intent intent, int widgetId) {
    	if (widgetId < 0) {
    		return;
    	}

    	if (widgetId > 0)
    		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);

        // from http://stackoverflow.com/questions/4011178/multiple-instances-of-widget-only-updating-last-widget/4011431#4011431
        Uri data = Uri.withAppendedPath(Uri.parse("hack://widget/id/"), "" + widgetId);
      	intent.setData(data);
    }	
}
