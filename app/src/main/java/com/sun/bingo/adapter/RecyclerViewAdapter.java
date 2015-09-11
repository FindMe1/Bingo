package com.sun.bingo.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.siyamed.shapeimageview.CircularImageView;
import com.sun.bingo.R;
import com.sun.bingo.control.NavigateManager;
import com.sun.bingo.entity.BingoEntity;
import com.sun.bingo.entity.UserEntity;
import com.sun.bingo.framework.dialog.LoadingDialog;
import com.sun.bingo.framework.dialog.ToastTip;
import com.sun.bingo.util.DateUtil;
import com.sun.bingo.util.ShareUtil;
import com.sun.bingo.util.UserEntityUtil;
import com.sun.bingo.widget.GroupImageView.GroupImageView;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.listener.UpdateListener;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context mContext;
    private List<BingoEntity> mEntities;
    private UserEntity userEntity;
    private int type = NORMAL;

    public static final int NORMAL = 0;
    public static final int CANCEL_FAVORITE = 1;
    protected LoadingDialog loadingDialog;

    private static final int TYPE_LIST = 0;
    private static final int TYPE_FOOT_VIEW = 1;

    public RecyclerViewAdapter(Context context) {
        this.mContext = context;
    }

    public RecyclerViewAdapter(Context context, List<BingoEntity> entities) {
        this(context);
        this.mEntities = entities;
        userEntity = BmobUser.getCurrentUser(context, UserEntity.class);
        loadingDialog = new LoadingDialog(context);
    }

    public RecyclerViewAdapter(Context context, List<BingoEntity> entities, int type) {
        this(context, entities);
        this.type = type;
    }

    @Override
    public int getItemCount() {
        return mEntities.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position + 1 == getItemCount()) {
            return TYPE_FOOT_VIEW;
        } else {
            return TYPE_LIST;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case TYPE_LIST:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_card_main, parent, false);
                return new ListViewHolder(view);
            case TYPE_FOOT_VIEW:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_footview_layout, parent, false);
                return new FootViewHolder(view);

        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ListViewHolder) {
            final ListViewHolder viewHolder = (ListViewHolder) holder;
            final BingoEntity entity = mEntities.get(position);
            final int mPosition = position;

            if (entity.getUserEntity() != null) {
                UserEntityUtil.setUserAvatarView(viewHolder.civUserAvatar, entity.getUserEntity().getUserAvatar());
                UserEntityUtil.setTextViewData(viewHolder.tvNickName, entity.getUserEntity().getNickName());
            }

            viewHolder.tvDescribe.setText(entity.getDescribe());

            if (entity.getCreateTime() > 0) {
                viewHolder.tvTime.setVisibility(View.VISIBLE);
                viewHolder.tvTime.setText(DateUtil.getDateStr(mContext, entity.getCreateTime()));
            } else {
                viewHolder.tvTime.setVisibility(View.GONE);
            }

            if (entity.getImageList() != null && entity.getImageList().size() > 0) {
                viewHolder.givImageGroup.setVisibility(View.VISIBLE);
                viewHolder.givImageGroup.setPics(entity.getImageList());
            } else {
                viewHolder.givImageGroup.setVisibility(View.GONE);
            }

            viewHolder.llRootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ObjectAnimator animator = ObjectAnimator.ofFloat(viewHolder.llRootView, "translationZ", 20, 0);
                    animator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            NavigateManager.gotoBingoDetailActivity(mContext, entity);
                        }
                    });
                    animator.start();
                }
            });

            viewHolder.ivItemMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPopMenu(v, mPosition);
                }
            });
        }
    }

    /**
     * 弹出菜单
     */
    private void showPopMenu(View ancho, final int position) {
        userEntity = BmobUser.getCurrentUser(mContext, UserEntity.class);
        final BingoEntity entity = mEntities.get(position);
        List<String> favoriteList = userEntity.getFavoriteList();

        PopupMenu popupMenu = new PopupMenu(mContext, ancho);
        popupMenu.getMenuInflater().inflate(R.menu.item_pop_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.pop_favorite:
                        loadingDialog.show();
                        if (item.getTitle().equals("取消收藏")) {
                            cancelFavoriteBingo(entity.getObjectId(), position);
                        } else {
                            handleFavoriteBingo(entity.getObjectId());
                        }
                        userEntity = BmobUser.getCurrentUser(mContext, UserEntity.class);
                        return true;
                    case R.id.pop_share:
                        ShareUtil.share(mContext, entity.getDescribe() + entity.getWebsite() + "\n[来自" + mContext.getString(R.string.app_name) + "的分享，下载地址：https://fir.im/bingoworld]");
                        return true;
                }
                return false;
            }
        });
        if (type == CANCEL_FAVORITE || (favoriteList != null && favoriteList.indexOf(entity.getObjectId()) >= 0)) {
            MenuItem menuItem = popupMenu.getMenu().findItem(R.id.pop_favorite);
            menuItem.setTitle("取消收藏");
        }
        popupMenu.show();
    }

    /**
     * 收藏
     */
    private void handleFavoriteBingo(String bingoId) {
        List<String> favoriteList = userEntity.getFavoriteList();
        if (favoriteList == null) {
            favoriteList = new ArrayList<>();
        }
        if (favoriteList.indexOf(bingoId) >= 0) {
            ToastTip.showToastDialog(mContext, "您已收藏过了");
            return;
        }
        favoriteList.add(bingoId);
        userEntity.setFavoriteList(favoriteList);
        userEntity.update(mContext, userEntity.getObjectId(), new UpdateListener() {
            @Override
            public void onSuccess() {
                loadingDialog.dismiss();
                ToastTip.showToastDialog(mContext, "收藏成功");
            }

            @Override
            public void onFailure(int i, String s) {
                loadingDialog.dismiss();
                ToastTip.showToastDialog(mContext, "收藏失败");
            }
        });
    }

    /**
     * 取消收藏
     */
    private void cancelFavoriteBingo(String bingoId, final int position) {
        List<String> favoriteList = userEntity.getFavoriteList();
        if (favoriteList == null) {
            favoriteList = new ArrayList<>();
        }
        if (favoriteList.indexOf(bingoId) < 0) {
            ToastTip.showToastDialog(mContext, "您已取消收藏了");
            return;
        }
        favoriteList.remove(bingoId);
        userEntity.setFavoriteList(favoriteList);
        userEntity.update(mContext, userEntity.getObjectId(), new UpdateListener() {
            @Override
            public void onSuccess() {
                loadingDialog.dismiss();
                ToastTip.showToastDialog(mContext, "取消成功");
                if (type == CANCEL_FAVORITE) {
                    mEntities.remove(position);
                    notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(int i, String s) {
                loadingDialog.dismiss();
                ToastTip.showToastDialog(mContext, "取消失败");
            }
        });
    }

    static class ListViewHolder extends RecyclerView.ViewHolder {
        @InjectView(R.id.civ_user_avatar)
        CircularImageView civUserAvatar;
        @InjectView(R.id.tv_nick_name)
        TextView tvNickName;
        @InjectView(R.id.tv_time)
        TextView tvTime;
        @InjectView(R.id.iv_item_more)
        ImageView ivItemMore;
        @InjectView(R.id.ll_icons)
        LinearLayout llIcons;
        @InjectView(R.id.tv_describe)
        TextView tvDescribe;
        @InjectView(R.id.ll_root_view)
        LinearLayout llRootView;
        @InjectView(R.id.giv_image_group)
        GroupImageView givImageGroup;

        public ListViewHolder(View view) {
            super(view);
            ButterKnife.inject(this, view);
        }
    }

    static class FootViewHolder extends RecyclerView.ViewHolder {
        @InjectView(R.id.tv_loading_more)
        TextView tvLoadingMore;

        public FootViewHolder(View view) {
            super(view);
            ButterKnife.inject(this, view);
        }
    }
}
