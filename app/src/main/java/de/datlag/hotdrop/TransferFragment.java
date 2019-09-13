package de.datlag.hotdrop;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.adroitandroid.near.model.Host;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.NotNull;

import java.io.File;

import de.datlag.hotdrop.utils.DiscoverHost;
import de.datlag.hotdrop.utils.FileUtil;


public class TransferFragment extends Fragment {

    private Activity activity;
    private DiscoverHost discoverHost;
    private Host host;
    private View rootView;
    private LinearLayoutCompat fileContainer;
    private AppCompatTextView hostName;

    public TransferFragment(Activity activity, DiscoverHost discoverHost, Host host) {
        this.activity = activity;
        this.discoverHost = discoverHost;
        this.host = host;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_transfer, container, false);
        init();
        initLogic();
        return rootView;
    }

    private void init() {
        fileContainer = rootView.findViewById(R.id.file_container);
        hostName = rootView.findViewById(R.id.host_name);
    }

    private void initLogic() {
        fileContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new MaterialAlertDialogBuilder(activity)
                        .setMessage("Sending File or Folder?")
                        .setPositiveButton("File", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                FileUtil.chooseFile(activity, new FileUtil.FileChooseCallback() {
                                    @Override
                                    public void onChosen(String path, File file) {
                                        discoverHost.send(host, FileUtil.jsonObjectToBytes(FileUtil.jsonObjectFromFile(file)));
                                    }
                                });
                            }
                        })
                        .setNegativeButton("Folder", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                FileUtil.chooseFolder(activity, new FileUtil.FolderChooseCallback() {
                                    @Override
                                    public void onChosen(String path, File file) {
                                        Log.e("Path", path);
                                        Log.e("File", file.getName());
                                    }
                                });
                            }
                        }).create().show();
            }
        });

        fileContainer.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                if (dragEvent.getAction() == DragEvent.ACTION_DROP) {
                    Toast.makeText(getContext(), "Dropped", Toast.LENGTH_LONG).show();
                }
                return false;
            }
        });

        hostName.setText(host.getName().substring(host.getName().indexOf(activity.getPackageName()) + activity.getPackageName().length() +1));
    }
}