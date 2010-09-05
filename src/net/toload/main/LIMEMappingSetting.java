/*    
**    Copyright 2010, The LimeIME Open Source Project
** 
**    Project Url: http://code.google.com/p/limeime/
**                 http://android.toload.net/
**
**    This program is free software: you can redistribute it and/or modify
**    it under the terms of the GNU General Public License as published by
**    the Free Software Foundation, either version 3 of the License, or
**    (at your option) any later version.

**    This program is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
**    GNU General Public License for more details.

**    You should have received a copy of the GNU General Public License
**    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package net.toload.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;

/**
 * 
 * @author Art Hung
 * 
 */

public class LIMEMappingSetting extends Activity {
	

		private AlertDialog ad;
		private ArrayList<File> filelist;
		private boolean hasSelectFile;
		
		private IDBService DBSrv = null;

		Button btnBackToPreviousPage = null;
		Button btnLoadMapping = null;
		Button btnResetMapping = null;
		TextView labSource = null;
		TextView labVersion = null;
		TextView labTotalAmount = null;
		TextView labImportDate = null;
		TextView labMappingSettingTitle = null;
		private ScrollView scrollSetting;
		
		private String imtype = null;
		
		LIMEPreferenceManager mLIMEPref;
		
		/** Called when the activity is first created. */
		@Override
		public void onCreate(Bundle icicle) {
			
			super.onCreate(icicle);
			this.setContentView(R.layout.kbsetting);


			// Startup Service
			getApplicationContext().bindService(new Intent(IDBService.class.getName()), serConn, Context.BIND_AUTO_CREATE);
			

			mLIMEPref = new LIMEPreferenceManager(this.getApplicationContext());
			
	        try{
		        Bundle bundle = this.getIntent().getExtras();
		        if(bundle != null){
		        	imtype = bundle.getString("keyboard");
		        }
	        }catch(Exception e){
	        	e.printStackTrace();
	        }

			// Initial Buttons
			initialButton();

			scrollSetting.setOnTouchListener(new OnTouchListener(){
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					updateLabelInfo();
					return false;
				}
			});
			
