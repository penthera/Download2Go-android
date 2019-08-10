package com.penthera.sdkdemo.dialog;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.penthera.sdkdemo.R;

/**
 * Simple example of dialog to explain need for allowing write permissions
 */
public class PermissionsExplanationDialog extends DialogFragment {

    public interface PermissionResponseListener {
        void permissionResponse(boolean allow);
    }

    private View mLayout;
    private PermissionResponseListener listener;

    public static PermissionsExplanationDialog newInstance(PermissionResponseListener listener) {
        PermissionsExplanationDialog d = new PermissionsExplanationDialog();
        d.listener = listener;
        return d;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mLayout = inflater.inflate(R.layout.permissions_dialog, container, false);

        mLayout.findViewById(R.id.btn_permission_ok).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if (listener != null) listener.permissionResponse(true);
                dismiss();
            }
        });

        mLayout.findViewById(R.id.btn_permission_no).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if (listener != null) listener.permissionResponse(false);
                dismiss();
            }
        });
        getDialog().setTitle("Permissions Reason");
        return mLayout;
    }
}
