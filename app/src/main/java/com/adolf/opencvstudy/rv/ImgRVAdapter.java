package com.adolf.opencvstudy.rv;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.adolf.opencvstudy.R;
import com.bumptech.glide.Glide;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.List;

/**
 * @program: OpenCVStudy
 * @description:
 * @author: Adolf
 * @create: 2020-09-02 17:36
 **/
public class ImgRVAdapter extends RecyclerView.Adapter<ImgRVAdapter.ViewHolder> {
    private List<String> mImgList;
    private Context mContext;
    private BigPicDialog myDialog;


    public ImgRVAdapter(List<String> imgList, Context context) {
        mImgList = imgList;
        mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rv_img, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Bitmap bitmap = BitmapFactory.decodeFile(mImgList.get(position));
        // Picasso.with(mContext).load(new File(mImgList.get(position))).into( holder.mImgItem);
        Glide.with(mContext).load(new File(mImgList.get(position))).override(500, 500).into(holder.mImgItem);


        holder.mImgItem.setOnClickListener(view -> {
            myDialog =new BigPicDialog(mContext,R.style.BigPicDialog);
            myDialog.setImgPath(mImgList.get(position));
            myDialog.show();
        });

    }

    @Override
    public int getItemCount() {
        return mImgList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView mImgItem;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mImgItem = itemView.findViewById(R.id.iv_item);

        }
    }
}
