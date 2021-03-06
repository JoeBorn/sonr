/***************************************************************************
 * Copyright (c) 2011, 2012 by Sonr Labs Inc (http://www.sonrlabs.com)
 *
 *You can redistribute this program and/or modify it under the terms of the GNU General Public License v. 2.0 as published by the Free Software Foundation
 *This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 **************************************************************************/

package com.sonrlabs.test.sonr;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.sonrlabs.v96.sonr.R;

public class ConfigurationActivity
      extends ListActivity {
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      // setContentView(R.layout.configuration_activity);

      String[] values = new String[] {
         "Auto-Start SONR",
         "Auto-Adjust Notification Volume"
      };
      ConfigurationListAdapter adapter = new ConfigurationListAdapter(this, values);
      setListAdapter(adapter);
   }

   @Override
   protected void onListItemClick(ListView l, View v, int position, long id) {
      String item = (String) getListAdapter().getItem(position);
      Toast.makeText(this, item + " selected", Toast.LENGTH_LONG).show();
   }

   public class ConfigurationListAdapter
         extends ArrayAdapter<String> {
      private final Context context;
      private final String[] values;

      public ConfigurationListAdapter(Context context, String[] values) {
         super(context, R.layout.configuration_listview_row, values);
         this.context = context;
         this.values = values;
      }

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
         LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
         View rowView = inflater.inflate(R.layout.configuration_listview_row, parent, false);
         TextView textView = (TextView) rowView.findViewById(R.id.label);
         // CheckBox checkBox = (CheckBox)
         // rowView.findViewById(R.id.checkBox);
         textView.setText(values[position]);

         return rowView;
      }
   }
}
