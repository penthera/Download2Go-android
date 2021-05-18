package com.penthera.sdkdemo.dialog;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.fragment.app.DialogFragment;

import com.penthera.sdkdemo.R;
import com.penthera.virtuososdk.client.IMimeTypeSettings;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

public class MimeTypeSettingsDialog extends DialogFragment {

    public interface MimeTypeSettingsCallback{
        IMimeTypeSettings initialSettings();

    }

    private MimeTypeSettingsCallback callback;
    private IMimeTypeSettings settings;

    private Spinner manifestSpinner;
    private Spinner segmentSpinner;
    private EditText values;

    private IMimeTypeSettings.ManifestType currentManifestType;
    private IMimeTypeSettings.SegmentType currentSegmentType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, 0);
        setCancelable(false);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.mime_settings_dialog, container, false);

        getDialog().setTitle("MIME type settings");
        manifestSpinner = view.findViewById(R.id.mime_manifest_spinner);
        manifestSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch(position){
                    case 1: {
                        currentManifestType = IMimeTypeSettings.ManifestType.HLS;
                        break;
                    }
                    case 2: {
                        currentManifestType = IMimeTypeSettings.ManifestType.DASH;
                        break;
                    }

                    case 3: {
                        currentManifestType = IMimeTypeSettings.ManifestType.ALL;
                        break;
                    }
                    default:
                        currentManifestType = null;
                }
                segmentSpinner.setSelection(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {


                segmentSpinner.setSelection(0);
            }
        });

        List<String> manifestStrings = Arrays.asList(getResources().getStringArray(R.array.mime_manifest_types));
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, manifestStrings);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        manifestSpinner.setAdapter(dataAdapter);


        segmentSpinner = view.findViewById(R.id.mime_segment_spinner);
        segmentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch(position){
                    case 1:{

                        currentSegmentType = IMimeTypeSettings.SegmentType.VIDEO;
                        break;
                    }

                    case 2:{
                        currentSegmentType  = IMimeTypeSettings.SegmentType.AUDIO;
                        break;
                    }

                    case 3: {
                        currentSegmentType = IMimeTypeSettings.SegmentType.TEXT;
                        break;
                    }

                    default: {
                        currentSegmentType = null;
                    }
                }

                populateValues();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                values.setText("");
            }
        });

        List<String> segmentStrings = Arrays.asList(getResources().getStringArray(R.array.mime_segment_types));
        ArrayAdapter<String> segmentAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, segmentStrings);
        segmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        segmentSpinner.setAdapter(segmentAdapter);


        values = view.findViewById(R.id.mime_type_values);

        view.findViewById(R.id.mime_settings_save_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(currentManifestType != null && currentSegmentType != null) {
                    String mimeValues = values.getText().toString();

                    StringTokenizer st = new StringTokenizer(mimeValues, "\n\r,");
                    ArrayList<String> list = new ArrayList<>();
                    while (st.hasMoreTokens()) {
                        String s = st.nextToken().trim();

                        if (s.length() > 0) {
                            list.add(s);
                        }

                    }

                    settings.setMimeTypesForFileAndSegment(currentManifestType, currentSegmentType, list);
                }

            }
        });

        view.findViewById(R.id.mime_settings_done_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        settings = callback.initialSettings();


        return view;
    }

    private void populateValues() {

        if(currentManifestType != null && currentSegmentType != null){
            List<String> mimeTypes = settings.getMimeTypesForFileAndSegment(currentManifestType,currentSegmentType);

            if(mimeTypes != null && mimeTypes.size() > 0){
                values.setText(TextUtils.join("\n", mimeTypes));
            }
            else{
                values.setText("");
            }
        }
        else{
            values.setText("");
        }
    }


    @Override
    public void onAttach(Context context){
        super.onAttach(context);

        if(context instanceof MimeTypeSettingsCallback){
            callback = (MimeTypeSettingsCallback)context;
        }

    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback = null;
    }
}
