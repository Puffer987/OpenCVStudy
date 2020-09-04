package com.adolf.opencvstudy.rv;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.adolf.opencvstudy.R;
import com.bumptech.glide.Glide;

import java.io.File;
import java.util.List;

/**
 * @program: OpenCVStudy
 * @description:
 * @author: Adolf
 * @create: 2020-09-02 17:36
 **/
public class ImgRVAdapter extends RecyclerView.Adapter<ImgRVAdapter.ViewHolder> {
    private List<ItemRVBean> mItemList;
    private Context mContext;
    private BigPicDialog myDialog;


    public ImgRVAdapter(List<ItemRVBean> itemList, Context context) {
        mItemList = itemList;
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
        Glide.with(mContext).load(new File(mItemList.get(position).getImgPath())).override(500, 500).into(holder.mItemImg);

        holder.mItemTitle.setText(mItemList.get(position).getImgTitle());

        holder.mItemImg.setOnClickListener(view -> {
            myDialog = new BigPicDialog(mContext, R.style.BigPicDialog);
            myDialog.setImgPath(mItemList.get(position).getImgPath());
            myDialog.show();
        });

    }

    @Override
    public int getItemCount() {
        return mItemList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView mItemImg;
        TextView mItemTitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mItemImg = itemView.findViewById(R.id.iv_item);
            mItemTitle = itemView.findViewById(R.id.tv_title);
        }
    }
}