			btnBackToPreviousPage.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					finish();
				}
			});

			
			btnLoadMapping.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					hasSelectFile = false;
					resetLabelInfo();
                	selectLimeFile(LIME.IM_LOAD_LIME_ROOT_DIRECTORY, imtype);
				}
			});

			
			btnResetMapping.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					
					AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
     				builder.setMessage(getText(R.string.l3_message_table_reset_confirm));
     				builder.setCancelable(false);
     				builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
     					public void onClick(DialogInterface dialog, int id) {
		    					initialButton();
		    					try {
		    						resetLabelInfo();
		    						updateLabelInfo();
		    						DBSrv.resetMapping(imtype);
		    					} catch (RemoteException e) {
		    						e.printStackTrace();
		    					}
		    	        	}
		    	     });
        
		    	    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
		    	    	public void onClick(DialogInterface dialog, int id) {
		    	        	}
		    	     });   
        
					AlertDialog alert = builder.create();
								alert.show();
								
					
				}
			});

		}
		
		/* (non-Javadoc)
		 * @see android.app.Activity#onStart()
		 */
		@Override
		protected void onStart() {
			super.onStart();
			initialButton();
		}
		

		/* (non-Javadoc)
		 * @see android.app.Activity#onStart()
		 */
		@Override
		protected void onResume() {
			super.onStart();
			initialButton();
		}
		
		
		private void initialButton(){

			btnBackToPreviousPage = (Button) findViewById(R.id.btnBackToPreviousPage);
			btnLoadMapping =  (Button) findViewById(R.id.btnLoadMapping);
			btnResetMapping =  (Button) findViewById(R.id.btnResetMapping);

			labSource = (TextView) findViewById(R.id.labSource);	
			labVersion = (TextView) findViewById(R.id.labVersion);	
			labTotalAmount = (TextView) findViewById(R.id.labTotalAmount);	
			labImportDate = (TextView) findViewById(R.id.labImportDate);
			labMappingSettingTitle = (TextView) findViewById(R.id.labMappingSettingTitle);
			
			scrollSetting = (ScrollView) this.findViewById(R.id.IMSettingScrollView);
			
			boolean hasIMLoaded = false;
			if(imtype != null){
				if(imtype.equals("cj")){
					SharedPreferences sp = getSharedPreferences(LIME.IM_CJ_STATUS, 0);
					if(sp.getString(LIME.IM_CJ_STATUS, "false").equals("false")){
						hasIMLoaded = true;
					}
					
				}else if(imtype.equals("scj")){
					SharedPreferences sp = getSharedPreferences(LIME.IM_SCJ_STATUS, 0);
					if(sp.getString(LIME.IM_SCJ_STATUS, "false").equals("false")){
						hasIMLoaded = true;
					}
				}else if(imtype.equals("phonetic")){
					SharedPreferences sp = getSharedPreferences(LIME.IM_PHONETIC_STATUS, 0);
					if(sp.getString(LIME.IM_PHONETIC_STATUS, "false").equals("false")){
						hasIMLoaded = true;
					}
				}else if(imtype.equals("ez")){
					SharedPreferences sp = getSharedPreferences(LIME.IM_EZ_STATUS, 0);
					if(sp.getString(LIME.IM_EZ_STATUS, "false").equals("false")){
						hasIMLoaded = true;
					}
				}else if(imtype.equals("dayi")){
					SharedPreferences sp = getSharedPreferences(LIME.IM_DAYI_STATUS, 0);
					if(sp.getString(LIME.IM_DAYI_STATUS, "false").equals("false")){
						hasIMLoaded = true;
					}
				}else if(imtype.equals("custom")){
					SharedPreferences sp = getSharedPreferences(LIME.IM_CUSTOM_STATUS, 0);
					if(sp.getString(LIME.IM_CUSTOM_STATUS, "false").equals("false")){
						hasIMLoaded = true;
					}
				}else if(imtype.equals("array")){
					SharedPreferences sp = getSharedPreferences(LIME.IM_CUSTOM_STATUS, 0);
					if(sp.getString(LIME.IM_CUSTOM_STATUS, "false").equals("false")){
						hasIMLoaded = true;
					}
				}
				
			}else{
				btnLoadMapping.setEnabled(false);
			}
			
			if(!hasIMLoaded){
				btnResetMapping.setEnabled(false);
			}
			updateLabelInfo();
			
		}

		public void resetLabelInfo(){
			mLIMEPref.setParameter(imtype+LIME.IM_MAPPING_FILENAME,"");
			mLIMEPref.setParameter(imtype+LIME.IM_MAPPING_VERSION,"");
			mLIMEPref.setParameter(imtype+LIME.IM_MAPPING_TOTAL,0);
			mLIMEPref.setParameter(imtype+LIME.IM_MAPPING_DATE,"");
		}
		
		public void updateLabelInfo(){
			try{
				labSource.setText(mLIMEPref.getParameterString(imtype+LIME.IM_MAPPING_FILENAME));
				labVersion.setText(mLIMEPref.getParameterString(imtype+LIME.IM_MAPPING_VERSION));
				labTotalAmount.setText(String.valueOf(mLIMEPref.getParameterInt(imtype+LIME.IM_MAPPING_TOTAL)));
				labImportDate.setText(mLIMEPref.getParameterString(imtype+LIME.IM_MAPPING_DATE));
				labMappingSettingTitle.setText(imtype.toUpperCase() + " Mapping Setting" );
			}catch(Exception e){
				e.printStackTrace();
			}
		}

		private ServiceConnection serConn = new ServiceConnection() {
			public void onServiceConnected(ComponentName name, IBinder service) {
				if(DBSrv == null){
					//Log.i("ART","Start up db service");
					DBSrv = IDBService.Stub.asInterface(service);
				}else{
					//Log.i("ART","Stop up db service");
				}
			}
			public void onServiceDisconnected(ComponentName name) {}

		};
		
		/**
		 * Select file to be import from the list
		 * 
		 * @param path
		 */
		public void selectLimeFile(String srcpath, String tablename) {

			// Retrieve Filelist
			filelist = getAvailableFiles(srcpath, tablename);

			ArrayList<String> showlist = new ArrayList<String>();

			int count = 0;
			for (File unit : filelist) {
				if (count == 0) {
					showlist.add(this.getResources().getText(
							R.string.lime_setting_select_root).toString());
					count++;
					continue;
				} else if (count == 1) {
					showlist.add(this.getResources().getText(
							R.string.lime_setting_select_parent).toString());
					count++;
					continue;
				}
				if (unit.isDirectory()) {
					showlist.add(" +[" + unit.getName() + "]");
				} else {
					showlist.add(" " + unit.getName());
				}
				count++;
			}

			// get a builder and set the view
			LayoutInflater li = LayoutInflater.from(this);
			View view = li.inflate(R.layout.filelist, null);

			ArrayAdapter<String> adapterlist = new ArrayAdapter<String>(this,
					R.layout.filerow, showlist);
			final String table = new String(tablename);
			ListView listview = (ListView) view.findViewById(R.id.list);
			listview.setAdapter(adapterlist);
			listview.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> arg0, View vi, int position,
						long id) {
					selectLimeFile(filelist.get(position).getAbsolutePath(), table);
				}
			});
			
			// if AlertDialog exists then dismiss before create
			if (ad != null) {
				ad.dismiss();
			}

			if (!hasSelectFile) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.lime_setting_btn_load_local_notice);
				builder.setView(view);
				builder.setNeutralButton(R.string.label_close_key,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dlg, int sumthin) {

							}
						});

				ad = builder.create();
				ad.show();
			}
			
		}
		
		/**
		 * Get list of the file from the path
		 * 
		 * @param path
		 * @return
		 */
		private ArrayList<File> getAvailableFiles(String path, String tablename) {

			ArrayList<File> templist = new ArrayList<File>();

			File check = new File(path);
			
			if (check.exists() && check.isDirectory()) {

				File root = null;
				root = new File(LIME.IM_LOAD_LIME_ROOT_DIRECTORY);

				// Fixed first 1 & 2
				templist.add(root);

				// Back to Parent
				if (check.getParentFile().getAbsolutePath().equals("/")) {
					templist.add(root);
				} else {
					templist.add(check.getParentFile());
				}

				File rootPath = new File(path);
				File list[] = rootPath.listFiles();
				for (File unit : list) {
					if (unit.isDirectory()
							|| (true)
							|| (unit.isFile() && unit.getName().toLowerCase().endsWith(".lime"))
							|| (unit.isFile() && unit.getName().toLowerCase().endsWith(".cin"))) {
						templist.add(unit);
					}
				}

			} else if (check.exists() && check.isFile()
					&& (  true || check.getName().toLowerCase().endsWith(".lime") || check.getName().toLowerCase().endsWith(".cin"))  ) {
				//Log.i("ART","run load mapping method : " + imtype);
				hasSelectFile = true;
				loadMapping(check);
			}
			return templist;
		}
		
		/**
		 * Import mapping table into database
		 * 
		 * @param unit
		 */
		public void loadMapping(File unit) {
			try {
				DBSrv.loadMapping(unit.getAbsolutePath(), imtype);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

}