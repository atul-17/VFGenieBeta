package com.libre.alexa.adapter;

import android.content.Context;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import com.libre.alexa.R;
import com.libre.alexa.models.ModelStoreDeviceDetails;
import com.libre.alexa.utils.interfaces.AddRemovedCheckedDevicesInterface;
import com.squareup.picasso.Picasso;
import com.suke.widget.SwitchButton;

import java.util.List;

public class ShowDevicesAdapter extends RecyclerView.Adapter<ShowDevicesAdapter.ShowDevicesHolder> {

    public Context context;
    public List<ModelStoreDeviceDetails> modelStoreDeviceDetailsList;
    public AddRemovedCheckedDevicesInterface addRemovedCheckedDevicesInterface;

    public String TAG = ShowDevicesAdapter.class.getSimpleName();


    public void setAddRemovedCheckedDevicesInterface(AddRemovedCheckedDevicesInterface addRemovedCheckedDevicesInterface) {
        this.addRemovedCheckedDevicesInterface = addRemovedCheckedDevicesInterface;
    }

    public ShowDevicesAdapter(Context context, List<ModelStoreDeviceDetails> modelStoreDeviceDetails) {
        this.context = context;
        this.modelStoreDeviceDetailsList = modelStoreDeviceDetails;
    }

    @Override
    public ShowDevicesHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.adapter_select_dinner_time_devices, viewGroup, false);
        ShowDevicesHolder showDevicesHolder = new ShowDevicesHolder(view);
