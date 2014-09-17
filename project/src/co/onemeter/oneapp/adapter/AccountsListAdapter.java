package co.onemeter.oneapp.adapter;

import java.util.ArrayList;
import java.util.Iterator;

import org.wowtalk.api.Account;
import org.wowtalk.api.Buddy;
import org.wowtalk.api.GlobalSetting;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.ui.PhotoDisplayHelper;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.ui.CircleImageView;
import co.onemeter.oneapp.ui.ManageAccountsActivity.RemoveAccountListener;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class AccountsListAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<Account> mAccounts;
    private boolean mIsDeleteMode;
    private RemoveAccountListener mRemoveAccountListener;

    public AccountsListAdapter(Context context, ArrayList<Account> accounts) {
        this(context, accounts, null);
    }

    public AccountsListAdapter(Context context, ArrayList<Account> accounts, RemoveAccountListener rmAccountListener) {
        mContext = context;
        mAccounts = accounts;
        mRemoveAccountListener = rmAccountListener;
    }

    public void setDataSource(ArrayList<Account> accounts) {
        mAccounts = accounts;
    }

    public void setDeleteMode(boolean isDeleteMode) {
        mIsDeleteMode = isDeleteMode;
    }

    @Override
    public int getCount() {
        return mAccounts.size();
    }

    @Override
    public Object getItem(int position) {
        return mAccounts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (null == convertView) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.listitem_account, null);
            holder.backgroundImg = (ImageView) convertView.findViewById(R.id.bg_img);
            holder.portraitImg = (CircleImageView) convertView.findViewById(R.id.img_portrait);
            holder.nameView = (TextView) convertView.findViewById(R.id.txt_name);
            holder.companyView = (TextView) convertView.findViewById(R.id.txt_company);
            holder.onlineText = (TextView) convertView.findViewById(R.id.online_text);
            holder.unreadText = (TextView) convertView.findViewById(R.id.unread_text);
            holder.deleteView = (TextView) convertView.findViewById(R.id.delete_view);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final Account account = mAccounts.get(position);
        if (account.albumCoverTimeStamp == -1 || TextUtils.isEmpty(account.albumCoverFileId)) {
            holder.backgroundImg.setImageResource(R.drawable.moment_default_album);
        } else {
            PhotoDisplayHelper.displayPhoto(mContext, holder.backgroundImg,
                    R.drawable.moment_default_album,
                    account.albumCoverFileId, account.albumCoverExt,
                    GlobalSetting.S3_MOMENT_FILE_DIR, null);
        }
        PrefUtil prefUtil = PrefUtil.getInstance(mContext);
        if (prefUtil.getUid().equals(account.uid)) {
            long photoTimeStamp = prefUtil.getMyPhotoUploadedTimestamp();
            // 本地保存的帐号中的头像信息不是最新的，需要更新SP_root中的帐号信息
            if (photoTimeStamp != account.photoUploadTimeStamp) {
                ArrayList<Account> accounts = prefUtil.getAccountList();
                Account tempAccount = null;
                for (Iterator<Account> iterator = accounts.iterator(); iterator.hasNext();) {
                    tempAccount = iterator.next();
                    if (account.uid.equals(tempAccount.uid)) {
                        account.photoUploadTimeStamp = photoTimeStamp;
                        tempAccount.photoUploadTimeStamp = photoTimeStamp;
                        break;
                    }
                }
                prefUtil.setAccountList(accounts);
            }
        }
        Buddy buddy = new Buddy(account.uid);
        buddy.setPhotoUploadedTimestamp(account.photoUploadTimeStamp);
        PhotoDisplayHelper.displayPhoto(mContext, holder.portraitImg,
                R.drawable.default_avatar_90, buddy, true);
        holder.nameView.setText(account.name);
        holder.companyView.setText(account.company);
        if (account.isOnline) {
            holder.onlineText.setVisibility(View.VISIBLE);
            holder.unreadText.setVisibility(View.INVISIBLE);
        } else {
            holder.onlineText.setVisibility(View.INVISIBLE);
            if (account.unreadCounts == 0) {
                holder.unreadText.setVisibility(View.INVISIBLE);
            } else {
                holder.unreadText.setVisibility(View.VISIBLE);
                holder.unreadText.setText(
                        String.format(mContext.getString(R.string.settings_account_unread_count),
                                account.unreadCounts));
            }
        }
        // 非当前在线用户才能删除
        holder.deleteView.setVisibility((mIsDeleteMode && !account.isOnline) ? View.VISIBLE : View.GONE);
        holder.deleteView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mRemoveAccountListener) {
                    mRemoveAccountListener.onRemovedAccount(account);
                }
            }
        });

        return convertView;
    }

    static class ViewHolder {
        ImageView backgroundImg;
        CircleImageView portraitImg;
        TextView nameView;
        TextView companyView;
        TextView onlineText;
        TextView unreadText;
        TextView deleteView;
    }
}
