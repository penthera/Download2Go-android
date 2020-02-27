//Copyright (c) 2017 Penthera Partners, LLC. All rights reserved.
//
//PENTHERA CONFIDENTIAL
//
//(c) 2015 Penthera Partners Inc. All Rights Reserved.
//
//NOTICE: This file is the property of Penthera Partners Inc.
//The concepts contained herein are proprietary to Penthera Partners Inc.
//and may be covered by U.S. and/or foreign patents and/or patent
//applications, and are protected by trade secret or copyright law.
//Distributing and/or reproducing this information is forbidden
//unless prior written permission is obtained from Penthera Partners Inc.
//
package com.penthera.sdkdemo.dialog;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.fragment.app.DialogFragment;

import com.penthera.sdkdemo.R;
import com.penthera.virtuososdk.client.IBackplaneDevice;

public class ChangeNickNameDialog extends DialogFragment {
	/** Log tag */
	private static final String TAG = ChangeNickNameDialog.class.getName();
	public static final int BTN_APPLY = 0;
	public static final int BTN_CANCEL = 1;
    private View mLayout;
    Button mApplyButton;
	
    /** Dialog Listener */
    private DialogCallback mListener;

    /** Dialog observer */
    public interface DialogCallback {
    	public void onSelected(DialogFragment dialog, int trigger, Bundle b);
    }
    
    public void setListener(DialogCallback listener) {
    	mListener = listener;
    }
        
    // --- Construction
    
    /** 
     * Create a new instance of the dialog with a callback 
     */
    public static ChangeNickNameDialog newInstance(DialogCallback cb) {
        ChangeNickNameDialog f = new ChangeNickNameDialog();
        f.mListener = cb;
        return f;
    }

    // --- Life-cycle Methods
    
    // onCreate
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, 0);
        
    }
    
    // onCreateView
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    	mLayout = inflater.inflate(R.layout.nickname_dialog, container, false);

        getDialog().setTitle("Change Nickname");
        EditText nickname_editor = (EditText) mLayout.findViewById(R.id.nickname);
        String nickname = getArguments().getString("nickname");
        final IBackplaneDevice device = getArguments().getParcelable("device");
        nickname_editor.setText(TextUtils.isEmpty(nickname) ? "":nickname);

        // Subscribe
        mApplyButton = (Button) mLayout.findViewById(R.id.btn_apply);
        mApplyButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mListener == null) {
					Log.e(TAG, "no listener");
					try { dismiss();} catch (Exception e) {}
					return;
				}

				Bundle b = new Bundle();
				b.putParcelable("device", device);
				try {
					EditText ed = (EditText) mLayout.findViewById(R.id.nickname);
					b.putString("nickname", ed.getText().toString());
				} catch (Exception e){}


				mListener.onSelected(ChangeNickNameDialog.this, BTN_APPLY, b);					
			}
        });
        // Cancel
        Button btn = (Button) mLayout.findViewById(R.id.btn_cancel);
        btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mListener == null) {
					Log.e(TAG, "no listener");
					try { dismiss();} catch (Exception e) {}
					return;
				}
				mListener.onSelected(ChangeNickNameDialog.this, BTN_CANCEL, null);
			}
        });
        return mLayout;
    }
    
    
	public static String getAuthority(Context context) {
		String authority = "";
		try {
			ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
			Bundle b =ai.metaData;
			authority = b.getString("com.penthera.virtuososdk.client.pckg");  
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}		
		return authority;
	}

    
	@Override
    public void onActivityCreated(Bundle b) {
    	super.onActivityCreated(b);
   }
    
    // onPause
    @Override
    public void onPause()
    {
    	super.onPause();
    }
    
    // onDestroy
    @Override
    public void onDestroy()
    {
    	super.onDestroy();
    }   
}