//        showDevicesHolder.setIsRecyclable(false);
        return showDevicesHolder;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void onBindViewHolder(final ShowDevicesHolder showDevicesHolder, int i) {

        setHostIcon(showDevicesHolder, modelStoreDeviceDetailsList.get(i).hostIcon);

        showDevicesHolder.tv_device_name.setText(modelStoreDeviceDetailsList.get(i).name);

        showDevicesHolder.tv_device_type.setText(modelStoreDeviceDetailsList.get(i).hostType);

        if (modelStoreDeviceDetailsList.get(i).isBlackListed) {
            //make it checked
            showDevicesHolder.sb_dinnertime_device.setChecked(true);
        } else {
            showDevicesHolder.sb_dinnertime_device.setChecked(false);
        }


        if (modelStoreDeviceDetailsList.get(i).isActive.equals("1")) {
            //isactive ==true
            //so make txt color black
            showDevicesHolder.tv_device_name.setTextColor(context.getResources().getColor(R.color.black));
            showDevicesHolder.tv_device_type.setTextColor(context.getResources().getColor(R.color.black));
        } else {
            //isactive == false
            //for now may chnange in the future
            //so make txt color ==black
            showDevicesHolder.tv_device_name.setTextColor(context.getResources().getColor(R.color.black));
            showDevicesHolder.tv_device_type.setTextColor(context.getResources().getColor(R.color.black));
        }


        showDevicesHolder.sb_dinnertime_device.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                if (view.isChecked()) {
                    //add device to the list
                    modelStoreDeviceDetailsList.get(showDevicesHolder.getAdapterPosition()).isBlackListed = true;
                    showDevicesHolder.sb_dinnertime_device.setChecked(true);
                    Log.d(TAG, "adding data on checked " + modelStoreDeviceDetailsList.get(showDevicesHolder.getAdapterPosition()).name);
                    addRemovedCheckedDevicesInterface
                            .addCheckedDevice(new ModelStoreDeviceDetails(
                                    modelStoreDeviceDetailsList.get(showDevicesHolder.getAdapterPosition()).isActive,
                                    modelStoreDeviceDetailsList.get(showDevicesHolder.getAdapterPosition()).isBlackListed,
                                    modelStoreDeviceDetailsList.get(showDevicesHolder.getAdapterPosition()).macId,
                                    modelStoreDeviceDetailsList.get(showDevicesHolder.getAdapterPosition()).name,
                                    modelStoreDeviceDetailsList.get(showDevicesHolder.getAdapterPosition()).hostIcon,
                                    modelStoreDeviceDetailsList.get(showDevicesHolder.getAdapterPosition()).hostType
                            ));
                } else {
                    showDevicesHolder.sb_dinnertime_device.setChecked(false);
                    //remove the device from the list
                    modelStoreDeviceDetailsList.get(showDevicesHolder.getAdapterPosition()).isBlackListed = false;
                    addRemovedCheckedDevicesInterface.
                            removeUnCheckedDevice(modelStoreDeviceDetailsList.get(showDevicesHolder.getAdapterPosition()).macId);
                }
            }
        });
    }


    public void setHostIcon(ShowDevicesHolder holder, String hostIcon) {
        switch (hostIcon) {
            case "-1":
                holder.iv_device_type.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_vdf_default));
                break;

            case "Game Console":
            case "0":
                holder.iv_device_type.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_game_console));
                break;

            case "IP Phone":
            case "1":
                holder.iv_device_type.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_vdf_phone));
                break;

            case "Personal Computer":
            case "2":
                holder.iv_device_type.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_laptop));

                break;

            case "Printer and scanner":
            case "3":
                holder.iv_device_type.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_printer));
                break;

            case "Mobile":
            case "4":
                holder.iv_device_type.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_mobile));
                break;

            case "Tablet":
            case "5":
                holder.iv_device_type.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_tablet));
                break;

            case "TV":
            case "6":
                holder.iv_device_type.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_tv));
                break;

            case "Router":
            case "7":
                holder.iv_device_type.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_vdf_router));
                break;

            case "NAS":
            case "8":
                holder.iv_device_type.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_vdf_nas));
                break;

            case "Camera":
            case "9":
                holder.iv_device_type.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_vdf_camera));
                break;

            case "Home Automation":
            case "10":
                holder.iv_device_type.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_device_overview_iot));
                break;

            case "Voice Assistant":
            case "11":
                holder.iv_device_type.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_vdf_voice_assistant));
                break;

            case "Wi-Fi Speaker":
            case "12":
                holder.iv_device_type.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_black_volume_up));
                break;

            case "Wearable and Smart Watch":
            case "13":
                holder.iv_device_type.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_vdf_watch));
                break;

            case "Wi-Fi Extender":
            case "14":
                holder.iv_device_type.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_vdf_extender));
                break;

            case "Content Player":
            case "15":
                holder.iv_device_type.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_vdf_player));
                break;

            case "Streaming Device":
            case "16":
                holder.iv_device_type.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_vdf_streaming_device));
                break;

            case "Content Receiver":
            case "17":
                holder.iv_device_type.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_vdf_content_receiver));
                break;
            case "Business and Office":
            case "18":
                holder.iv_device_type.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_vdf_business_review));
                break;

            case "Network":
            case "19":
                holder.iv_device_type.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_vdf_network));
                break;

            case "USB Device":
            case "20":
                holder.iv_device_type.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_vdf_usb));
                break;


            case "Server":
            case "21":
                holder.iv_device_type.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_vdf_server));
                break;

            case "Small Boards":
            case "22":
                holder.iv_device_type.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_vdf_esim));
                break;

            case "Other Devices":
            case "23":
                holder.iv_device_type.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_vdf_default));
                break;


            default:
                holder.iv_device_type.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_vdf_default));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return modelStoreDeviceDetailsList.size();
    }

    public static class ShowDevicesHolder extends RecyclerView.ViewHolder {

        AppCompatImageView iv_device_type;

        AppCompatTextView tv_device_name, tv_device_type;

        SwitchButton sb_dinnertime_device;

        public ShowDevicesHolder(View itemView) {
            super(itemView);

            iv_device_type = (AppCompatImageView) itemView.findViewById(R.id.ivDeviceType);

            tv_device_name = itemView.findViewById(R.id.tvDeviceName);

            tv_device_type = itemView.findViewById(R.id.tvDeviceType);

            sb_dinnertime_device = itemView.findViewById(R.id.sbDinnertimeDevice);

        }
    }


}
