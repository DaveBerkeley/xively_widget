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

import uk.co.rotwang.xively.XivelyWidget.UpdateService;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class XivelyWidgetActivity extends Activity {

	private static final String TAG = "XivelyWidgetActivity";

	private int widgetId;

    /** Called when the activity is first created. */
    //@OverrideSettings
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        widgetId = Settings.getWidgetId(intent);

        if (widgetId < 0) {
        	// Show message about configuring widgets
        	Log.v(TAG, "bad id=, do nothing");
            setContentView(R.layout.no_wid);
            return;
        }

        setContentView(R.layout.main);

        // Configure the duration spinner
        Spinner spinner = (Spinner) findViewById(R.id.duration);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.duration_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Only allow hex digits for number editing
        EditText text;
        InputFilter hex_filter[] = {
       		new TextFilter("0123456789" + "abcdef" + "ABCDEF"),
        };
        text = (EditText) findViewById(R.id.number_colour);
        text.setFilters(hex_filter);
        text = (EditText) findViewById(R.id.graph_colour);
        text.setFilters(hex_filter);

        // Set the field values from storage
        Settings s = new Settings();
        s.read(this, widgetId);
        setGuiSettings(s);
    }

    @Override
    protected void onPause() {
    	super.onPause();

    	if (widgetId < 0)
    		return;

   		// Save the settings when the Activity is paused
   		Settings s = getGuiSettings();
   		s.save(getApplicationContext(), widgetId);

   		// Tell the widget to update itself
   		Intent intent = new Intent(this, UpdateService.class);
   		Settings.setWidgetId(intent, widgetId);
   		startService(intent);
    }

    class TextFilter implements InputFilter {
    	/*
    	 * Class to implement generic text filtering
    	 */
    	String okay;
    	TextFilter(String s) {
    		okay = s;
    	}

    	public CharSequence filter(CharSequence source, int start, int end,
    				Spanned dest, int dstart, int dend) {
    		for (int i = start; i < end; i++) {
    			int idx = okay.indexOf(source.charAt(i));
    			if (idx == -1)
    				return "";
    		}
    		return null;
    	}
    }; 

    private static final int MENU_ABOUT = 1;
    private static final int MENU_HELP = 2;
    private static final int MENU_COSM = 3;

    private static final int DIALOG_ABOUT = 1;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu); 
		// Add the menus
    	menu.add(Menu.NONE, MENU_ABOUT, Menu.NONE, R.string.about_menu);
    	menu.add(Menu.NONE, MENU_HELP, Menu.NONE, R.string.help_menu);
    	menu.add(Menu.NONE, MENU_COSM, Menu.NONE, R.string.xively_menu);
    	return true;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        	case DIALOG_ABOUT: {
        		return new MyAboutDialog(this);
        	}
       	}
    	return super.onCreateDialog(id);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
        	case MENU_ABOUT: {
        		showDialog(DIALOG_ABOUT);
        		break;
        	}
        	case MENU_HELP: {
        		// Goto help page
        		Resources res = getResources();
        		String url = res.getString(R.string.help_url);
        		Intent intent = new Intent(Intent.ACTION_VIEW);
        		intent.setData(Uri.parse(url));
        		startActivity(intent);
        		break;
        	}
        	case MENU_COSM: {
        		// Goto Xively page
        		Resources res = getResources();
        		String url = res.getString(R.string.xively_url);
                Settings s = new Settings();
                s.read(this, widgetId);
                url += "" + s.feed;
        		Intent intent = new Intent(Intent.ACTION_VIEW);
        		intent.setData(Uri.parse(url));
        		startActivity(intent);
        		break;
        	}
        }
        return false;
    }

    private class MyAboutDialog extends Dialog {
		protected MyAboutDialog(Context context) {
			super(context);
			setContentView(R.layout.about_dialog);
			setTitle(R.string.app_name);
			TextView tv = (TextView) findViewById(R.id.about_dialog_text);
			// allow http links in the text to be active
		    tv.setMovementMethod(LinkMovementMethod.getInstance());
		}
    };

    	/*
    	 * 
    	 */
    
    private Settings getGuiSettings()
	{
		// Read Settings from the GUI elements.
        Settings s = new Settings();
        EditText edit;
        Spinner spinner;
        CheckBox check;

        edit = (EditText) findViewById(R.id.key);
        s.key = edit.getText().toString();
        edit = (EditText) findViewById(R.id.feed);
        s.feed = edit.getText().toString();
        edit = (EditText) findViewById(R.id.stream);
        s.stream = edit.getText().toString();
        edit = (EditText) findViewById(R.id.scale_min);
        s.scale_min = edit.getText().toString();
        edit = (EditText) findViewById(R.id.scale_max);
        s.scale_max = edit.getText().toString();
        edit = (EditText) findViewById(R.id.number_ids);
        s.number_ids = edit.getText().toString();
        spinner = (Spinner) findViewById(R.id.duration);
        s.duration = spinner.getSelectedItem().toString();
        check = (CheckBox) findViewById(R.id.scale_auto);
        s.scale_auto = check.isChecked();
        edit = (EditText) findViewById(R.id.graph_colour);
        s.graph_colour = edit.getText().toString();
        edit = (EditText) findViewById(R.id.number_colour);
        s.number_colour = edit.getText().toString();
        edit = (EditText) findViewById(R.id.title);
        s.title = edit.getText().toString();
        s.show("get gui");

        return s;
	}

	private int findIndex(String match, int id) {
		// check through the string array looking for a match
		Resources res = getResources();
		String[] haystack = res.getStringArray(id);
		for (int i = 0; i < haystack.length; ++i)
			if (haystack[i].equals(match))
				return i;
		return -1;
	}

	private void setGuiSettings(Settings s)
	{
		// Write Settings to the GUI elements
		EditText edit;
		Spinner spinner;
		CheckBox check;

        edit = (EditText) findViewById(R.id.key);
        edit.setText(s.key);
        edit = (EditText) findViewById(R.id.feed);
        edit.setText(s.feed);
        edit = (EditText) findViewById(R.id.stream);
        edit.setText(s.stream);
        edit = (EditText) findViewById(R.id.scale_min);
        edit.setText(s.scale_min);
        edit = (EditText) findViewById(R.id.scale_max);
        edit.setText(s.scale_max);
        edit = (EditText) findViewById(R.id.number_ids);
        edit.setText(s.number_ids);
        spinner = (Spinner) findViewById(R.id.duration);
        spinner.setSelection(findIndex(s.duration, R.array.duration_array));
        check = (CheckBox) findViewById(R.id.scale_auto);
        check.setChecked(s.scale_auto);
        edit = (EditText) findViewById(R.id.graph_colour);
        edit.setText(s.graph_colour);
        edit = (EditText) findViewById(R.id.number_colour);
        edit.setText(s.number_colour);
        edit = (EditText) findViewById(R.id.title);
        edit.setText(s.title);
	}
}
